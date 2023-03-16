(ns madek.auth.http.session
  (:require
    [buddy.core.codecs :refer [bytes->b64]]
    [buddy.core.hash :as hash]
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [madek.auth.constants :refer [MADEK_SESSION_COOKIE_NAME]]
    [madek.auth.constants :refer []]
    [madek.auth.utils.core :refer [presence presence!]]
    [next.jdbc :as jdbc]
    )
  (:import
    [java.util UUID]
    ))


(defn session-data [user-auth-system token request]
  (-> user-auth-system
      (select-keys [:auth_system_id :user_id])
      (merge 
        {:token_hash (-> token hash/sha256 bytes->b64) 
         :token_part (str/slice token 0 5)
         :meta_data  [:lift {:user_agent (get-in request [:headers "user-agent"])
                             :remote_addr (get-in request [:remote-addr])}]})))

(defn create-user-session-response
  [user-auth-system {tx :tx :as request}]
  "Create and returns the user_session. The map includes additionally
  the original token to be used as the value of the session cookie."
  (let [token (str (UUID/randomUUID))
        user-session (-> (sql/insert-into :user_sessions)
                         (sql/values [(session-data user-auth-system 
                                                    token request)])
                         (sql-format)
                         (#(jdbc/execute-one! tx % {:return-keys true})))]
    (assoc user-session :token token)
    {:status 200
     :body {:user_session user-session}
     :cookies {MADEK_SESSION_COOKIE_NAME
               {:value token
                :http-only true
                :path "/"
                :secure false}}}))


;#### debug ###################################################################
;(debug-ns *ns*)
