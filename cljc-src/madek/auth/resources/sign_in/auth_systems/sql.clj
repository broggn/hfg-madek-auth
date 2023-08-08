(ns madek.auth.resources.sign-in.auth-systems.sql
  (:require
    [logbug.debug :refer [debug-ns]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [honey.sql.pg-ops :as pg-sql]
    [madek.auth.db.core :refer [get-ds]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))



(defn auth-systems-match-cond [email-or-login]
  [:and 
   [:<> :auth_systems.email_or_login_match nil]
   [pg-sql/regex email-or-login :auth_systems.email_or_login_match]])


(defn user-cond [email-or-login]
  [:and [:<> :users.is_deactivated true]
   [:or 
    [:= [:lower :users.email] [:lower email-or-login]]
    [:= :users.login email-or-login]]])


(defn users-matches-and-connected-cond [email-or-login]
  "Check if the user is connected to the auth_system. The connection
  can be direct, via a group or match via email_or_login_match."
  [:and 
   (user-cond email-or-login)
   [:or 
    (auth-systems-match-cond email-or-login)
    [:exists 
     (-> (sql/select true)
         (sql/from [:auth_systems_users :asus])
         (sql/where [:= :asus.auth_system_id :auth_systems.id])
         (sql/where [:= :asus.user_id :users.id])
         (sql/where [:or 
                     [:= :asus.expires_at nil]
                     [:> :asus.expires_at [:now]]]))]
    [:exists (-> (sql/select true) 
                 (sql/from :groups_users)
                 (sql/where [:= :groups_users.user_id :users.id])
                 (sql/join :groups [:= :groups.id :groups_users.group_id])
                 (sql/join [:auth_systems_groups :asgs] [:= :asgs.group_id :groups.id])
                 (sql/where [:= :auth_systems.id :asgs.auth_system_id]))]]])

(defn users-matches-and-connected [email-or-login]
  (-> (sql/from :users)
      (sql/where (users-matches-and-connected-cond email-or-login))))

(defn auth-systems-query [email-or-login]
  (-> (sql/from :auth_systems)
      (sql/select 
        [:auth_systems.external_sign_in_url :auth_system_url]
        [:auth_systems.id :auth_system_id] 
        [:auth_systems.name :auth_system_name]
        [:auth_systems.type :auth_system_type]
        [:users.id :user_id]
        [:users.email :email] 
        [:users.login :login])
      (sql/where [:= :auth_systems.enabled true])
      (sql/where [:or 
                  (auth-systems-match-cond email-or-login)
                  [:exists (-> email-or-login users-matches-and-connected
                               (sql/select true))]])
      (sql/left-join 
        :users (users-matches-and-connected-cond email-or-login))
      (sql/order-by [:priority :desc])))


(comment 
  (-> "tashia_sawayn_a44f13e7@boyer.example" 
      auth-systems-query
      (sql-format :inline true)
      (#(jdbc/execute-one! (get-ds) %))
      ))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
