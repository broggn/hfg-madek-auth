(ns madek.auth.html.spa.main
  (:require
   [clojure.java.io :as io]
   [hiccup.page :refer [html5 include-js include-css]]
   [madek.auth.utils.cli :refer [long-opt-for-key]]
   [madek.auth.utils.json :as json]
   [madek.auth.utils.url :as url]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   (include-css "/auth/public/css/main.css")])

(def js-manifest
  (some-> "auth/public/js/manifest.edn"
          io/resource
          slurp
          read-string))

(def js-includes
  (->> js-manifest seq
       (map :output-name)
       (map #(str "/auth/public/js/" %))
       (map hiccup.page/include-js)))

(defn html-handler [{user :authenticated-entity
                     settings :settings
                     :as request}]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html5
          (head)
          [:body {:data-user (-> user json/to-json url/encode)
                  :data-settings (-> settings json/to-json url/encode)}
           [:div#app {:class "full-height-container"}
            [:div.loading-screen
             [:h1 "Madek Authentication Service"]
             [:p "Loading application ..."]]]]
          js-includes)})

(defn dispatch [root-handler request]
  (if (and (-> request :route :data :bypass-spa not)
           (= :html (-> request :accept :mime))
           (not= :post (-> request :request-method)))
    (html-handler request)
    (root-handler request)))

(defn wrap [handler]
  (fn [request]
    (dispatch handler request)))
