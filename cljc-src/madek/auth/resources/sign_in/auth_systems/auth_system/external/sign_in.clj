(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.sign-in
  (:require
   [buddy.core.keys :as keys]
   [buddy.sign.jwt :as jwt]
   [cuerdas.core :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :refer [debug-ns]]
   [madek.auth.db.core :refer [get-ds]]
   [madek.auth.http.session :refer [create-user-session-response]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.manage :refer [manage-account]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.pki :refer [private-key! public-key!]]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [madek.auth.routes :refer [path]]
   [madek.auth.utils.core :refer [presence]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]
   [tick.core :as time]))

(defn extended-auth-system-user-query [email-or-login auth_system_id]
  (-> email-or-login auth-systems-query
      (sql/select :auth_systems.*)
      (sql/where [:= :auth_systems.id auth_system_id])))

(defn auth-system-user! [email-or-login auth_system_id tx]
  (let [auth-system-user (-> (extended-auth-system-user-query email-or-login auth_system_id)
                             (sql-format :inline true)
                             (#(jdbc/execute-one! tx %)))]
    (when-not (:user_id auth-system-user)
      (throw (ex-info "No match of user account and authentication system"
                      {:status 401})))
    auth-system-user))

(defn auth-system [auth_system_id tx]
  (-> (sql/select :*)
      (sql/from :auth_systems)
      (sql/where [:= :auth_systems.id auth_system_id])
      sql-format
      (#(jdbc/execute-one! tx %))))

;;; token and claims validation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn validate-request-claims!
  "Check that the request token is not expired. 
  At a later point possibly also validate to a nonce."
  [sign-in-request-token internal-pub-key]
  (jwt/unsign sign-in-request-token internal-pub-key {:alg :es256}))

(defn validate-response-claims! [request-claims response-claims]
  (when-not (= true (:success response-claims))
    (throw (ex-info "The claims returned from external auth does not indicate successful authentication."
                    {:status 401
                     :body {:request-claims request-claims
                            :response-claims response-claims
                            :error_message (:error_message response-claims)}}))))

(defn validate-and-extract-response-token! [auth-system token tx]
  (let [external-pub-key (-> auth-system :external_public_key public-key!)
        internal-pub-key (-> auth-system :internal_public_key public-key!)
        {sign-in-request-token :sign_in_request_token
         :as response-claims} (jwt/unsign token external-pub-key {:alg :es256})
        request-claims (validate-request-claims! sign-in-request-token internal-pub-key)
        _ (validate-response-claims! request-claims response-claims)]
    [request-claims response-claims]))

;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handler [{{auth_system_id :auth_system_id} :params
                {token :token} :body
                tx :tx :as request}]
  (if-let [auth-system (auth-system auth_system_id tx)]
    (let [[request-claims
           response-claims] (validate-and-extract-response-token!
                             auth-system token tx)

          {email-or-login :email-or-login
           account :account
           :as response-properties} response-claims]
      (when (:manage_accounts auth-system)
        (manage-account account auth-system tx))
      (let [auth-system-user (auth-system-user!
                              email-or-login (:id auth-system) tx)]
        (update-in
         (create-user-session-response auth-system-user request)
         [:body]
         #(merge % {:request-claims request-claims
                    :response-claims response-claims}))))
    {:status 404
     :body {:error_message "Authentication system not found."}}))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
