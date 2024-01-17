(ns madek.auth.http.client.shared
  (:refer-clojure :exclude [str keyword send-off])
  (:require
   [clojure.string :as string]
   [madek.auth.utils.core :refer [keyword presence str]]
   [reagent.core :as reagent]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn wait-component [req]
  [:div.wait-component
   {:style {:opacity 0.4}}
   [:div.text-center
    [:i.fas.fa-spinner.fa-spin.fa-5x]]
   [:div.text-center
    {:style {}}
    "Wait for " (-> req :method str string/upper-case)
    " " (:url req)]])

