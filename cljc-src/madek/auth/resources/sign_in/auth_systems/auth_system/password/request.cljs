(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.request
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


(def data* (ratom {}))

(defn continue [resp-data]
  (if-let [return-to (get-in  @state/routing* [:query-params :return-to])]
    (navigate! return-to :reload true)
    (navigate! "/my" :reload true)))

(defn submit-sign-in []
  (go (some-> 
        {:url (path :sign-in-user-auth-system-sign-in
                    (:path-params @state/routing*)
                    (:query-params @state/routing*))
         :method :post
         :modal-on-request true
         :json-params @data*}
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

(defn form []
  [:div.my-4.d-flex.align-items-center.justify-content-center
   [:form
    {:on-submit (fn [e]
                  (.preventDefault e)
                  (submit-sign-in))}
    [:div.mb-3
     (let [email-data* (reaction {:email (get-in @state/routing* 
                                                 [:path-params :email])})]
       [forms/input-component email-data*[:email]
        :disabled true])]
    [:div
     [forms/input-component data* [:password]
      :type :password]]
    [:div.d-grid
     [:button.btn.btn-primary 
      {:type :submit
       :disabled (-> @data* :password presence not)}
      "Submit" ]]]])

(defn page []
  [:div.page
   ;[hidden-routing-state-component :did-change request]
   [:h1.text-center "Madek Password Authentication"]
   [form]
   [page-debug]])

(def components 
  {:page page})
