(ns madek.auth.translations
  (:require
   [taoensso.timbre :refer [debug info warn error spy]]))

(def translations
  {:user-menu-my-archive ["Mein Archiv"
                          "My archive"]
   :user-menu-info ["Session-Info"
                    "Session Info"]
   :user-menu-sign-out ["Abmelden"
                        "Log out"]

   :login-box-title ["Medienarchiv-Login" "Media Archive Login"]

   :step1-username-label ["E-Mail oder Benutzername"
                          "Email or username"]
   :step1-submit-label ["Weiter"
                        "Continue"]

   :step2-message-username-missing ["Benutzername fehlt"
                                    "Username missing"]
   :step2-message-login-not-possible ["Einloggen nicht möglich mit diesem Benutzernamen"
                                      "Log in not possible with with this username"]
   :step2-username-label ["Benutzername"
                          "Username"]
   :step2-login-as ["Anmelden als"
                    "Login as"]
   :step2-back-label ["Zurück"
                      "Back"]
   :step2-change-username-label ["Anderer Benutzername"
                                 "Other username"]

   :step3-message-password-required ["Passwort ist ein Pflichtfeld"
                                     "Password is required"]
   :step3-message-login-failed ["Falscher Benutzername/Passwort"
                                "Unknown username or wrong password"]
   :step3-username-label ["Benutzername"
                          "Username"]
   :step3-password-label ["Passwort"
                          "Password"]
   :step3-change-username-label ["Anderer Benutzername"
                                 "Other username"]
   :step3-submit-label ["Anmelden"
                        "Log in"]})

(defn get-translation [key lang]
  (let [translations (or (-> translations key seq) [(str "[Missing: " key "]")])
        lang-index (cond (= lang "en") (min 1 (- (count translations) 1)) :else 0)]
    (nth translations lang-index)))
