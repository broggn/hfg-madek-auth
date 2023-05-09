(ns madek.auth.utils.url
  (:require
    #?(:clj [ring.util.codec])
    ))

(def decode
  #?(:cljs js/decodeURIComponent
     :clj ring.util.codec/url-decode))

(def encode
  #?(:cljs js/encodeURIComponent
     :clj ring.util.codec/url-encode))


;(comment (-> "/auth/info" encode decode))
