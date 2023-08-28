(ns madek.auth.localization
  (:require 
   [taoensso.timbre :refer [debug info warn error spy]]
   [madek.auth.state :as state]
   [madek.auth.translations :refer [get-translation]]))

(defn- current-lang []
  (or (some-> @state/routing* :query-params :lang)
      (some-> @state/settings* :default_locale)
      "de"))

(defn translate [key]
  (get-translation key (current-lang)))

(defn localized-setting [key]
  (let [setting (key @state/settings*)
        lang (keyword (current-lang))]
    (or (->> setting lang)
        (->> setting vals (filter some?)))))
