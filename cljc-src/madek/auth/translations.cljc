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

   :forgot-password-label ["Passwort vergessen?"
                           "Forgot password?"]
   :forgot-password-txt ["Erhalten Sie einen Link per E-Mail, um das Passwort zurückzusetzen."
                         "Receive a link by email to reset the password."]
   :forgot-password-send-label ["E-Mail senden"
                                "Send email"]
   :forgot-password-success-txt ["Es wurde ein Mail mit einem Link zum Zurücksetzen des Passworts gesendet. Bitte prüfen Sie Ihren E-Mail-Posteingang."
                                 "An email with a link to reset the password has been sent. Please check your inbox."]
   :forgot-password-manual-entry-info ["Falls der Link nicht funktionieren sollte: "
                                       "If the link does not work: "]
   :forgot-password-reset-link-label ["Klicken Sie hier, um den im E-Mail enthaltenen Code manuell einzugeben"
                                      "Click here to manually enter the code included in the email"]

   :reset-password-txt ["Geben Sie den Code ein, welchen Sie per E-Mail erhalten haben."
                        "Enter the code you received by email."]
   :reset-password-token-input-label ["Code"
                                      "Code"]
   :reset-password-username-input-label ["E-Mail oder Login"
                                         "Email or login"]
   :reset-password-new-password-input-label ["Neues Passwort"
                                             "New password"]
   :reset-password-message-password-required ["Passwort ist ein Pflichtfeld"
                                              "Password is required"]
   :reset-password-success-txt ["Das Passwort wurde erfolgreich zurückgesetzt."
                                "The password has been successfully reset."]
   :reset-password-login-link-label ["Klicken Sie hier, um sich anzumelden."
                                     "Click here to log in."]
   :reset-password-strength-hint ["Das Passwort muss mindestens 12 Zeichen lang sein und mindestens einen Grossbuchstaben, einen Kleinbuchstaben und eine Zahl enthalten."
                                  "The password must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, and one number."]
   :reset-password-strength-validation-message ["Bitte Passwortregel beachten"
                                                "Please follow the password rule"]

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
