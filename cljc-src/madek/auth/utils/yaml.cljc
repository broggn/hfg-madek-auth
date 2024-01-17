(ns madek.auth.utils.yaml
  (:require
   #?(:cljs ["js-yaml" :as yaml]
      :clj [clj-yaml.core :as yaml])
   [clojure.walk]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defn parse [s]
  (clojure.walk/keywordize-keys
   #?(:clj (-> s yaml/parse-string)
      :cljs (-> s yaml/load js->clj))))

