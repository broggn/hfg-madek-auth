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

   :login-box-title ["Login" "Login"]

   :step1-username-label ["E-Mail"
                          "Email"]
   :step1-submit-label ["Weiter"
                        "Continue"]

   :step2-message-username-missing ["E-Mail fehlt"
                                    "Email missing"]
   :step2-message-login-not-possible ["Einloggen nicht möglich mit dieser E-Mail"
                                      "Log in not possible with with this email"]
   :step2-username-label ["E-Mail"
                          "Email"]
   :step2-login-as ["Anmelden als"
                    "Login as"]
   :step2-back-label ["Zurück"
                      "Back"]
   :step2-change-username-label ["Andere E-Mail"
                                 "Other email"]

   :step3-message-password-required ["Passwort ist ein Pflichtfeld"
                                     "Password is required"]
   :step3-message-login-failed ["Unbekannte E-Mail oder falsches Passwort"
                                "Unknown email or wrong password"]
   :step3-username-label ["E-Mail"
                          "Email"]
   :step3-password-label ["Passwort"
                          "Password"]
   :step3-change-username-label ["Andere E-Mail"
                                 "Other email"]
   :step3-submit-label ["Anmelden"
                        "Log in"]

   :ext-redirecting ["Automatische Weiterleitung..."
                     "Redirecting..."]
   :ext-callback-processing ["..."
                             "..."]
   :ext-callback-success ["Login erfolgreich, Weiterleitung zum Archiv..."
                          "Login successful, redirecting back to archive..."]})

(defn get-translation [key lang]
  (let [translations (or (-> translations key seq) [(str "[Missing: " key "]")])
        lang-index (cond (= lang "en") (min 1 (- (count translations) 1)) :else 0)]
    (nth translations lang-index)))
