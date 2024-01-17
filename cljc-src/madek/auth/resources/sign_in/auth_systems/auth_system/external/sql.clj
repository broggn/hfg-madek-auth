(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.sql
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :refer [debug-ns]]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn extended-auth-systems-query [email-or-login auth_system_id]
  (-> email-or-login auth-systems-query
      (sql/select [:auth_systems.internal_private_key :internal_private_key])
      (sql/select [:auth_systems.external_sign_in_url :external_sign_in_url])
      (sql/where [:= :auth_systems.id auth_system_id])))
