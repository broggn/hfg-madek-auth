(ns madek.auth.resources.sign-in.auth-systems.auth-system.ldap.sign-in
  (:require [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [madek.auth.http.session :refer [create-user-session-response]]
            [madek.auth.resources.sign-in.auth-systems.auth-system.ldap.ldap :refer [ldap-auth]]
            [madek.auth.resources.sign-in.auth-systems.auth-system.ldap.manage :refer [manage-account]]
            [madek.auth.resources.sign-in.auth-systems.sql :as auth-systems-sql]
            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [info]]))

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
  (info "ldap-sign-in " auth_system_id email-or-login password)
 
  (if-let [ldap-user (ldap-auth email-or-login password)]
    (let [auth-system {:type "ldap" :id "ldap" :managed_domain "hfg-karlsruhe.de"}
          newUser (manage-account ldap-user auth-system tx)
          newAuthUser {:user_id (:id newUser) :auth_system_id "password"}]
      (info "ldap-sign-in: created/updated user: " newUser)
          ;(let [newAuthUser (some-> email-or-login
          ;                      auth-system-user-password-hash-query
          ;                      (sql-format :inline true)
          ;                      (#(jdbc/execute-one! tx %)))]
      (info "ldap-sign-in: login new user: " newAuthUser)
      (create-user-session-response newAuthUser request)
            ;)
      )
  
  
        ; or invalid ldap user
    {:status 401 :body {:message "LDAP Password authentication is not available"}})
    )

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
