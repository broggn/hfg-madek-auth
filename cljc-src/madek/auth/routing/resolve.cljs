(ns madek.auth.routing.resolve
  (:require 
    [madek.auth.resources.auth.main :as auth]
    [madek.auth.resources.info.main :as info]
    [madek.auth.resources.sign-in.auth-systems.auth-system.request :as sign-in-auth-system-request]
    [madek.auth.resources.sign-in.auth-systems.auth-system.sign-in :as sign-in-auth-system-sign-in]
    [madek.auth.resources.sign-in.auth-systems.main :as sign-in-auth-systems]
    [madek.auth.resources.sign-in.main :as sign-in]))

(def routes-resources
  {:auth auth/components
   :info info/components
   :sign-in sign-in/components
   :sign-in-user-auth-systems sign-in-auth-systems/components
   :sign-in-user-auth-system-request sign-in-auth-system-request/components
   :sign-in-user-auth-system-sign-in sign-in-auth-system-sign-in/components})

