(ns madek.auth.utils.emails
  (:require
   [clojure.spec.alpha :as spec]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn insert-into-emails! [tx user-id to from subject body]
  (-> (sql/insert-into :emails)
      (sql/values [{:user_id user-id,
                    :to_address to,
                    :from_address from,
                    :subject subject,
                    :body body}])
      sql-format
      (->> (jdbc/execute! tx))))

(defn send-email! [{tx :tx, {from :smtp_default_from_address} :settings}
                   {user-id :id to :email} subject body]
  (insert-into-emails! tx user-id to from subject body))
