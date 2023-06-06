(ns madek.auth.resources.sign-in.auth-systems.sql
  (:require
    [next.jdbc :as jdbc]
    [madek.auth.db.core :refer [get-ds]]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [taoensso.timbre :refer [debug error info spy warn]]))


; TODO that part with the email match when sign-up is enabled
(def auth-systems-users-sql-cond
  [:or 
   [:exists (-> (sql/select true)
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
                (sql/where [:= :auth_systems.id :asgs.auth_system_id])
                )]])

(defn auth-systems-query [email]
  (-> (sql/from :auth_systems :users)
      (sql/select-distinct [:auth_systems.id :auth_system_id] 
                           [:auth_systems.name :auth_system_name]
                           [:auth_systems.type :auth_system_type]
                           [:users.id :user_id]
                           [:users.email :email] 
                           [:users.login :login])
      (sql/where auth-systems-users-sql-cond)
      (sql/where [:= :auth_systems.enabled true])
      (sql/where [:= :users.is_deactivated false]) ; we might have to change this for ad-hoc sign-up
      (sql/where [:= [:lower :users.email] [:lower email]])))

