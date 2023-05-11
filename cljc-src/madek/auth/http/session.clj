(ns madek.auth.http.session
  (:require
    [buddy.core.codecs :refer [bytes->b64 bytes->str]]
    [buddy.core.hash :as hash]
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :refer [debug-ns]]
    [madek.auth.constants :refer [MADEK_SESSION_COOKIE_NAME]]
    [madek.auth.utils.core :refer [presence presence!]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]
    )
  (:import
    [java.util UUID]
    ))


;#### helper ##################################################################

(defn token-hash [token]
  (-> token hash/sha256 bytes->b64 bytes->str))


;#### create ##################################################################

(defn session-data [user-auth-system token request]
  (-> user-auth-system
      (select-keys [:auth_system_id :user_id])
      (merge 
        {:token_hash (token-hash token) 
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


;#### wrap ####################################################################


(def expiration-sql-expr
  [:+ :user_sessions.created_at 
   [:* :auth_systems.session_max_lifetime_minutes [:raw "INTERVAL '1 minute'"]]])

(def selects 
  [
   [:auth_systems.id :auth_system_id]
   [:auth_systems.name :auth_system_name]
   [:people.first_name :person_first_name]
   [:people.institutional_id :person_institutional_id]
   [:people.last_name :person_last_name]
   [:people.pseudonym :person_pseudonym]
   [:user_sessions.created_at :session_created_at]
   [:user_sessions.id :session_id]
   [:users.email :user_email]
   [:users.id :user_id]
   [:users.institutional_id :user_institutional_id]
   [:users.login :user_login]
   [expiration-sql-expr :session_expires_at]
   ])

(defn user-session-query [token-hash]
  (-> (apply sql/select selects)
      (sql/from :user_sessions)
      (sql/join :users [:= :user_sessions.user_id :users.id])
      (sql/join :people [:= :people.id :users.person_id])
      (sql/join :auth_systems [:= :user_sessions.auth_system_id :auth_systems.id])
      (sql/where [:= :user_sessions.token_hash token-hash])
      (sql/where [:<= [:now] expiration-sql-expr])))

(defn user-session [token-hash tx]
  (-> token-hash
      user-session-query
      (sql-format :inline true)
      spy
      (#(jdbc/execute-one! tx %))))

(defn session-token-hashed [request]
  (some-> request :cookies (get MADEK_SESSION_COOKIE_NAME nil) 
          :value token-hash))

(defn authenticate [{tx :tx :as request}]
  (if-let [user-session (some-> request 
                                session-token-hashed
                                (user-session tx))]
    (assoc request :authenticated-entity user-session)
    request))


(defn wrap [handler]
  (fn [req]
    (info 'session/handler)
    (-> req authenticate handler)))


;#### debug ###################################################################
(debug-ns *ns*)
