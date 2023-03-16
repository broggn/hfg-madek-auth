(ns madek.auth.resources.sign-in.auth-systems.auth-system.request
  (:require
    [madek.auth.utils.json :as json]
    [lambdaisland.uri :as uri]
    [cljs.core.async :refer [go go-loop]]
    [cljs.pprint :refer [pprint]]
    [madek.auth.html.forms.core :as forms]
    [madek.auth.html.icons :as icons]
    [madek.auth.http.client.core :as http-client]
    [madek.auth.routes :refer [navigate! path]]
    [madek.auth.state :as state :refer [debug?* hidden-routing-state-component]]
    [madek.auth.utils.core :refer [presence]]
    [reagent.core :as reagent :refer [reaction] :rename {atom ratom}]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn current-uri []
  (uri/uri (.. js/window -location)))

(def data* 
  (ratom  (current-uri)))

(defn req-data []
  {"base-url"
   (let [url (current-uri)]
     (str (:scheme url)
          "://"
          (:host url)
          (when-let [p (:port url)]
            (str  ":" p))))})

(defn continue [resp-data]
  (info 'continue resp-data)
  (navigate! (str  (:external_sing_in_url resp-data)
                  "?token=" (:token resp-data))  
             :reload true))

(defn request []
  (go (some-> 
        {:method :post
         :json-params (req-data)}
        http-client/request :chan <!
        http-client/filter-success! :body
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
   [:h1.text-center "Sign-in: processing request"]
   [page-debug]])

(def components 
  {:page page})
