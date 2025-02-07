(ns madek.auth.routing.resolve
  (:refer-clojure :exclude [resolve])
  (:require
   [logbug.debug :refer [debug-ns]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.request :as sign-in-user-auth-system-external-request]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.sign-in :as sign-in-user-auth-system-external-sign-in]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.forgot :as sign-in-user-auth-system-password-forgot]
   [madek.auth.resources.sign-in.auth-systems.auth-system.ldap.request :as sign-in-user-auth-system-ldap-request]
   [madek.auth.resources.sign-in.auth-systems.auth-system.ldap.sign-in :as sign-in-user-auth-system-ldap-sign-in]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.request :as sign-in-user-auth-system-password-request]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.reset :as sign-in-user-auth-system-password-reset]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.sign-in :as sign-in-user-auth-system-password-sign-in]
   [madek.auth.resources.sign-in.auth-systems.main :as sign-in-user-auth-systems]
   [madek.auth.resources.sign-out.main :as sign-out]
   [madek.auth.routes :as routes]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defn resolve [{{route-name :name} :data path-params :path-params :as route}]
  (debug "ROUTE" route-name path-params route)
  (case route-name
    :sign-in-user-auth-systems #'sign-in-user-auth-systems/handler

    :sign-in-user-auth-system-password-forgot (case (:auth_system_type path-params)
                                                "password" #'sign-in-user-auth-system-password-forgot/handler
                                                (warn "No matching :auth_system_type" (:auth_system_type path-params)))
    :sign-in-user-auth-system-password-reset (case (:auth_system_type path-params)
                                               "password" #'sign-in-user-auth-system-password-reset/dispatch
                                               (warn "No matching :auth_system_type" (:auth_system_type path-params)))
    :sign-in-user-auth-system-request (case (:auth_system_type path-params)
                                        "external" #'sign-in-user-auth-system-external-request/handler
                                        "ldap" #'sign-in-user-auth-system-ldap-request/handler
                                        "password" #'sign-in-user-auth-system-password-request/handler
                                        (warn "No matching :auth_system_type" (:auth_system_type path-params)))
    :sign-in-user-auth-system-sign-in (case (:auth_system_type path-params)
                                        "external" #'sign-in-user-auth-system-external-sign-in/handler
                                        "ldap" #'sign-in-user-auth-system-ldap-sign-in/handler
                                        "password" #'sign-in-user-auth-system-password-sign-in/handler
                                        (warn "No matching :auth_system_type" (:auth_system_type path-params)))
    :sign-out sign-out/handler))

(defn route-resolve [handler request]
  (debug 'route-resolve (:uri request))
  (if-let [route (some-> request :uri routes/route)]
    (let [{{route-name :name} :data} route
          params-coerced (routes/coerce-params route)
          ; replace plain :path-params with coerced ones
          route (update route :path-params
                        #(merge {} % (:path params-coerced)))]
      (handler (-> request (assoc :route route
                                  :route-name route-name
                                  :route-handler (resolve route))
                   (update-in [:params] #(merge {} % (:path-params route))))))
    (handler request)))

;#### debug ###################################################################
;(debug-ns *ns*)
