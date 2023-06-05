(ns madek.auth.resources.sign-in.auth-systems.auth-system.external.pki
  (:require
    [buddy.core.keys :as keys]
    [buddy.sign.jwt :as jwt]
    [cuerdas.core :as str]
    [madek.auth.utils.core :refer [presence]]
    [taoensso.timbre :refer [debug error info spy warn]]))


(defn prepare-key-str [s]
  (->> (-> s (str/split #"\n"))
       (map str/trim)
       (map presence)
       (filter identity)
       (str/join "\n")))

(defn private-key! [s]
  (-> s prepare-key-str keys/str->private-key
      (or (throw
            (ex-info "Private key error!"
                     {:status 500})))))

(defn public-key! [s]
  (-> s prepare-key-str keys/str->public-key
      (or (throw
            (ex-info "Public key error!"
                     {:status 500})))))


