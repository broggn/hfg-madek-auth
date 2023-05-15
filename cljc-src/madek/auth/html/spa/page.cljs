(ns madek.auth.html.spa.page
  (:require
    ["react-bootstrap" :as bs]
    [madek.auth.routes :refer [path]]
    [madek.auth.state :as state]
    [taoensso.timbre :refer [debug info warn error spy]]
    [madek.auth.html.user :as user]
    ))


(defn header []
  [:> bs/Navbar {:bg :light}
   [:> bs/Container {:class "justify-content-start"}
    [:> bs/Navbar.Brand {:href (path :auth)} "Madek Auth"]]
   [:> bs/Container {:class "justify-content-center"}
    ;[:> bs/Nav.Item [:> bs/Nav.Link {:href (path :sign-in)} [:div] " Sign-in"]]
    ]
   [:> bs/Container {:class "justify-content-end"}
    [user/navbar-part-user]]])


(defn footer []
  [:> bs/Navbar {:bg :light}
   [:> bs/Navbar.Collapse {:class_name "justify-content-end"}]
   [:> bs/Form {:inline "true" :class "px-2"}
    [:> bs/Form.Group {:control-id "debug"}
     [:> bs/Form.Check {:type "checkbox" :label "Debug"
                        :checked @state/debug?*
                        :on-change #(swap! state/debug?* (fn [b] (not b)))}]]]])
