(ns madek.auth.resources.sign-out.main
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn handler [{tx :tx :as request}]
  (if-let [session-id (some-> request :authenticated-entity :session_id)]
    (if (-> (sql/delete-from :user_sessions)
            (sql/where [:= :id session-id])
            (sql-format)
            (#(jdbc/execute-one! tx % {:return-keys true})))
      {:status 204}
      (throw (ex-info "Session was not deleted" {})))
    {:status 401}))
