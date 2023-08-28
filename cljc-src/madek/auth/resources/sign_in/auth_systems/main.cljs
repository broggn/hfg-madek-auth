(ns madek.auth.resources.sign-in.auth-systems.main
  (:require
   [cljs.core.async :refer [go go-loop <!]]
   [cljs.pprint :refer [pprint]]
   [clojure.set :refer [rename-keys]]
   [clojure.walk :refer [stringify-keys]]
   [clojure.string :refer [trim]]
   [madek.auth.html.forms.core :as forms]
   [madek.auth.html.components :refer [change-username-button]]
   [madek.auth.http.client.core :as http-client]
   [madek.auth.routes :refer [navigate! path]]
   [madek.auth.state :as state :refer [debug?* hidden-routing-state-component]]
   [madek.auth.utils.core :refer [presence]]
   [madek.auth.utils.query-params :as query-params]
   [madek.auth.localization :refer [translate]]
   [reagent.core :as reagent :refer [reaction] :rename {atom ratom}]
   [taoensso.timbre :refer [debug error info spy warn]]))


(defonce data* (ratom {}))

(defonce email-or-login* (reaction (some-> @state/state* :routing :query-params :email-or-login trim)))

(defn auth-system-url [sys]
  (if (= "legacy" (:auth_system_type sys))
    (str (:auth_system_url sys)
         "?"
         (-> (merge {:return_to "/my"}
                    (rename-keys (:query-params @state/routing*)
                                 {:return-to :return_to})
                    {:email-or-login @email-or-login*})
             stringify-keys query-params/encode))
    (path :sign-in-user-auth-system-request
          (select-keys sys [:auth_system_id :auth_system_type])
          (merge (:query-params @state/routing*) {:email-or-login @email-or-login*}))))

(defn continue [sys]
  (let [url (auth-system-url sys)]
    (if (= "legacy" (:auth_system_type sys))
      (set! js/window.location url)
      (navigate! url))))

(defn request-auth-systems [& _]
  (reset! data* nil)
  (go (some->>
       {} http-client/request :chan <!
       http-client/filter-success :body
       ((fn [data]
          (if (-> data count (= 1))
            (-> data first continue)
            (reset! data* data)))))))

(defn page-debug []
  [:<> (when @debug?*
         [:div.debug
          [:hr]
          [:h4 "Page debug"]
          [:pre.bg-light
           [:code
            (with-out-str (pprint @data*))]]])])

(defn auth-system [sys]
  [:div.form-row
   [:a.primary-button
    {:href (auth-system-url sys)}
    (:auth_system_name sys)]])


(defn auth-systems []
  [:div
   (for [sys @data*]
     ^{:key (:auth_system_id sys)}
     [auth-system sys])])

(defn page []
  (let [email-or-login @email-or-login*
        back-url (path :sign-in {} (some-> @state/state* :routing
                                           :query-params (select-keys [:return-to :lang])))]
    [:div.card-page
     [hidden-routing-state-component
      :did-change request-auth-systems]
     [:div.card-page__head [:h1 (translate :login-box-title)]]
     [:div.card-page__body {:style {:min-height "20em"}}
      (cond
        (nil? @data*)
        [:h2.mb-3 "Loading..."]

        (= "" email-or-login)
        [:<>
         [:div.mb-5.validation-message (translate :step2-message-username-missing)]
         [:div
          [change-username-button {:href back-url} (translate :step2-back-label)]]]

        (empty? @data*)
        [:<>
         [:div.form-row
          [:div.bold.mb-2 (translate :step2-username-label)]
          [:div email-or-login]]
         [:div.form-row
          [:div.validation-message (translate :step2-message-login-not-possible)]]
         [change-username-button {:href back-url} (translate :step2-change-username-label)]]

        :else
        [:<>
         [:div.form-row
          [:div.bold.mb-2 (translate :step2-username-label)]
          [:div email-or-login]]
         [:div.mb-5
          [auth-systems]]
         [:div
          [change-username-button {:href back-url} (translate :step2-change-username-label)]]])]

     [page-debug]]))

(def components
  {:page page})
