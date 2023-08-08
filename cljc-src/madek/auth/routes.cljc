(ns madek.auth.routes
  (:require
    #?(:cljs [madek.auth.html.history-navigation :as client-navigation :refer []])
    [clojure.walk :refer [stringify-keys]]
    [cuerdas.core :as string :refer []]
    [madek.auth.utils.query-params :as query-params]
    [reitit.coercion]
    [reitit.core :as reitit]
    [taoensso.timbre :refer [debug info warn error spy]]
    ))


(def coerce-params reitit.coercion/coerce!)

(def routes 
  [["/auth" 
    ["" {:name :auth}]
    ["/info" {:name :info}]
    ["/sign-in" {}
     ["" {:name :sign-in}]
     ["/auth-systems/" 
      ["" {:name :sign-in-user-auth-systems}]
      [":auth_system_type" {}
       ["/:auth_system_id" {}
        ["/request" :sign-in-user-auth-system-request]
        ["/sign-in" :sign-in-user-auth-system-sign-in]]]]]
    ["/sign-out" {:name :sign-out}]]])


(def router (reitit/router routes))

(def routes-flattened (reitit/routes router))

(defn route [path]
  (-> path
      (string/split #"\?" )
      first
      (->> (reitit/match-by-path router))))

(defn path
  ([kw]
   (path kw {}))
  ([kw route-params]
   (path kw route-params {}))
  ([kw route-params query-params]
   (when-let [p (reitit/match->path
                  (reitit/match-by-name
                    router kw route-params))]
     (if (seq query-params)
       (str p "?" (-> query-params stringify-keys query-params/encode))
       p))))

#?(:cljs
   (defn navigate!
     [url &{:keys [reload]
            :or {reload false}}]
     (if reload 
       (set! js/window.location url)
       (client-navigation/navigate! url))))

