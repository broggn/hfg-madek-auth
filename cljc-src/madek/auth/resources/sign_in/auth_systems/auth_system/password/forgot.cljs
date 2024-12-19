(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.forgot
  (:require
   [cljs.core.async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [madek.auth.html.forms.core :as forms]
   [madek.auth.http.client.core :as http-client]
   [madek.auth.localization :refer [translate]]
   [madek.auth.routes :refer [navigate! path]]
   [madek.auth.state :as state :refer [debug?* hidden-routing-state-component]]
   [reagent.core :as reagent :refer [reaction] :rename {atom ratom}]
   [taoensso.timbre :refer [warn]]))

(def data* (ratom {}))
(def response-data* (ratom {}))

(def waiting?*
  (reaction
   (->> @http-client/requests*
        (map second)
        (sort-by :timestamp)
        last
        (#(and % (-> % :response not))))))

(defn handle [response]
  (warn response)
  (reset! response-data* response)
  (when (< (:status response) 300)
    (navigate! (path :sign-in-user-auth-system-password-forgot
                     (:path-params @state/routing*)
                     (-> @state/routing*
                         :query-params
                         (select-keys [:lang])
                         (assoc :success true)))
               :reload true)))

(defn submit []
  (go (some->
       {:url (path :sign-in-user-auth-system-password-forgot
                   (:path-params @state/routing*)
                   (-> @data*
                       (select-keys [:email-or-login])
                       (assoc :lang (-> @state/routing* :query-params :lang))))
        :method :post
        :modal-on-request false
        :modal-on-response-error true}
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

(defn form [email-or-login]
  [:form
   {:on-submit (fn [e] (.preventDefault e) (submit))}
   (when (some-> @response-data* :status (#(> % 300)))
     [:div.form-row
      [:div.validation-message
       (if-let [msg (some->> @response-data* :body :error_message)] msg "Error")]])
   [:div.form-row
    [forms/input-component data* [:email-or-login]
     :classes "form-row" :label (translate :step2-username-label)
     :auto-focus? true
     :value email-or-login]]
   [:div.form-row
    [:button.primary-button
     {:type :submit}
     (translate :forgot-password-send-label)
     (when @waiting?* "...")]]])

(defn page []
  [:div.card-page
   [hidden-routing-state-component
    :did-mount #(swap! data* assoc :email-or-login
                       (get-in @state/routing* [:query-params :email-or-login]))]
   [:div.card-page__head [:h1 (translate :login-box-title)]]
   [:div.card-page__body {:style {:min-height "20em"}}
    (let [email-or-login (get-in @state/routing* [:query-params :email-or-login])]
      (if (boolean (get-in @state/routing* [:query-params :success]))
        [:<>
         [:h2.form-row (translate :forgot-password-success-txt)]
         [:div.form-row
          [:div.mb-2 (translate :forgot-password-manual-entry-info)]
          [:a {:href (path :sign-in-user-auth-system-password-reset
                           (:path-params @state/routing*)
                           (select-keys (:query-params @state/routing*) [:lang]))}
           (translate :forgot-password-reset-link-label)]]]
        [:<>
         [:h2.form-row (translate :forgot-password-txt)]
         [form email-or-login]]))]
   [page-debug]])

(def components
  {:page page})

