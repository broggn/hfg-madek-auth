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
    ["/sign-in" {}
     ["" {:name :sign-in}]
     ["/:email/auth-systems/" 
      ["" {:name :sign-in-user-auth-systems}]
      [":auth_system_id" {}
       ["/request" :sign-in-user-auth-system-request]
       ["/sign-in" {:name :sign-in-user-auth-system-sign-in}]]]]]])


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


(comment
  (path :sign-in {} {:return-to "/auth/info"})
  (->> [:sign-in {} {:email "foo@bar.com"} ]
       spy
       (apply path)
       spy
       (reitit/match-by-path router)
       spy)


  (reitit/match-by-name router :users {:user-id "123"}))
