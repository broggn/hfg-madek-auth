(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.request
  (:require [cljs.core.async :refer [go <!]]
            [cljs.pprint :refer [pprint]]
            [lambdaisland.uri :as uri]
            [madek.auth.http.client.core :as http-client]
            [madek.auth.localization :refer [translate]]
            [madek.auth.routes :refer [navigate!]]
            [madek.auth.state :as state :refer [debug?*
                                                hidden-routing-state-component]]
            [reagent.core :as reagent :rename {atom ratom}]
            [taoensso.timbre :refer [debug]]))

(defn current-uri []
  (uri/uri (.. js/window -location)))

(def data*
  (ratom (current-uri)))

(defn req-data []
  {"base-url"
   (let [url (current-uri)]
     (str (:scheme url)
          "://"
          (:host url)
          (when-let [p (:port url)]
            (str ":" p))))})

(defn continue [resp-data]
  (let [prefix (:external_sign_in_url resp-data)
        token (:token resp-data)
        url (str prefix "?token=" token)]
    (debug 'url url)
    (navigate! url :reload true)))

(defn request []
  (go (some->
       {:method :post
        :json-params (req-data)
        :modal-on-request false}
       http-client/request :chan <!
       http-client/filter-success :body
       continue)))

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn page []
  [:div.page
   [hidden-routing-state-component
    :did-change request]
   [:h1.text-center (translate :ext-redirecting)]
   [page-debug]])

(def components
  {:page page})
