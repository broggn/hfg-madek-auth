(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.sign-in
  (:require
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :refer [debug-ns]]
    [madek.auth.db.core :refer [get-ds]]
    [madek.auth.http.session :refer [create-user-session-response]]
    [madek.auth.resources.sign-in.auth-systems.sql :as auth-systems-sql]
    [madek.auth.routes :refer [path]]
    [madek.auth.utils.core :refer [presence]]
    [next.jdbc :as jdbc]
    [tick.core :as time]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn auth-system-user-password-hash-query [email-or-login]
  (-> (sql/from :users)
      (sql/where (auth-systems-sql/user-cond email-or-login))
      (sql/join :auth_systems_users [:= :auth_systems_users.user_id :users.id])
      (sql/join :auth_systems [:= :auth_systems_users.auth_system_id :auth_systems.id])
      (sql/where [:= :auth_systems.id "password"])
      (sql/where [:or 
                  [:= :auth_systems_users.expires_at nil]
                  [:> :auth_systems_users.expires_at [:now]]])
      (sql/select [:auth_systems_users.data :password_hash]
                  [:auth_systems.id :auth_system_id]
                  [:users.id :user_id])))

(defn password-check-query [password password-hash]
  (sql/select [[:= password-hash 
                [:crypt password password-hash]]
               :password_matches]))

(defn handler [{{auth_system_id :auth_system_id
                 email-or-login :email-or-login} :params 
                {password :password} :body
                tx :tx :as request}]
  (debug auth_system_id email-or-login password)
  (if-let [res (some-> email-or-login 
                       auth-system-user-password-hash-query
                       (sql-format :inline true)
                       (#(jdbc/execute-one! tx %)))]
    (if (some-> (password-check-query password (:password_hash res))
                (sql-format :inline false)
                (#(jdbc/execute-one! tx %)) :password_matches) 
      (create-user-session-response res request)
      {:status 401 :body {:message "Password missmatch"}})
    {:status 401 :body {:message "Password authentication is not available"}}))




;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(debug-ns *ns*)
