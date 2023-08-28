(ns madek.auth.html.components
  (:require
   [madek.auth.html.icons :as icons]))

(defn change-username-button [{:keys [href]} children]
  [:a {:href href}
   [:span {:style {:margin-left "4px" :margin-right "8px" :font-size "12px"}} [icons/back]]
   children])
