(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.request
  (:require
   [buddy.core.keys :as keys]
   [buddy.sign.jwt :as jwt]
   [cuerdas.core :as str]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [logbug.debug :refer [debug-ns]]
   [madek.auth.db.core :refer [get-ds]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.pki :refer [private-key! public-key!]]
   [madek.auth.resources.sign-in.auth-systems.sql :refer [auth-systems-query]]
   [madek.auth.routes :refer [path]]
   [madek.auth.utils.core :refer [presence]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]
   [tick.core :as time]))

(defn extended-auth-systems-query [email-or-login auth_system_id]
  (-> email-or-login auth-systems-query
      (sql/select [:auth_systems.internal_private_key :internal_private_key])
      (sql/select [:auth_systems.external_sign_in_url :external_sign_in_url])
      (sql/where [:= :auth_systems.id auth_system_id])))

(defn claims! [email-or-login sign-in-url return-to]
  {:exp (time/>> (time/now) (time/of-seconds 90))
   :iat (time/now)
   :return-to (presence return-to)
   :email-or-login email-or-login
   :sign-in-url sign-in-url})

; base-url: madek does not have this value stored in db/settings it is
; somewhere in the settings which makes it (not only) hard for testing; so we
; take the value from the frontend

(defn handler [{{email-or-login :email-or-login
                 auth_system_id :auth_system_id
                 return-to :return-to} :params
                {base-url :base-url} :body
                tx :tx :as request}]
  (debug 'request request)
  (if-let [auth-system (-> (extended-auth-systems-query
                            email-or-login auth_system_id)
                           (sql-format :inline false)
                           (#(jdbc/execute-one! tx %)))]
    (let [priv-key (-> auth-system :internal_private_key private-key!)
          sign-in-url (str base-url
                           (path :sign-in-user-auth-system-sign-in
                                 (select-keys auth-system
                                              [:auth_system_type :auth_system_id])))
          claims (claims! email-or-login sign-in-url return-to)
          token (jwt/sign claims priv-key {:alg :es256})]
      (debug {:claims claims})
      {:status 200
       :body (merge {}
                    (select-keys auth-system [:external_sign_in_url])
                    {:token token})})
    (throw (ex-info "No suiteable authentication system found" {:status 422}))))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(debug-ns *ns*)
