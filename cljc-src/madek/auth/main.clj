(ns madek.auth.main
  (:require 
    [madek.auth.utils.yaml :as yaml]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli]
    [environ.core :refer [env]]
    [madek.auth.server :as server]
    [madek.auth.utils.logging :as logging]
    [madek.auth.utils.repl :as repl]
    [taoensso.timbre :refer [debug error info spy warn]]
    ))

(def cli-options
  (concat
    [["-h" "--help"]
     [nil "--dev-mode DEV_MODE" "dev mode true|false yaml boolean"
      :default (or (some-> :dev-mode env yaml/parse) false)
      :parse-fn yaml/parse 
      :validate [boolean? "Must parse to a boolean"]]]
    repl/cli-options))

(defn main-usage [options-summary & more]
  (->> ["" 
        "Madek Auth Service"
        ""
        "usage: madek-auth [<opts>] SCOPE [<scope-opts>] ..."
        ""
        "available scopes: server"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn main [args]
  (try 
    (logging/init)
    (let [{:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options :in-order true)
          cmd (some-> arguments first keyword)
          pass-on-args (->> (rest arguments) flatten (into []))
          options (into (sorted-map) options)
          print-summary #(println (main-usage summary {:args args :options options}))]
      (info {'args args 'options options 'cmd cmd 'pass-on-args pass-on-args})
      ;(when (:dev-mode options) (cider-ci.dev/init #'cider-ci.main/main args))
      (repl/init options)
      (cond
        (:help options) (print-summary)
        :else (case cmd
                :server (server/main options pass-on-args)
                (print-summary))))

    (catch Exception ex
      (warn ex)
      (throw ex))))


(defonce args* (atom nil))
(when @args* (main @args*))

(defn -main [& args]
  (reset! args* (or args []))
  (main args))

