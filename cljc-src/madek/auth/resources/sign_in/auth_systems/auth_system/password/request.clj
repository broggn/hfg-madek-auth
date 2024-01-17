(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.request
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn password-auth-system-query [email-or-login]
  (-> email-or-login auth-systems-query
      (sql/select :auth_systems.*)
      (sql/where [:= :auth_systems.id "password"])))

(defn password-auth-system! [email-or-login tx]
  (let [auth-system (-> (password-auth-system-query email-or-login)
                        (sql-format :inline true)
                        (#(jdbc/execute-one! tx %)))]
    (when (nil? auth-system)
      (throw (ex-info "Password authentication not available for user"
                      {:status 401})))
    auth-system))

(defn handler [{{email-or-login :email-or-login} :params tx :tx :as request}]
  (let [auth-system (password-auth-system! email-or-login tx)]
    {:body auth-system :status 200}))
