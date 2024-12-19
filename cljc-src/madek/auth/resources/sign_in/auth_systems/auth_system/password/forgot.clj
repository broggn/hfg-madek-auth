(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.forgot
  (:require
   [clojure.string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.routes :refer [path]]
   [madek.auth.utils.core :refer [presence]]
   [madek.auth.utils.emails :refer [send-email!]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre :refer [debug spy]]))

(defn email-template-de [token link ext-base-url site-title]
  (str "Guten Tag,\n"
       "\n"
       (str "Sie möchten ein neues Passwort für Ihr Benutzerkonto auf " site-title ".\n")
       "\n"
       "Um ein neues Passwort zu setzen, öffnen Sie folgenden Link::\n"
       (str link "\n")
       "\n"
       "Falls der Link nicht funktioniert, kopieren Sie bitte folgenden Code in das entsprechende Feld auf der Website: " token "\n"
       "\n"
       "Wünschen Sie kein neues Passwort? Ignorieren Sie bitte diese E-Mail.\n"
       "\n"
       (str site-title "\n")
       ext-base-url))

(defn email-template-en [token link ext-base-url site-title]
  (str "Hello,\n"
       "\n"
       (str "You have requested to set a new password for your account on " site-title ".\n")
       "\n"
       "To set a new password click on this link:\n"
       (str link "\n")
       "\n"
       "Enter the following code on the website in case the link does not work: " token "\n"
       "\n"
       "If you did not request a password change, please ignore this email.\n"
       "\n"
       (str site-title "\n")
       ext-base-url))

(defn email-body
  [token lang {site-titles :site_titles, ext-base-url :madek_external_base_url}]
  (let [token-path (path :sign-in-user-auth-system-password-reset
                         {:auth_system_type "password"
                          :auth_system_id "password"}
                         (cond-> {:token token}
                           (= lang "en") (assoc :lang lang)))
        site-title (get site-titles lang)
        link (str ext-base-url token-path)]
    (if (= lang "en")
      (email-template-en token link ext-base-url site-title)
      (email-template-de token link ext-base-url site-title))))

(defn email-subject [lang {site-titles :site_titles}]
  (str (get site-titles lang) (if (= lang "en")
                                ": set new password"
                                ": Neues Passwort setzen")))

(def TOKEN-VALIDITY-DURATION "1 hour")

(defn insert-into-user-password-resets!
  [tx user-id email-or-login]
  (-> (sql/insert-into :user_password_resets)
      (sql/values [{:user_id [:cast user-id :uuid],
                    :used_user_param email-or-login,
                    :valid_until [:+ [:now] [:interval TOKEN-VALIDITY-DURATION]]}])
      (sql/returning :token)
      sql-format
      (->> (jdbc/execute! tx))))

(comment
  (require '[madek.auth.db.core :as db])
  (insert-into-user-password-resets! (db/get-ds)
                                     "16ae30bc-8f4a-4aef-aafe-918ec1c8b03e"
                                     "mkmit"))
(defn handler [{tx :tx settings :settings
                {email-or-login :email-or-login lang :lang} :params
                :as request}]
  (if-let [user (-> (sql/select :users.*)
                    (sql/from :users)
                    (sql/where [:= :users.password_sign_in_enabled true])
                    (sql/where [:or [:= :users.email email-or-login]
                                [:= :users.login email-or-login]])
                    sql-format
                    (#(jdbc/execute-one! tx %)))]
    (let [token (-> (insert-into-user-password-resets! tx (:id user) email-or-login)
                    first
                    :token)
          lang (or (presence lang) "de")
          subject (email-subject lang settings)
          body (email-body token lang settings)]
      (send-email! request user subject body)
      {:status 200, :body {:message "OK"}})
    {:status 403, :body {:error_message "User not found or password sign-in for the user is disabled."}}))
