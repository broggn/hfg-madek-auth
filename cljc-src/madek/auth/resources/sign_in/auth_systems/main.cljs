(ns madek.auth.resources.sign-in.auth-systems.main
  (:require
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


(defonce data* (ratom nil))

(defonce email* (reaction (some-> @state/state* :routing :path-params :email)))

(defn request-auth-systems [& _]
  (reset! data* nil)
  (go (some->>
        {} http-client/request :chan <!
        http-client/filter-success! :body
        (reset! data*))))

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn auth-system [sys]
  [:div.my-2.d-flex.align-items-center.justify-content-center  
   [:div 
    [:a.btn.btn-primary
     {:href (spy (path :sign-in-user-auth-system-request  
                       {:auth_system_id (:auth_system_id sys)
                        :email @email*}
                       (merge {} (:query-params @state/routing*))))}
     (:auth_system_name sys)]]])

(defn auth-systems []
  [:div.my-5
   (for [sys @data*]
     ^{:key (:auth_system_id sys)} 
     [auth-system sys])])

(defn page []
  [:div
   [hidden-routing-state-component
    :did-change request-auth-systems]
   [:h1.text-center "Sign-in: choose an authentication method"
    [:<> (when-let [email @email*]
           [:span " for " [:code email]])]]
   (cond 
     (nil? @data*)  [:div [:h2 "Wait"]]
     (empty? @data*) [:div.alert.alert-danger 
                      [:p [:strong  "Sign-in or sign-up is not available for this e-mail address."]]
                      [:spann "There are serveral possible causes:"
                       [:ul
                        [:li "The e-mail address is wrongly typed."]
                        [:li "This email is not registered with any account or external sign-up authentication system."]
                        [:li "There is an registered account but it is deactivated."]]]
                      [:spann "You can contact your support for help."]]
     :else [:div [auth-systems]])
   [:div.d-flex.mb-3
    [:a.btn.btn-warning {:href (path :sign-in {} (some-> @state/state* :routing 
                                                         :query-params (select-keys [:return-to])) )}
     [:span [icons/back] " Use a different e-mail address"]] ]

   [page-debug]])

(def components 
  {:page page})
