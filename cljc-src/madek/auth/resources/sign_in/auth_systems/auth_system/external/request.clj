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
    [tick.core :as time]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn query [email auth_system_id]
  (-> email auth-systems-query 
      (sql/select-distinct [:auth_systems.internal_private_key :internal_private_key])
      (sql/select-distinct [:auth_systems.external_sign_in_url :external_sing_in_url])
      (sql/where [:= :auth_systems.id auth_system_id])))


; base-url: madek does not have this value stored in db/settings it is
; somewhere in the settings which makes it (not only) hard for testing;
; so we take the value from the front end

(defn claims! [user-auth-system base-url return-to]
  (-> user-auth-system
      (select-keys [:email :login])
      (merge 
        {:exp (time/>> (time/now) (time/of-seconds 90))
         :iat (time/now)
         :return-to (presence return-to)
         :sign-in-url (str base-url
                           (path :sign-in-user-auth-system-sign-in
                                 (select-keys user-auth-system 
                                              [:auth_system_type :auth_system_id :email])))})))

(defn handler [{{email :email
                 auth_system_id :auth_system_id
                 return-to :return-to} :params 
                {base-url :base-url} :body
                tx :tx :as request}]
  (debug 'request request)
  (if-let [user-auth-system (-> (query email auth_system_id)                
                                (sql-format :inline false)
                                (#(jdbc/execute-one! tx %)))]
    (let [priv-key (-> user-auth-system :internal_private_key private-key!)
          claims (claims! user-auth-system base-url return-to)
          token (jwt/sign claims priv-key {:alg :es256})]
      {:body (merge {} 
                    (select-keys user-auth-system [:external_sing_in_url])
                    {:token token })})
    {:status 402}))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(debug-ns *ns*)
