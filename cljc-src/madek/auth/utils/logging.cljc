(ns madek.auth.utils.logging
  (:require
    #?(:clj [taoensso.timbre.tools.logging])
    [taoensso.timbre.appenders.core :as appenders]
    [taoensso.timbre :as timbre :refer [debug info]]))


(def LOGGING_CONFIG
  {:min-level [[#{
                  "madek.auth.resources.sign-in.auth-systems.auth-system.password.*"
                  ;"madek.auth.http.*" 
                  ;"madek.auth.main" 
                  ;"madek.auth.resources.sign-in.*"
                  ;"madek.auth.routing.*"
                  } :debug]
               [#{
                  #?(:clj "com.zaxxer.hikari.*")
                  "madek.*"} :info]
               [#{"*"} :warn]]
   :appenders #?(:clj {:spit (appenders/spit-appender {:fname "log/debug.log"})}
                 :cljs {})
   :log-level nil})


(defn init
  ([] (init LOGGING_CONFIG))
  ([logging-config]
   (info "initializing logging " logging-config)
   (timbre/merge-config! logging-config)
   #?(:clj (taoensso.timbre.tools.logging/use-timbre))
   (info "initialized logging " (pr-str timbre/*config*))))
