(ns madek.auth.html.spa.page
  (:require
   ["react-bootstrap" :as bs]
   [madek.auth.html.user :as user]
   [madek.auth.localization :refer [localized-setting]]
   [madek.auth.routes :refer [path]]
   [madek.auth.state :as state]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defn header []
  (let [brand-logo-url (some-> @state/settings* :brand_logo_url
                               (#(str "url(" % ")")))]
    [:header.header.container-inverted
     [:a.header-brand {:href "/"}
      [:span.header-brand__logo {:style (merge {} (when brand-logo-url {:background-image brand-logo-url}))}]
      [:div.header-brand__text.header-brand-text
       [:h1.header-brand-text__instance-name (localized-setting :site_titles)]
       [:h2.header-brand-text__brand-name (localized-setting :brand_texts)]]]
     [:div [user/navbar-part-user]]]))

(defn footer []
  [:footer.footer.container-inverted
   [:div {:class "ui-footer-copy"}
    [:a {:href "/release"} "Madek"]
    " — "
    (localized-setting :brand_texts)
    [:label {:style {:display "none"}}
     [:input {:type "checkbox" :label "Debug"
              :checked @state/debug?*
              :on-change #(swap! state/debug?* (fn [b] (not b)))}]
     "Debug"]]])
