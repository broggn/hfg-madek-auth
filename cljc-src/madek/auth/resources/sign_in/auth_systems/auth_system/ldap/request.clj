(ns madek.auth.resources.sign-in.auth-systems.auth-system.ldap.request
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))


(defn handler [{{email-or-login :email-or-login} :params tx :tx :as request}]
  (let [auth-system [{:auth_system_id "ldap"
                      :auth_system_type "ldap"
                      :email email-or-login}]]
    {:body auth-system :status 200}))
