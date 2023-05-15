(ns madek.auth.html.user
  (:require
    ["react-bootstrap" :as bs]
    [cljs.core.async :refer [go go-loop]]
    [cuerdas.core :as str]
    [madek.auth.html.icons :as icons]
    [madek.auth.http.client.core :as http-client]
    [madek.auth.routes :refer [path navigate!]]
    [madek.auth.utils.core :refer [presence]]
    [reagent.core :as reagent :refer [reaction]]
    [taoensso.timbre :refer [debug info warn error spy]]
    [madek.auth.state :refer [user*]]))


(def name-or-some-identifier* 
  (reaction 
    (or (-> (str/join " " [(:person_first_name @user*) 
                           (:person_last_name @user*)]) 
            str/trim presence)
        (-> @user* :person_pseudonym presence)
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
     [:> bs/NavDropdown {:title @name-or-some-identifier*}
      [:> bs/NavDropdown.Item {:href "/my"}
       "My archive"]
      [:> bs/NavDropdown.Item {:href (path :info)}
       "Session information"] 
      [:> bs/NavDropdown.Item
       [:form.form.d-flex
        {:on-submit #(.preventDefault %)}
        [:button.btn.btn-warning.btn-sm.flex-fill
         {:type :submit
          :on-click sign-out}
         [:span [icons/sign-out] " " "Sign out"]]]]])])
