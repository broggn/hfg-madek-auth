(ns madek.auth.server
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :as cli]
   [madek.auth.db.core :as db]
   [madek.auth.http.server :as http-server]
   [madek.auth.routing.main :as routing]
   [madek.auth.state :as state]
   [madek.auth.utils.exit :as exit]
   [taoensso.timbre :refer [debug error info spy warn]]))

(def ^:dynamic options {})

(defn shutdown []
  (http-server/stop))

(defn init-http [options]
  (-> (routing/init options)
      (http-server/init options)))

(defn run [options]
  (def ^:dynamic options options)
  (info "run with options:" (str options))
  (exit/init options)
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(shutdown)))
  (state/init options)
  ;(init-shutdown options)
  (db/init options)
  ;(settings/init)
  (init-http options))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]]
   exit/pid-file-options
   db/cli-options
   state/cli-options
   http-server/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Madek Auth"
        ""
        "usage: madek-auth [<opts>] server [<server-opts>]"
        ""
        "Arguments to options can also be given through environment variables or java system properties."
        "Boolean arguments are parsed as YAML i.e. yes, no, true or false strings are interpreted. "
        ""
        "Run options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn main [parent-options args]
  (info 'main [parent-options args])
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (merge (sorted-map) parent-options options)]
    (def ^:dynamic options options)
    (if (:help options)
      (do (println (main-usage summary {:args args :options options}))
          (exit/exit))
      (run options))))

