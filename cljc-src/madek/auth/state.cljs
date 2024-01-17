(ns madek.auth.state
  (:require
   [cljs.pprint :refer [pprint]]
   [cuerdas.core :as str]
   [madek.auth.html.dom :as dom]
   [madek.auth.utils.core :refer [presence]]
   [reagent.core :as reagent :refer [reaction]]
   [taoensso.timbre :refer [debug info warn error spy]]))

(def routing* (reagent/atom {}))

(def debug?* (reagent/atom false))

(def server* (reagent/atom {}))

(def settings* (reagent/atom nil))

(def user* (reagent/atom nil))

(def state* (reagent/reaction
             {:debug @debug?*
              :routing @routing*
              :server @server*
              :settings @settings*
              :user @user*}))

(defn hidden-routing-state-component
  [& {:keys [did-mount did-change did-update will-unmount]
      :or {did-update #()
           did-change #()
           did-mount #()
           will-unmount #()}}]
  "Invisible react component; fires did-change, did-update, did-change, will-unmount handlers according
  to react handlers and changes in the routing state:
  * did-change on :component-did-mount, :component-did-update or when routing state changed
  * did-mount corresponds to reagent :component-did-mount,
  * did-update corresponds to reagent did-update
  * will-unmount corresponds to reagent :component-will-unmount,
  "
  (let [old-state* (reagent/atom nil)
        eval-did-change (fn [handler args]
                          (let [old-state @old-state*
                                new-state @routing*]
                            (debug 'old-state old-state 'new-state new-state)
                            (when (not= old-state new-state)
                              (reset! old-state* new-state)
                              (apply handler (concat
                                              [old-state new-state]
                                              args)))))]
    (reagent/create-class
     {:component-will-unmount (fn [& args] (apply will-unmount args))
      :component-did-mount (fn [& args]
                             (apply did-mount args)
                             (eval-did-change did-change args))
      :component-did-update (fn [& args]
                              (apply did-update args)
                              (eval-did-change did-change args))
      :reagent-render
      (fn [& {:keys []
              :or {}}]
        [:div.hidden-routing-state-component
         {:style {:display :none}}
         [:pre (with-out-str (pprint @routing*))]])})))

(defn debug-ui-component []
  [:div.debug.state-debug
   (when @debug?*
     [:<>
      [:hr]
      [:h4 "Debug global " [:code "@state*"]]
      [:pre.bg-light
       [:code
        (with-out-str (pprint @state*))]]])])

(defn init []
  (info "initializing state ...")
  (swap! server* merge (dom/data-attribute "body" "server-state"))
  (swap! user* merge (dom/data-attribute "body" "user"))
  (swap! settings* merge (dom/data-attribute "body" "settings"))
  (info "initialized state"))
