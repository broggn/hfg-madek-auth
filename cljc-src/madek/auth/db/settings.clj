(ns madek.auth.db.settings
  (:require
   [clj-yaml.core :as yaml]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.db.core :refer [get-ds]]
   [madek.auth.db.type-conversion]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as connection]
   [next.jdbc.result-set :as jdbc-rs]
   [taoensso.timbre :refer [debug info warn error spy]]))

(defonce yaml-settings (atom nil))

(defn init []
  (info "Loading settings from ../config/settings.yml")
  (reset! yaml-settings
          (try
            (let [settings-file (some #(when (.exists (java.io.File. %)) %)
                                      ["../config/settings.local.yml"
                                       "../config/settings.yml"])
                  settings (yaml/parse-string (slurp settings-file))]
              (info "YAML settings:" settings)
              settings)
            (catch Exception e
              (warn (str "Error parsing settings YAML file: " (.getMessage e))))))
  (info "Loading settings from ../config/settings.yml done."))

(def selected-columns
  [:brand_logo_url
   :brand_texts
   :site_titles
   :default_locale
   :available_locales
   :sitemap])

(defn settings [tx]
  (or (-> (apply sql/select selected-columns)
          (sql/from :app_settings)
          (sql/where [:= :id 0])
          sql-format
          (#(jdbc/execute-one! tx %)))
      (warn "There seem to be no (app-) settings; this instance might not be set up properly.")))

(defn smtp-settings [tx]
  (or (-> (sql/select [:default_from_address :smtp_default_from_address])
          (sql/from :smtp_settings)
          (sql/where [:= :id 0])
          sql-format
          (#(jdbc/execute-one! tx %)))
      (warn "There seem to be no smtp_settings; this instance might not be set up properly.")))

(defn wrap
  ([handler]
   (fn [request]
     (wrap handler request)))
  ([handler {tx :tx :as request}]
   (handler (assoc request :settings
                   (merge @yaml-settings
                          (settings tx)
                          (smtp-settings tx))))))
