(ns madek.auth.resources.sign-in.auth-systems.auth-system.sign-in
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


(def data* (ratom  nil))

(defn signed-in [data]
  (info signed-in)
  (reset! data* data))

(defn sign-in []
  (go (some-> 
        {:json-params (-> @state/routing* :query-params)
         :method :post
         :url (path (:name @state/routing*)
                    (:path-params @state/routing*))} 
        http-client/request :chan <!
        http-client/filter-success! :body
        signed-in)))

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
    :did-change sign-in]
   [:h1.text-center "Sign-in: setting up session"]
   [:pre.pg-light
    [:code
     (with-out-str (pprint @data*))]]
   [page-debug]])

(def components 
  {:page page})
