(ns madek.auth.resources.sign-in.auth-systems.auth-system.password.shared)

(def pwd-strength-regex #"(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*")

(defn satisfies-strength? [password]
  (->> password
       (re-matches pwd-strength-regex)
       (and (>= (count password) 12))
       boolean))
