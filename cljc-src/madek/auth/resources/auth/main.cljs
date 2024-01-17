(ns madek.auth.resources.auth.main
  (:require
   [madek.auth.utils.core]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn page []
  [:h1 "Madek Auth"])

(def components
  {:page page})
