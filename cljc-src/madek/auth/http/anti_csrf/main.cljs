(ns madek.auth.http.anti-csrf.main
  (:require
   [goog.net.Cookies]
   [madek.auth.http.shared :refer [ANTI_CRSF_TOKEN_COOKIE_NAME]]))

(defonce ^:dynamic *cookies* (or goog.net.cookies (.getInstance goog.net.Cookies)))

(defn token []
  (.get *cookies* ANTI_CRSF_TOKEN_COOKIE_NAME))

(defn hidden-form-group-token-component []
  [:div.form-group
   [:input
    {:name :csrf-token
     :type :hidden
     :value (token)}]])
