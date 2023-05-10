(ns madek.auth.routing.main
  (:require
    [clj-yaml.core :as yaml]
    [logbug.debug :as debug :refer [I> debug-ns]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [madek.auth.db.core :as db]
    [madek.auth.html.spa.main :as spa]
    [madek.auth.http.anti-csrf.main :as anti-csrf]
    [madek.auth.http.session :as session]
    [madek.auth.http.static-resources :as static-resources]
    [madek.auth.routes :as routes]
    [madek.auth.routing.resolve :refer [resolve-table]]
    [ring.middleware.accept]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.cookies]
    [ring.middleware.json :refer [wrap-json-response wrap-json-body json-response]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn not-found-handler [request]
  {:status 404
   :body "Not Found"})



;;; routes ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn route-resolve [handler request]
  (debug 'route-resolve (:uri request))
  (if-let [route (some-> request :uri
                         routes/route)]
    (let [{{route-name :name} :data} route
          params-coerced (routes/coerce-params route)
          ; replace plain :path-params with coerced ones
          route (update route :path-params
                        #(merge {} % (:path params-coerced)))]
      (debug 'route route)
      (debug 'route-coreced-params params-coerced)
      (debug "route match" route-name)
      (handler (-> request
                   (assoc
                     :route route
                     :route-name route-name
                     :route-handler (resolve-table route-name))
                   (update-in [:params] #(merge {} % (:path-params route))))))
    (handler request)))

(defn wrap-route-resolve [handler]
  (fn [request]
    (route-resolve handler request)))

(defn route-dispatch [handler request]
  (if-let [route-handler (:route-handler request)]
    (route-handler request)
    (handler request)))

(defn wrap-route-dispatch [handler]
  (fn [request]
    (route-dispatch handler request)) )


;;; misc wrapper ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn wrap-accept [handler]
  (ring.middleware.accept/wrap-accept
    handler
    {:mime
     ["application/json" :qs 1 :as :json
      "image/apng" :qs 0.8 :as :apng
      "text/css" :qs 1 :as :css
      "text/html" :qs 1 :as :html]}))

(defn wrap-add-vary-header [handler]
  "should be used if content varies based on `Accept` header, e.g. if using `ring.middleware.accept`"
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Vary"] "Accept"))))

(defn wrap-parsed-query-params [handler]
  (fn [request]
    (handler
      (assoc request :query-params-parsed
             (->> request :query-params
                  (map (fn [[k v]] [(keyword  k) (yaml/parse-string v)]))
                  (into {}))))))

(defn wrap-exception [handler] 
  (fn [request]
    (try 
      (handler request)
      (catch Exception ex
        (if-let [response (and (some-> ex ex-data :status int?)
                               (ex-data ex))]
          (do (info (ex-message ex))
              (json-response response {}))
          (do (error ex)
              {:status 500
               :headers {"Content-Type" "text/plain"}
               :body "Unclassified server error, see the server logs for details."}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn build-routes [options]
  (I> wrap-handler-with-logging
      not-found-handler
      wrap-route-dispatch
      wrap-route-resolve
      wrap-json-response
      spa/wrap
      session/wrap
      (wrap-json-body {:keywords? true})
      db/wrap-tx
      wrap-keyword-params
      wrap-params
      anti-csrf/wrap
      ring.middleware.cookies/wrap-cookies
      (static-resources/wrap
        "" {:allow-symlinks? true
            :cache-bust-paths []
            :never-expire-paths
            [#".*[^\/]*\d+\.\d+\.\d+.+"  ; match semver in the filename
             #".+\.[0-9a-fA-F]{32,}\..+"] ; match MD5, SHAx, ... in the filename
            :cache-enabled? (not (:dev-mode options))})
      wrap-accept
      wrap-add-vary-header
      wrap-exception
      wrap-content-type))

(defn init [options]
  (info "initializing routing ...")
  (let [routes (build-routes options)]
    (info "initialized routing")
    routes ))


;#### debug ###################################################################
;(debug-ns *ns*)
