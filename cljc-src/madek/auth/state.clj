(ns madek.auth.state
  (:require
   [cuerdas.core :as str]
   [environ.core :refer [env]]
   [madek.auth.utils.yaml :as yaml]
   [taoensso.timbre :refer [debug info warn error spy]]))

(def cli-options
  [[nil "--passwords-path PASSWORDS-PATH" "Path to a yaml passwords file"
    :default (some-> "PASSWORDS-PATH" env)]])

(defonce passwords* (atom {}))

(defn hide-values [m]
  (into {} (for [[k v] m]
             [k (->> v
                     (map char)
                     (map (constantly "*"))
                     str/join)])))

(defn init [options]
  (info "state init with options:" (str options))
  (when (:passwords-path options)
    (reset! passwords* (-> (:passwords-path options) slurp yaml/parse))
    (info "passwords:" (hide-values @passwords*))))

