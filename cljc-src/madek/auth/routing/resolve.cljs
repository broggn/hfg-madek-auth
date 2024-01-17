(ns madek.auth.routing.resolve
  (:refer-clojure :exclude [resolve])
  (:require
   [madek.auth.resources.auth.main :as auth]
   [madek.auth.resources.info.main :as info]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.request :as sign-in-auth-system-external-request]
   [madek.auth.resources.sign-in.auth-systems.auth-system.external.sign-in :as sign-in-auth-system-external-sign-in]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.request :as sign-in-auth-system-password-request]
   [madek.auth.resources.sign-in.auth-systems.main :as sign-in-auth-systems]
   [madek.auth.resources.sign-in.main :as sign-in]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defn resolve [match]
  (case (get-in match [:data :name])
    :auth auth/components
    :info info/components
    :sign-in sign-in/components
    :sign-in-user-auth-systems sign-in-auth-systems/components
    :sign-in-user-auth-system-request (case (get-in match [:path-params :auth_system_type])
                                        "external" sign-in-auth-system-external-request/components
                                        "password" sign-in-auth-system-password-request/components)
    :sign-in-user-auth-system-sign-in (case (get-in match [:path-params :auth_system_type])
                                        "external" sign-in-auth-system-external-sign-in/components)))
