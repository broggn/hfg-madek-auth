(ns madek.auth.resources.sign-in.auth-systems.auth-system.sign-in
  (:require
    [buddy.core.keys :as keys]
    [buddy.sign.jwt :as jwt]
    [cuerdas.core :as str]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [logbug.debug :refer [debug-ns]]
    [madek.auth.db.core :refer [get-ds]]
    [madek.auth.http.session :refer [create-user-session-response]]
    [madek.auth.resources.sign-in.auth-systems.auth-system.pki :refer [private-key! public-key!]]
    [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
    [madek.auth.routes :refer [path]]
    [madek.auth.utils.core :refer [presence]]
    [next.jdbc :as jdbc]
    [tick.core :as time]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn query [email auth_system_id]
  (-> email auth-systems-query 
      (sql/select-distinct 
        [:auth_systems.external_public_key :external_public_key]
        [:auth_systems.internal_public_key :internal_public_key])
      (sql/where [:= :auth_systems.id auth_system_id])))


(defn validate-email-equality! 
  "Validate equality of used (requested) email and the
  one returned by the external authentication system."
  [int-email ext-email]
  (when-not (= (str/lower int-email)
               (str/lower ext-email))
    (throw (ex-info (str/join 
                      ["Provided email address may not differ from the email " 
                       "address returned by the external authentication system"])
                    {:status 401}))))

(defn validate-request-claims! 
  "Check that the request token is not expired. 
  At a later point possibly also validate to a nonce."
  [sign-in-request-token internal-pub-key]
  (jwt/unsign sign-in-request-token internal-pub-key {:alg :es256}))


(defn auth-system [auth_system_id tx]
  (-> (sql/select :*)
      (sql/from :auth_systems)
      (sql/where [:= :auth_systems.id auth_system_id])
      sql-format
      (#(jdbc/execute-one! tx %))))

(defn validate-response-claims! [request-claims response-claims]
  (when-not (= true (:success response-claims))
    (throw (ex-info "The claims returned from external auth does not indicate successful authentication."
                    {:status 401
                     :body {:request-claims request-claims
                            :response-claims response-claims
                            :error_message (:error_message response-claims)}}))))


(defn handler [{{auth_system_id :auth_system_id} :params 
                {token :token} :body
                tx :tx :as request}]
  (if-let [auth-system (auth-system auth_system_id tx)]
    (let [external-pub-key (-> auth-system :external_public_key public-key!)
          internal-pub-key (-> auth-system :internal_public_key public-key!)
          {ext-email :email 
           sign-in-request-token :sign_in_request_token 
           :as response-claims} (jwt/unsign token external-pub-key {:alg :es256})
          request-claims (validate-request-claims! sign-in-request-token internal-pub-key)
          _ (validate-response-claims! request-claims response-claims)
          user-auth-system (-> (query (:email response-claims) auth_system_id)                
                               spy
                               (sql-format :inline true)
                               spy
                               (#(jdbc/execute-one! tx %))
                               spy
                               )]
      (when-not user-auth-system 
        (throw (ex-info "No suitable authentication-system found!" 
                        {:status 401})))
      (update-in 
        (create-user-session-response user-auth-system request)
        [:body]
        #(merge % {:request-claims request-claims
                   :response-claims response-claims})))
    {:status 401
     :body {:error_message "No authentication system found."}}))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(debug-ns *ns*)
