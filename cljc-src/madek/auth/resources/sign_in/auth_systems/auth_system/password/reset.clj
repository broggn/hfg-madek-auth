(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.reset
  (:refer-clojure :exclude [get])
  (:require
   [clojure.string :as string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.request :refer [password-auth-system!]]
   [madek.auth.resources.sign-in.auth-systems.auth-system.password.shared :refer [satisfies-strength?]]
   [madek.auth.utils.core :refer [presence]]
   [madek.auth.utils.emails :refer [send-email!]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :as timbre :refer [debug spy]]
   [taoensso.timbre :refer [error warn info debug spy]]
   [tick.core :as tick]))

(defn normalize-token-str [str]
  (-> str
      string/upper-case
      (string/escape {\O 0, \I 1, \L 1})))

(defn get-from-user-password-resets
  [tx token-param]
  (-> (sql/select :*)
      (sql/from :user_password_resets)
      (sql/where [:= (normalize-token-str token-param) :token])
      sql-format
      (->> (jdbc/execute-one! tx))))

(defn get [{tx :tx {token :token} :params :as request}]
  (let [pwd-reset (get-from-user-password-resets tx token)
        user (-> (sql/select :users.*)
                 (sql/from :users)
                 (sql/where [:= :users.id (:user_id pwd-reset)])
                 sql-format
                 (#(jdbc/execute-one! tx %)))]
    (cond
      (not pwd-reset)
      {:status 404,
       :body {:error_message "Password reset for the token not found."}}

      (not (:password_sign_in_enabled user))
      {:status 403,
       :body {:error_message "Password sign-in for the user is disabled."}}

      (tick/> (tick/now) (:valid_until pwd-reset))
      {:status 403,
       :body {:error_message "The token has expired."}}

      :else {:status 201,
             :body {:email-or-login (:used_user_param pwd-reset)}})))

; ==================================================================================================

(defn password-hash [tx password]
  (-> (sql/select [[:crypt password [:gen_salt "bf"]] :pw_hash])
      sql-format
      (->> (jdbc/execute-one! tx))
      :pw_hash))

(defn sql-command [user-id pw-hash]
  (-> (sql/insert-into :auth_systems_users)
      (sql/values [{:user_id user-id
                    :auth_system_id "password"
                    :data pw-hash}])
      (sql/on-conflict :user_id :auth_system_id)
      (sql/do-update-set :data)
      (sql/returning :*)
      sql-format))

(defn set-password! [tx user-id password]
  (let [pw-hash (password-hash tx (str password))
        sql-command (sql-command user-id pw-hash)]
    (jdbc/execute! tx sql-command)))

(comment (do (require '[madek.auth.db.core :as db])
             #_(sql-command #uuid "16ae30bc-8f4a-4aef-aafe-918ec1c8b03e"
                            (password-hash (db/get-ds) "password"))
             #_(password-hash (db/get-ds) (str 1223))
             (-> (sql/select :*)
                 (sql/from :emails)
                 (sql/limit 1)
                 sql-format
                 (->> (jdbc/execute-one! (db/get-ds)))
                 :body)
             #_(set-password! (db/get-ds)
                              #uuid "16ae30bc-8f4a-4aef-aafe-918ec1c8b03e"
                              "bhole")))

; ==================================================================================================

(defn email-template-en [ext-base-url site-title]
  (str "Hello,\n"
       "\n"
       (str "Your password change was successful. A new password was set for your account at "
            site-title ".\n")
       "\n"
       (str site-title "\n")
       ext-base-url))

(defn email-template-de [ext-base-url site-title]
  (str "Hello,\n"
       "\n"
       (str "Ihr Passwort wurde erfolgreich geändert. Für Ihr Benutzerkonto auf "
            site-title " wurde ein neues Passwort gesetzt.\n")
       "\n"
       (str site-title "\n")
       ext-base-url))

(defn email-body
  [lang {site-titles :site_titles, ext-base-url :madek_external_base_url}]
  (let [site-title (clojure.core/get site-titles lang)]
    (if (= lang "en")
      (email-template-en ext-base-url site-title)
      (email-template-de ext-base-url site-title))))

(defn email-subject [lang {site-titles :site_titles}]
  (str (clojure.core/get site-titles lang)
       (if (= lang "en")
         ": Changed Password"
         ": Passwort geändert")))

; ==================================================================================================

(defn post [{tx :tx, settings :settings,
             {token :token, password :password} :body
             {lang :lang} :params
             :as request}]
  (let [pwd-reset (get-from-user-password-resets tx token)
        user (-> (sql/select :users.*)
                 (sql/from :users)
                 (sql/where [:= :users.id (:user_id pwd-reset)])
                 sql-format
                 (#(jdbc/execute-one! tx %)))]
    (cond
      (not pwd-reset)
      {:status 404,
       :body {:error_message "Password reset for the token not found."}}

      (not (:password_sign_in_enabled user))
      {:status 403,
       :body {:error_message "Password sign-in for the user is disabled."}}

      (tick/> (tick/now) (:valid_until pwd-reset))
      {:status 403,
       :body {:error_message "The token has expired."}}

      (not (satisfies-strength? password))
      {:status 400,
       :body {:error_message "Password does not meet the requirements."}}

      :else (do (set-password! tx (:user_id pwd-reset) password)
                (let [lang (or (presence lang) "de")]
                  (send-email! request user
                               (email-subject lang settings)
                               (email-body lang settings)))
                {:status 204}))))

; ==================================================================================================

(defn dispatch [request]
  (case (:request-method request)
    :get (get request)
    :post (post request)
    (throw (ex-info "Unknown method" {}))))
