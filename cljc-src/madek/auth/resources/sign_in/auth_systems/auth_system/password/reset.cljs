(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.reset
  (:require
   [cljs.core.async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [madek.auth.html.forms.core :as forms]
   [madek.auth.http.client.core :as http-client]
   [madek.auth.localization :refer [translate]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.shared :refer [satisfies-strength?]]
   [madek.auth.routes :refer [navigate! path]]
   [madek.auth.state :as state :refer [debug?* hidden-routing-state-component]]
   [madek.auth.utils.core :refer [presence]]
   [reagent.core :as reagent :refer [reaction] :rename {atom ratom}]
   [taoensso.timbre :refer [debug info warn]]))

(def data* (ratom {}))
(def response-data* (ratom {}))
(def validation-message* (ratom nil))
(def pw-strength-message* (ratom nil))

(def waiting?*
  (reaction
   (->> @http-client/requests*
        (map second)
        (sort-by :timestamp)
        last
        (#(and % (-> % :response not))))))

(defn handle-validate-token-response [response]
  (reset! response-data* response)
  (some->> response :body :email-or-login
           (swap! data* assoc :email-or-login)))

(defn validate-token [& _]
  (when-let [token (or (-> @state/routing* :query-params :token presence)
                       (-> @data* :token presence))]
    (swap! data* assoc :token token)
    (go (some-> {:url (path :sign-in-user-auth-system-password-reset
                            (:path-params @state/routing*)
                            (-> @data*
                                (select-keys [:lang])
                                (merge {:token token})))
                 :method :get
                 :modal-on-request false
                 :modal-on-response-error true}
                http-client/request :chan <!
                handle-validate-token-response))))

(defn handle [response]
  (reset! response-data* response)
  (when (< (:status response) 300)
    (navigate! (path :sign-in-user-auth-system-password-reset
                     (:path-params @state/routing*)
                     (-> @state/routing*
                         :query-params
                         (select-keys [:lang])
                         (merge {:success true
                                 :email-or-login (:email-or-login @data*)})))
               :reload true)))

(defn submit []
  (go (some->
       {:url (path :sign-in-user-auth-system-password-reset
                   (:path-params @state/routing*)
                   (-> @state/routing*
                       :query-params
                       (select-keys [:lang])))
        :json-params @data*
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

(defn form []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (reset! validation-message* nil)
                 (reset! pw-strength-message* nil)
                 (if (:email-or-login @data*)
                   (cond (-> @data* :password presence not)
                         (reset! validation-message* (translate :reset-password-message-password-required))
                         (-> @data* :password satisfies-strength? not)
                         (reset! pw-strength-message* (translate :reset-password-strength-validation-message))
                         :else (submit))
                   (validate-token)))}
   (when (some-> @response-data* :status (#(> % 300)))
     [:div.form-row
      [:div.validation-message
       (if-let [msg (some->> @response-data* :body :error_message)] msg "Error")]])
   [:div.form-row
    [forms/input-component data* [:token]
     :classes "form-row"
     :label (translate :reset-password-token-input-label)
     :disabled (some? (:email-or-login @data*))
     :auto-focus? true]]
   (when (:email-or-login @data*)
     [:<>
      [:div.form-row
       [forms/input-component data* [:email-or-login]
        :classes "form-row"
        :label (translate :reset-password-username-input-label)
        :disabled true
        :auto-focus? true]]
      [:div.form-row
       [forms/input-component data* [:password]
        :hint (translate :reset-password-strength-hint)
        :classes "form-row"
        :type :password
        :label (translate :reset-password-new-password-input-label)
        :auto-focus? true]]])
   (when @validation-message*
     [:div.form-row.validation-message @validation-message*])
   (when (and @pw-strength-message* (-> @data* :password satisfies-strength? not))
     [:div.form-row.validation-message @pw-strength-message*])

   [:div.form-row
    [:button.primary-button {:type :submit}
     (translate :step1-submit-label) (when @waiting?* "...")]]])

(defn page []
  [:div.card-page
   [hidden-routing-state-component :did-mount validate-token :force-routing-reset true]
   [:div.card-page__head [:h1 (translate :login-box-title)]]
   [:div.card-page__body {:style {:min-height "20em"}}
    (if (-> @state/routing* :query-params :success)
      [:<>
       [:h2.form-row (translate :reset-password-success-txt)]
       [:div.form-row
        [:a {:href (path :sign-in nil
                         (some-> @state/routing*
                                 :query-params
                                 (select-keys [:lang :email-or-login])))}
         (translate :reset-password-login-link-label)]]]
      [:<>
       (when-not (-> @state/routing* :query-params :token presence) [:h2.form-row (translate :reset-password-txt)])
       [form]])]
   [page-debug]])

(def components
  {:page page})
