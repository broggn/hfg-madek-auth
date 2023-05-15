(ns madek.auth.resources.sign-in.main
  (:require
    [cljs.pprint :refer [pprint]]
    [cljs.core.async :refer [go go-loop]]
    [madek.auth.html.forms.core :as forms]
    [madek.auth.http.client.core :as http-client]
    [madek.auth.routes :refer [navigate! path]]
    [madek.auth.state :as state :refer [debug?*]]
    [madek.auth.utils.core :refer [presence]]
    [reagent.core :as reagent :rename {atom ratom}]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defonce data* (ratom {}))

(defn continue []
  (navigate! (path :sign-in-user-auth-systems  
                   {:email (get-in @data* [:email])}
                   (some-> @state/state* :routing 
                           :query-params (select-keys [:return-to])))))

(defn email-form []
  [:div.row
   [:div.col-md-3]
   [:div.col-md
    [:form.form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (continue))}
     [:div.mb-3
      [:label.col-form-label {:for :email}  
       "Provide your " [:b "email address "] " to sign in" ]
      [:input.form-control 
       {:id :email
        :type :email
        :value (get-in @data* [:email])
        :on-change #(-> % .-target .-value presence (forms/set-value data* [:email]))}]]
     [:div.d-flex.mb-3
      [:div.ms-auto
       [:button.btn.btn-primary {:type :submit} "Continue"]]]]]
   [:div.col-md-3]])

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn page []
  [:div
   [:h1.text-center "Sign-in: provide e-mail address"]
 [email-form]
 [page-debug]])


(def components 
  {:page page})
