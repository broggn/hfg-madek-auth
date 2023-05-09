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


(defonce data* (ratom  nil))

(defn handle [response]
  (warn response)
  (reset! data* response)
  (when (< (:status response) 300)
    (let [target-path (or (some-> response :body :request-claims :return-to)
                          "/my")]
      (info "navigating to" target-path)
      (navigate! target-path :reload true))))

(defn sign-in []
  (go (some-> 
        {:json-params (-> @state/routing* :query-params)
         :method :post
         :url (path (:name @state/routing*)
                    (:path-params @state/routing*))} 
        http-client/request :chan <!
        handle)))

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn fail-component []
  [:div
   [:div.alert.alert-danger 
    [:p [:strong  "Authentication failed"]]
    [:<> (if-let [msg (some->> @data* :body :error_message)]
           [:p msg]
           [:p "The authentication system did not return any indication why the authrizaion failed. "])]
    [:hr]
    [:p "You can try again or contact your support for help."]]
   [:div.d-flex.mb-3
    [:a.btn.btn-warning 
     {:href (path :sign-in {} 
                  (merge {}
                         (some-> @data* :body :request-claims 
                                 (select-keys [:return-to]))))}
     [:span [icons/back] " Start over"]]]])


(defn success-component []
  [:d
   [:div.alert.alert-success
    [:p [:strong  "Authentication succeeded"]]]])

(defn page []
  [:div.page
   [hidden-routing-state-component 
    :did-change sign-in]
   [:h1.text-center "Sign-in: validating authorization"]
   [:<> (cond 
          (some-> @data* 
                  :status (#(> % 300))) [fail-component]
          (some-> @data* 
                  :status (#(< % 300))) [success-component]
          :else [:div.placeholder])]
   
   [page-debug]])

(def components 
  {:page page})
