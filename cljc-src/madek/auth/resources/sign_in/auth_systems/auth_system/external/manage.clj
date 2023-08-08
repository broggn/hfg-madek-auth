(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.manage
  (:require
    [clojure.set :refer [rename-keys]]
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :refer [debug-ns]]
    [madek.auth.db.core :refer [get-ds]]
    [madek.auth.utils.core :refer [presence]]
    [next.jdbc :as jdbc]
    [tick.core :as time]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn assert-property! [m k]
  (assert (get m k) (str k " is missing in map"))
  m)

(defn institution! [auth-system]
  (or 
    (-> auth-system :managed_domain presence)
    (throw 
      (ex-info "managed_domain resp instition must be present" {}))))


(defn person-properties [account]
  (-> (select-keys account [:last_name :first_name])
      (assoc :subtype "Person")))

(defn user-properties [account auth-system]
  (-> (select-keys account [:email :login])
      (assoc :institutional_id  (:id account))
      (assoc :institution (institution! auth-system))))



;;; create ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-or-create-user [account auth-system tx]
  (let [instituional-id (-> account :id presence)
        institution (institution! auth-system)]
    (assert instituional-id)
    (or (-> (sql/from :users)
            (sql/select :*)
            (sql/where [:= :institutional_id instituional-id])
            (sql/where [:= :institution institution])
            (sql-format :inline false)
            (#(jdbc/execute-one! tx %)))
        (let [person (-> (sql/insert-into :people)
                         (sql/values [(person-properties account)])
                         (sql-format :inline false)     
                         (#(jdbc/execute-one! tx % {:return-keys true})))]
          (-> (sql/insert-into :users)
              (sql/values [(-> (user-properties account auth-system)
                               (assoc :person_id (:id person)))])
              (sql-format :inline false)     
              (#(jdbc/execute-one! tx % {:return-keys true})))))))


;;; update ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn update-person [user account auth-system tx]
  (-> (sql/update :people)
      (sql/set (person-properties account))
      (sql/where [:= :id (:person_id user)])
      (sql-format :inline false)     
      (#(jdbc/execute-one! tx % {:return-keys true}))))

(defn update-user [user account auth-system tx]
  (-> (sql/update :users)
      (sql/set (user-properties account auth-system))
      (sql-format :inline false)     
      (#(jdbc/execute-one! tx % {:return-keys true}))))

;;; groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def group-id-keys
  [:institution :institutional_id]
  )

(def group-other-keys
  [:institutional_name
   :name 
   :type])

(def group-keys 
  (concat group-id-keys group-other-keys))

(defn current-groups [user-id institution tx]
  (-> (sql/from :groups)
      (sql/select :*)
      (sql/join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/where [:= :groups_users.user_id user-id])
      (sql-format :inline false)
      (#(jdbc/execute! tx % {}))))


(defn create-or-update-group [group tx]
  (-> (sql/insert-into :groups)
      (sql/values [group])
      (sql/upsert 
        (as-> (sql/on-conflict :institutional_id :institution) clause 
          (apply sql/do-update-set (concat [clause] group-other-keys))))
      (sql-format :inline false)
      (#(jdbc/execute-one! tx % {:return-keys true}))))

(comment 
  (create-or-update-group 
    {:institutional_id "0001" :institution "bar.com" :name "first group test"}
    (get-ds)
    ))

(defn create-or-update-target-groups [account institution tx]
  (->> account 
       :groups
       (map #(rename-keys % {:id :institutional_id}))
       (map #(assoc % :institution institution))
       (map #(select-keys % group-keys))
       (map #(create-or-update-group % tx))))

(defn add-user-to-groups [user group-ids tx]
  (-> (sql/insert-into :groups_users)
      (sql/values (->> group-ids 
                       (map #(assoc {} :group_id % :user_id (:id user)))))
      (sql-format :inline false)
      (#(jdbc/execute! tx % {:return-keys true}))))

(defn remove-user-from-groups [user group-ids tx]
  (when (seq group-ids)
    (-> (sql/delete-from :groups_users)
        (sql/where [:in :group_id group-ids])
        (sql/where [:= :user_id (:id user)])
        (sql-format :inline false)
        (#(jdbc/execute! tx % {})))))

(defn update-groups 
  [user  account auth-system tx]
  (let [institution (institution! auth-system)
        target-groups (create-or-update-target-groups account institution tx)
        target-ids (->> target-groups (map :id) set)
        current-groups (current-groups (:id user) institution tx)
        current-ids (->> current-groups (map :id) set)]
    (debug target-ids)
    (debug current-ids)
    (add-user-to-groups user (clojure.set/difference target-ids current-ids) tx)
    (remove-user-from-groups user (clojure.set/difference current-ids target-ids) tx)
    ))

;;; update ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn manage-account [account auth-system tx]
  (let [user (get-or-create-user account auth-system tx)]
    (update-person user account auth-system tx)
    (update-user user account auth-system tx)
    (update-groups user account auth-system tx)
    user))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
