(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.sign-in
  (:require
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :refer [debug-ns]]
    [madek.auth.db.core :refer [get-ds]]
    [madek.auth.http.session :refer [create-user-session-response]]
    [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
    [madek.auth.routes :refer [path]]
    [madek.auth.utils.core :refer [presence]]
    [next.jdbc :as jdbc]
    [tick.core :as time]
    [taoensso.timbre :refer [debug error info spy warn]]))



(defn auth-system-user-password-hash-query [email]
  (-> (sql/select [:auth_systems_users.data :password_hash]
                  :auth_systems_users.auth_system_id
                  :auth_systems_users.user_id)
      (sql/from :users)
      (sql/join :auth_systems_users [:= :auth_systems_users.user_id :users.id])
      (sql/where [:= :auth_systems_users.auth_system_id "password"])
      (sql/where [:in :users.id 
                  (-> email auth-systems-query
                      (dissoc :select-distinct)
                      (sql/select-distinct :users.id)
                      )])))

(comment 
  (-> "elma_9a23a0f5@ondricka-flatley.example"
      auth-system-user-password-hash-query
      (sql-format :inline true)
      ))

(defn password-check-query [password password-hash]
  (sql/select [[:= password-hash 
                [:crypt password password-hash]]
               :password_matches]))

(defn handler [{{auth_system_id :auth_system_id
                 email :email} :params 
                {password :password} :body
                tx :tx :as request}]
  (debug auth_system_id email password)
  (if-let [res (some-> email 
                       auth-system-user-password-hash-query
                       (sql-format :inline false)
                       (#(jdbc/execute-one! tx %))
                       )]
    (if (some-> (password-check-query password (:password_hash res))
                (sql-format :inline false)
                (#(jdbc/execute-one! tx %)) :password_matches) 
      (create-user-session-response res request)
      {:status 401 :body {:message "Password missmatch"}})
    {:status 401 :body {:message "Password authentication is not available"}}))




;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(debug-ns *ns*)


