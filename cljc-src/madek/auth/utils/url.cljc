(ns madek.auth.utils.url
  (:require
   #?(:clj [ring.util.codec])
   #?(:cljs [goog.string :refer [urlDecode urlEncode]])))

(def decode
  #?(:cljs urlDecode
     :clj ring.util.codec/url-decode))

(def encode
  #?(:cljs urlEncode
     :clj ring.util.codec/url-encode))

;(comment (-> "/auth/info" encode decode))
