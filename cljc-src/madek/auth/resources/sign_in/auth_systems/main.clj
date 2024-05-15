(ns madek.auth.resources.sign-in.auth-systems.main
  (:require
   [clojure.string :refer [trim]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.db.core :refer [get-ds]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.ldap.ldap :refer [ldap-has-user]]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn auth-systems [email tx]
  (-> email
      auth-systems-query
      sql-format
      (#(jdbc/execute! tx %))))

#_(defn handler [{{email-or-login :email-or-login} :params tx :tx :as request}]
  (if-let [auth-system-user (auth-systems (some-> email-or-login trim) (get-ds))]
    {:body auth-system-user
     :status 200}
    (when (= true (ldap-has-user email-or-login))
      (info "no auth system user but found ldap user")
      {:body [{:auth_system_id "ldap"
               :auth_system_type "ldap"
               :email email-or-login}]
       :status 200})))

(defn handle_auth_user [auth-system-user]
  (info "handle_auth_user " auth-system-user)
  {:body auth-system-user :status 200})

(defn handle_no_auth_user [email-or-login]
  (info "handle_no_auth_user " email-or-login)
  (if (= true (ldap-has-user email-or-login))

    (handle_auth_user [{:auth_system_id "ldap"
                        :auth_system_type "ldap"
                        :email email-or-login}])
    (handle_auth_user [])))

(defn handler [{{email-or-login :email-or-login} :params tx :tx :as request}]
  (info "signin main" email-or-login)
  (try
    
    (let [auth-systems (auth-systems (some-> email-or-login trim) (get-ds))]
      (if (first auth-systems)
        (handle_auth_user auth-systems)
        (handle_no_auth_user email-or-login)))
    (catch Exception ex (warn "signin main got exception" ex))
  ))


