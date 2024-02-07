(ns madek.auth.http.session
  (:require
   [buddy.core.codecs :refer [bytes->b64 bytes->str]]
   [buddy.core.hash :as hash]
   [cuerdas.core :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :refer [debug-ns]]
   [madek.auth.constants :refer [MADEK_SESSION_COOKIE_NAME MADEK_SIGNED_IN_USERS_GROUP]]
   [madek.auth.utils.core :refer [presence presence!]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]])
  (:import
   [java.util UUID]))

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
        :meta_data [:lift {:user_agent (get-in request [:headers "user-agent"])
                           :remote_addr (get-in request [:remote-addr])}]})))

(defn ensure-default-madek-signed-in-group-exists [tx]
  (let [group-id (UUID/fromString (:id MADEK_SIGNED_IN_USERS_GROUP))]
    (when-not (-> (sql/select true)
                  (sql/from :groups)
                  (sql/where [:= :id group-id])
                  (sql-format)
                  (#(jdbc/execute-one! tx %)))
      (-> (sql/insert-into :groups)
          (sql/values [(update-in MADEK_SIGNED_IN_USERS_GROUP
                                  [:id] #(UUID/fromString %))])
          (sql-format)
          (#(jdbc/execute-one! tx %))))))

(defn add-to-standard-authentication-group [{user-id :user_id :as user-session} tx]
  (let [group-id (UUID/fromString (:id MADEK_SIGNED_IN_USERS_GROUP))]
    (ensure-default-madek-signed-in-group-exists tx)
    (-> (sql/insert-into :groups_users)
        (sql/values [{:user_id user-id :group_id group-id}])
        (sql/upsert (-> (sql/on-conflict :user_id :group_id)
                        (sql/do-nothing)))
        (sql-format)
        (#(jdbc/execute-one! tx %)))))

(defn update-last-signed-in-at [{user-id :user_id} tx]
  (-> (sql/update :users)
      (sql/set {:last_signed_in_at [:now]})
      (sql/where [:= :id user-id])
      (sql-format)
      (#(jdbc/execute-one! tx %))))

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
    (add-to-standard-authentication-group user-session tx)
    (update-last-signed-in-at user-session tx)
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
   [:* :auth_systems.session_max_lifetime_hours [:raw "INTERVAL '1 hour'"]]])

(def selects
  [[:auth_systems.id :auth_system_id]
   [:auth_systems.name :auth_system_name]
   [:first_name :user_first_name]
   [:last_name :user_last_name]
   [:user_sessions.created_at :session_created_at]
   [:user_sessions.id :session_id]
   [:users.email :user_email]
   [:users.id :user_id]
   [:users.institutional_id :user_institutional_id]
   [:users.login :user_login]
   [expiration-sql-expr :session_expires_at]])

(defn user-session-query [token-hash]
  (-> (apply sql/select selects)
      (sql/from :user_sessions)
      (sql/join :users [:= :user_sessions.user_id :users.id])
      (sql/join :auth_systems [:= :user_sessions.auth_system_id :auth_systems.id])
      (sql/where [:= :user_sessions.token_hash token-hash])
      (sql/where [:<= [:now] expiration-sql-expr])))

(defn user-session [token-hash tx]
  (-> token-hash
      user-session-query
      (sql-format :inline true)
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
    (debug 'session/handler)
    (-> req authenticate handler)))

;#### debug ###################################################################
;(debug-ns *ns*)
