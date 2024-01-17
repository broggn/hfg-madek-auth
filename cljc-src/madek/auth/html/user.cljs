(ns madek.auth.html.user
  (:require
   [cljs.core.async :refer [go go-loop <!]]
   [cuerdas.core :as str]
   [madek.auth.html.icons :as icons]
   [madek.auth.http.client.core :as http-client]
   [madek.auth.localization :refer [translate]]
   [madek.auth.routes :refer [path navigate!]]
   [madek.auth.state :refer [user*]]
   [madek.auth.utils.core :refer [presence]]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre :refer [debug info warn error spy]]))

(def name-or-some-identifier*
  (reaction
   (or (-> (str/join " " [(:user_first_name @user*)
                          (:user_last_name @user*)])
           str/trim presence)
       (-> @user* :user_email presence)
       (-> @user* :user_login presence)
       (-> @user* :user_id))))

(defn sign-out [& args]
  (debug 'sign-out)
  (go (some->
       {:method :post
        :url (path :sign-out {})
        :json-params {}}
       http-client/request :chan <!
       http-client/filter-success
       (#(navigate! "/" :reload true)))))

(defn navbar-part-user []
  [:<>
   (when-let [user @user*]
     [:div.menu
      [:button.menu__toggle-button
       @name-or-some-identifier*
       [icons/down]]
      (when true
        [:ul.menu__flyout {:role :menu}
         [:li
          [:a {:href "/my"} (translate :user-menu-my-archive)]]

         [:li
          [:a {:href (path :info)} (translate :user-menu-info)]]

         [:li
          [:form
           {:on-submit #(.preventDefault %)}
           [:button.link-button.bold
            {:type :submit
             :on-click sign-out}
            (translate :user-menu-sign-out)]]]])])])
