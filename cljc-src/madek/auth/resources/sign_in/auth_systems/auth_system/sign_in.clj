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
                    {:status 403}))))

(defn validate-request-claims! 
  "Check that the request token is not expired. 
  At a later point possibly also validate to a nonce."
  [sign-in-request-token internal-pub-key]
  (jwt/unsign sign-in-request-token internal-pub-key {:alg :es256}))


(defn handler [{{auth_system_id :auth_system_id
                 int-email :email} :params 
                {token :token} :body
                tx :tx :as request}]
  (if-let [user-auth-system (-> (query int-email auth_system_id)                
                                (sql-format :inline false)
                                (#(jdbc/execute-one! tx %)) spy)]
    (let [external-pub-key (-> user-auth-system :external_public_key public-key!)
          internal-pub-key (-> user-auth-system :internal_public_key public-key!)
          {ext-email :email 
           sign-in-request-token :sign_in_request_token 
           :as claims} (jwt/unsign token external-pub-key {:alg :es256})
          request-claims (validate-request-claims! sign-in-request-token internal-pub-key)]
      (validate-email-equality! int-email ext-email)
      (update-in 
        (create-user-session-response user-auth-system request)
        [:body]
        #(merge % {:request-claims request-claims
                   :response-claims claims})))
    {:status 403}))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(debug-ns *ns*)
