(ns madek.auth.resources.sign-in.main
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [go go-loop]]
    [madek.auth.html.forms.core :as forms]
    [madek.auth.http.client.core :as http-client]
    [madek.auth.routes :refer [navigate! path]]
    [madek.auth.state :as state :refer [debug?* routing*]]
    [madek.auth.utils.core :refer [presence]]
    [madek.auth.localization :refer [translate]]
    [reagent.core :as reagent :rename {atom ratom}]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defonce data* (ratom {}))

(defn continue []
  (navigate! (path :sign-in-user-auth-systems  {}
                   (merge {:email-or-login (get-in @data* [:email-or-login])}
                          (some-> @state/state* :routing 
                                  :query-params (select-keys [:return-to :lang]))))))

(defn email-or-login-form []
  [:form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (continue))}
   [:div.form-row
    [:label.form-label {:for :email-or-login} (translate :step1-username-label)]
    [:input.text-input
     {:id :email-or-login
      :type :text
      :value (get-in @data* [:email-or-login])
      :on-change #(-> % .-target .-value presence (forms/set-value data* [:email-or-login]))
      :auto-focus true}]]
   [:div.form-row
    [:button.primary-button {:type :submit} (translate :step1-submit-label)]]])

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn page []
  [:div.card-page
   [:div.card-page__head [:h1 (translate :login-box-title)]]
   [:div.card-page__body
    [email-or-login-form]]
   [page-debug]])


(def components 
  {:page page})
