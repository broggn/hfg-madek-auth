(ns madek.auth.utils.logging
  (:require
   #?(:clj [taoensso.timbre.tools.logging])
   [taoensso.timbre :as timbre :refer [debug info]]
   [taoensso.timbre.appenders.core :as appenders]))

(def LOGGING_CONFIG
  {:min-level [[#{;"madek.auth.*"
                  ;"madek.auth.http.*"
                  ;"madek.auth.http.session"
                  ;"madek.auth.main"
                  ;"madek.auth.resources.sign-in.auth-systems.auth-system.external.*"
                  ;"madek.auth.resources.sign-in.auth-systems.auth-system.external.manage"
                  ;"madek.auth.resources.sign-in.auth-systems.auth-system.external.sign-in"
                  ;"madek.auth.resources.sign-in.auth-systems.auth-system.password.*"
                  ;"madek.auth.utils.ring-audits"
                  }:debug]
               [#{#?(:clj "com.zaxxer.hikari.*")
                  "madek.*"} :info]
               [#{"*"} :warn]]
   :log-level nil})

(defn init
  ([] (init LOGGING_CONFIG))
  ([logging-config]
   (info "initializing logging " logging-config)
   (timbre/merge-config! logging-config)
   #?(:clj (taoensso.timbre.tools.logging/use-timbre))
   (info "initialized logging " (pr-str timbre/*config*))))
