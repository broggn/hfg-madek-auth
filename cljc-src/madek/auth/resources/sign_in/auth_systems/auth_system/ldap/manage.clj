(ns madek.auth.resources.sign-in.auth-systems.auth-system.ldap.manage
  (:require
   [clojure.set :refer [rename-keys]]
   [cuerdas.core :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :refer [debug-ns]]
   [madek.auth.db.core :refer [get-ds]]
   [madek.auth.utils.core :refer [presence]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]
   [tick.core :as time]))

(defn assert-property! [m k]
  (assert (get m k) (str k " is missing in map"))
  m)

(defn institution! [auth-system]
  (or
   (-> auth-system :managed_domain presence)
   (throw
    (ex-info "managed_domain resp instition must be present" {}))))

#_{:objectClass ["top" "inetOrgPerson" "eduPerson" "schacLinkageIdentifiers"], 
   :eduPersonAffiliation ["employee" "member"], 
   :uid ["aliebrich"], 
   :mail ["aliebrich@hfg-karlsruhe.de"], 
   :eduPersonPrincipalName ["aliebrich@hfg-karlsruhe.de"], 
   :givenName ["Alexander"], 
   :eduPersonUniqueId ["03356e009c56482ea0532d4a0add0ded@hfg-karlsruhe.de"], 
   :sn ["Liebrich"], 
   :cn ["Alexander Liebrich"]}

(defn person-properties [account]
  (-> (rename-keys account {:sn :last_name :givenName :first_name})
      (select-keys [:last_name :first_name])
      (assoc :subtype "Person")))

(defn user-properties [account auth-system]
  (-> (rename-keys account {:mail :email :uid :login :givenName :first_name :sn :last_name})
      (select-keys [:email :login ])
      (assoc :institutional_id (:eduPersonUniqueId account))
      (assoc :institution (institution! auth-system))))

;;; create ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-or-create-user [account auth-system tx]
  (let [institutional-id (-> account :eduPersonUniqueId presence)
        institution (institution! auth-system)]
    (assert institutional-id)
    (or (-> (sql/from :users)
            (sql/select :*)
            (sql/where [:= :institutional_id institutional-id])
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
      (sql/where [:= :users.id (:id user)])
      (sql-format :inline false)
      (#(jdbc/execute-one! tx % {:return-keys true}))))

;;; groups ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def group-id-keys
  [:id
   :institution
   :institutional_id
   :institutional_name])

(def group-changeable-keys
  [:name
   :type])

(def group-keys
  (concat group-id-keys group-changeable-keys))

(defn current-groups [user-id institution tx]
  (-> (sql/from :groups)
      (sql/select :*)
      (sql/join :groups_users [:= :groups_users.group_id :groups.id])
      (sql/where [:= :groups_users.user_id user-id])
      (sql/where [:= :groups.institution institution])
      (sql-format :inline false)
      (#(jdbc/execute! tx % {}))))

(defn group-where [sql properties]
  (assert (-> properties :institution presence))
  (as-> sql sql
    (sql/where sql [:= :institution (:institution properties)])
    (cond
      (:id properties) (sql/where sql [:= :id (:id properties)])
      (:institutional_id
       properties) (sql/where
                    sql [:= :institutional_id
                         (:institutional_id properties)])
      (:institutional_name
       properties) (sql/where
                    sql [:= :institutional_name
                         (:institutional_name properties)]))))

(defn find-group [properties tx]
  (-> (sql/select :*)
      (sql/from :groups)
      (group-where properties)
      (sql-format :inline true)
      ((partial jdbc/execute-one! tx))))

(defn update-group [group properties tx]
  (let [allowed-props (select-keys properties group-changeable-keys)]
    (if (= allowed-props (select-keys group (keys allowed-props)))
      group
      (-> (sql/update :groups)
          (sql/set allowed-props)
          (sql/where [:= :id (:id group)])
          (sql-format :inline false)
          (#(jdbc/execute-one! tx % {:return-keys true}))))))

(defn create-group [properties tx]
  "Tries to create a group but may return nil if properties an not sufficient
  or cause a collision with existing data. "
  (when (:name properties)
    (try
      (-> (sql/insert-into :groups)
          (sql/values [properties])
          (sql-format :inline false)
          (#(jdbc/execute-one! tx % {:return-keys true})))
      (catch Exception ex
        (warn "Failed to create group" ex)))))

(defn create-or-update-group [properties tx]
  "If necessary updates or tries to create a group according to properties.
  Returns the group with full properties or nil, see create-group "
  (if-let [group (find-group properties tx)]
    (update-group group properties tx)
    (create-group properties tx)))

(comment
  (create-or-update-group
   {:institutional_name "uname" :institution "bar.com" :name "first group test"}
   (get-ds)))

(defn create-or-update-target-groups [account institution tx]
  (->> account
       :groups
       (map #(assoc % :institution institution))
       (map #(select-keys % group-keys))
       (map #(create-or-update-group % tx))
       (filter identity)))

(defn add-user-to-groups [user group-ids tx]
  (-> (sql/insert-into :groups_users)
      (sql/values (->> group-ids
                       (map #(assoc {} :group_id % :user_id (:id user)))))
      (sql-format :inline true)
      (#(jdbc/execute! tx % {:return-keys true}))))

(defn remove-user-from-groups [user group-ids tx]
  (when (seq group-ids)
    (-> (sql/delete-from :groups_users)
        (sql/where [:in :group_id group-ids])
        (sql/where [:= :user_id (:id user)])
        (sql-format :inline false)
        (#(jdbc/execute! tx % {})))))

(defn update-groups
  [user account auth-system tx]
  (let [institution (institution! auth-system)
        target-groups (create-or-update-target-groups account institution tx)
        target-ids (->> target-groups (map :id) set)
        current-groups (current-groups (:id user) institution tx)
        current-ids (->> current-groups (map :id) set)]
    (debug target-ids)
    (debug current-ids)
    (when-let [add-ids (seq (clojure.set/difference target-ids current-ids))]
      (debug {:add-ids add-ids})
      (add-user-to-groups user add-ids tx))
    (when-let [remove-ids (seq (clojure.set/difference current-ids target-ids))]
      (debug {:remove-ids remove-ids} remove-ids)
      (remove-user-from-groups user remove-ids tx))))

;;; update ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn manage-account [account auth-system tx]
  (debug 'manage-account {:account account})
  (let [user (get-or-create-user account auth-system tx)]
    (update-user user account auth-system tx)
    (update-person user account auth-system tx)
    #_(update-groups user account auth-system tx)
    user))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
