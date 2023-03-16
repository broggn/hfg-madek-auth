(ns madek.auth.routing.resolve
  (:require 
    [madek.auth.resources.sign-in.auth-systems.main :as sign-in-user-auth-systems]
    [madek.auth.resources.sign-in.auth-systems.auth-system.request :as sign-in-user-auth-system-request]
    [madek.auth.resources.sign-in.auth-systems.auth-system.sign-in :as sing-in-user-auth-system-sign-in]))

(def resolve-table
  {:sign-in-user-auth-systems  #'sign-in-user-auth-systems/handler
   :sign-in-user-auth-system-request #'sign-in-user-auth-system-request/handler
   :sign-in-user-auth-system-sign-in #'sing-in-user-auth-system-sign-in/handler
   })

