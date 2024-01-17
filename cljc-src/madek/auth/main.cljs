(ns madek.auth.main
  (:require
   [madek.auth.html.spa.main :as spa]
   [madek.auth.routing.main :as routing]
   [madek.auth.state :as state]
   [madek.auth.utils.logging :as logging]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defn ^:dev/after-load init [& args]
  (logging/init)
  (state/init)
  (routing/init)
  (spa/mount))
