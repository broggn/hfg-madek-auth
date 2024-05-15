(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.request
  (:require
   [cljs.core.async :refer [<! go go-loop]]
   [cljs.pprint :refer [pprint]]
   [lambdaisland.uri :as uri]
   [madek.auth.html.components :refer [change-username-button]]
   [madek.auth.html.forms.core :as forms]
   [madek.auth.http.client.core :as http-client]
   [madek.auth.localization :refer [translate]]
   [madek.auth.routes :refer [navigate! path]]
   [madek.auth.state :as state :refer [debug?* hidden-routing-state-component]]
   [madek.auth.utils.core :refer [presence]]
   [madek.auth.utils.json :as json]
   [reagent.core :as reagent :refer [reaction] :rename {atom ratom}]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def auth-system* (ratom nil))
(def data* (ratom {}))
(def validation-message* (ratom nil))

(defn continue [resp-data]
  (if-let [return-to (not-empty (get-in @state/routing* [:query-params :return-to]))]
    (navigate! return-to :reload true)
    (navigate! "/my" :reload true)))

(defn request-auth-system [& _]
  (reset! auth-system* nil)
  (go (some->>
       {} http-client/request :chan <!
       http-client/filter-success :body
       (reset! auth-system*))))

(defn password-mismatch? [req]
  (-> req :response :body :message (= "Password missmatch")))

(def password-mismatch?*
  (reaction
   (->> @http-client/requests*
        (map second)
        (sort-by :timestamp)
        (filter password-mismatch?)
        first)))

(def waiting?*
  (reaction
   (->> @http-client/requests*
        (map second)
        (sort-by :timestamp)
        last
        (#(and % (-> % :response not))))))

(defn submit-sign-in []
  (go (some->
       {:url (path :sign-in-user-auth-system-sign-in
                   (:path-params @state/routing*)
                   (:query-params @state/routing*))
        :method :post
        :modal-on-request false
        :modal-filter #(-> % password-mismatch? not)
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
  (let [email-or-login (get-in @state/routing* [:query-params :email-or-login])
        validation-message @validation-message*
        password-mismatch? @password-mismatch?*]
    [:form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (reset! validation-message* nil)
                   (if (-> @data* :password presence)
                     (submit-sign-in)
                     (reset! validation-message* (translate :step3-message-password-required))))}
     (if validation-message
       [:div.form-row.validation-message validation-message]
       ; else
       (when password-mismatch?
         [:div.form-row.validation-message (translate :step3-message-login-failed)]))

     [:div.form-row
      [:div.bold.mb-2 (translate :step2-username-label)]
      [:div email-or-login]]

     [:div
      [forms/input-component data* [:password]
       :type :password :classes "form-row" :label (translate :step3-password-label) :auto-focus? true]]

     [:div.form-row
      [:button.primary-button
       {:type :submit}
       (translate :step3-submit-label)
       (when @waiting?* "...")]]
     [:div
      [change-username-button
       {:href (path :sign-in {} (some-> @state/state* :routing
                                        :query-params (select-keys [:return-to :lang])))}
       (translate :step3-change-username-label)]]]))

(defn page []
  [:div.card-page
   [hidden-routing-state-component
    :did-change request-auth-system]
   [:div.card-page__head [:h1 (translate :login-box-title)]]
   [:div.card-page__body {:style {:min-height "20em"}}
    [:<>
     ;[:h2.form-row (-> @auth-system* :auth_system_name (or "\u00A0"))]
     [form]]]
   [page-debug]])

(def components
  {:page page})
