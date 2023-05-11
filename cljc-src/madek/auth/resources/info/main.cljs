(ns madek.auth.resources.info.main
  (:require 
    ["js-yaml" :as yaml]
    [cljs.pprint :refer [pprint]]
    [madek.auth.state :as state]
    [madek.auth.utils.core]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn user-session-info []
  [:<> 
   (when @state/user* 
     [:div.info
      [:h2 "Authentication and session information about "
       [:em @state/user-name-or-some-identifier*]]
      [:pre.bg-light
       [:code.user-session-data
        (str (yaml/dump
               (->> @state/user*
                    (into (sorted-map))
                    clj->js)
               {}))]]])])

(defn page []
  [:div.page 
   [:h1 "Madek Auth: Info"]
   [user-session-info]])

(def components
  {:page page})
