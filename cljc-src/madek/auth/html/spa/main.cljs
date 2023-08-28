(ns madek.auth.html.spa.main
  (:require 
    [madek.auth.http.client.modals :as http-client-modal]
    [reagent.dom :as rdom]
    [madek.auth.html.spa.page :refer [header footer]]
    [madek.auth.state :as state]
    [taoensso.timbre :refer [debug error info spy warn]]
    ))

(defn not-found-page []
  [:div.page
   [:h1.text-danger "Page Not-Found"]
   ])

(defn html []
  [:<>
   [:div
    [header]
    [http-client-modal/modal-component]
    [:div.main
     (if-let [page (:page @state/routing*)]
       [:<>
        [page]]
       [not-found-page])]
    [state/debug-ui-component]]
   [footer]])

(defn mount []
  (info "mounting application")
  (when-let [app (.getElementById js/document "app")]
    (rdom/render [html] app))
  (info "mounted application"))
