(ns madek.auth.resources.sign-in.main
  (:require
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn handler [{:as request}]
  (info request)
  {:status 444})
