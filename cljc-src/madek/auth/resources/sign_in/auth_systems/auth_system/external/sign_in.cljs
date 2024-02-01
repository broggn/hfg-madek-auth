(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.sign-in
  (:require [cljs.core.async :refer [go]]
            [cljs.pprint :refer [pprint]]
            [madek.auth.html.icons :as icons]
            [madek.auth.http.client.core :as http-client]
            [madek.auth.localization :refer [translate]]
            [madek.auth.routes :refer [navigate! path]]
            [madek.auth.state :as state :refer [debug?*
                                                hidden-routing-state-component]]
            [reagent.core :as reagent :rename {atom ratom}]
            [taoensso.timbre :refer [warn]]))

(defonce data* (ratom nil))

(defn handle [response]
  (warn response)
  (reset! data* response)
  (when (< (:status response) 300)
    (let [target-path (or (some-> response :body :request-claims :return-to)
                          "/my")]
      (navigate! target-path :reload true))))

(defn sign-in []
  (go (some->
       {:modal-on-response-error false
        :modal-on-request false
        :json-params (-> @state/routing* :query-params)
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
    [:p [:strong "Authentication failed"]]
    [:<> (if-let [msg (some->> @data* :body :error_message)]
           [:p msg]
           [:p "The authentication system did not return any indication why the authorization failed. "])]
    [:hr]
    [:p "You can try again or contact your support for help."]]
   [:div.d-flex.mb-3
    [:a.btn.btn-warning
     {:href (path :sign-in {}
                  (merge {}
                         (some-> @data* :body :request-claims
                                 (select-keys [:return-to :lang]))))}
     [:span [icons/back] " Start over"]]]])

(defn success-component []
  [:d
   [:div.alert.alert-success
    [:p (translate :ext-callback-success)]]])

(defn page []
  [:div.page
   [hidden-routing-state-component
    :did-change sign-in]
   [:<> (cond
          (some-> @data*
                  :status (#(> % 300))) [fail-component]
          (some-> @data*
                  :status (#(< % 300))) [success-component]
          :else [:div.placeholder (translate :ext-callback-processing)])]

   [page-debug]])

(def components
  {:page page})
