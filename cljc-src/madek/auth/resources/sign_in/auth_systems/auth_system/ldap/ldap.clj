(ns madek.auth.resources.sign-in.auth-systems.auth-system.ldap.ldap
  (:import [java.util Map Hashtable]
           [javax.naming Context AuthenticationException]
           [javax.naming.directory Attribute SearchControls]
           [javax.naming.ldap InitialLdapContext LdapContext ExtendedRequest ExtendedResponse Rdn])
  (:require
   [clojure.string :as string]
   [environ.core :refer [env]]
   [madek.auth.utils.cli :refer [long-opt-for-key]]
   [taoensso.timbre :refer [debug info warn]])
  )

;;; cli-options ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce options* (atom nil))

(def ldap-provider-key :ldap-provider)
(def ldap-security-auth-key :ldap-security-auth)
(def ldap-connect-pool-key :ldap-connect-pool)
(def ldap-tls-cbtype-key :ldap-tls-cbtype)

(def ldap-dc-key :ldap-dc)
(def ldap-users-search-key :ldap-users-search)
(def ldap-login-search-key :ldap-login-search)
(def ldap-email-search-key :ldap-email-search)


(def options-keys [ldap-provider-key
                   ldap-security-auth-key
                   ldap-connect-pool-key
                   ldap-tls-cbtype-key

                   ldap-dc-key
                   ldap-users-search-key
                   ldap-login-search-key
                   ldap-email-search-key])

(def cli-options
  [[nil (long-opt-for-key ldap-provider-key) "LDAP provider url, default is undef, so no ldap is enabled"
    :default (or (some-> ldap-provider-key env)
                 "ldaps://ldap.hfg-karlsruhe.de:636/")]
   [nil (long-opt-for-key ldap-security-auth-key) "LDAP security auth | simple"
    :default (or (some-> ldap-security-auth-key env)
                 "simple")]
   [nil (long-opt-for-key ldap-connect-pool-key) "LDAP connect pool settings | false"
    :default (or (some-> ldap-connect-pool-key env)
                 "false")]
   [nil (long-opt-for-key ldap-tls-cbtype-key) "LDAP security tls cbtype | tls-server-entpoint"
    :default (or (some-> ldap-tls-cbtype-key env)
                 "tls-server-entpoint")]
   [nil (long-opt-for-key ldap-dc-key) "LDAP domain controller | dc=hfg-karlsruhe,dc=de"
    :default (or (some-> ldap-dc-key env)
                 "dc=hfg-karlsruhe,dc=de")]
   [nil (long-opt-for-key ldap-login-search-key) "LDAP user login search string | uid"
    :default (or (some-> ldap-login-search-key env)
                 "uid")]
   [nil (long-opt-for-key ldap-email-search-key) "LDAP user email search string | mail"
    :default (or (some-> ldap-email-search-key env)
                 "mail")]
   [nil (long-opt-for-key ldap-users-search-key) "LDAP user email search string | ou=people"
    :default (or (some-> ldap-users-search-key env)
                 "ou=people")]
   ])

(defn dc-login []
  (:ldap-login-search @options*))
;"uid"

(defn dc-email []
  (:ldap-email-search @options*))
;"mail"

(defn dc-domain []
  (:ldap-dc @options*))
;"dc=hfg-karlsruhe,dc=de"

(defn dc-users [] (apply str (:ldap-users-search @options*) "," (dc-domain)))
;ou=people

(defn format-ldap-auth [username]
  (let [;search (:ldap-auth-search @options*)
        search (apply str (dc-login) "=%s," (dc-users))
        result (format search (Rdn/escapeValue username))]
    (info 'format-ldap-auth 'search search 'result result)
    #_(format "uid=%s,ou=people,dc=hfg-karlsruhe,dc=de" (Rdn/escapeValue username))
    result))

(defn naming-ctx []
  (let [provider-url (:ldap-provider @options*)
        security (:ldap-security-auth @options*)
        connect-pool (:ldap-connect-pool @options*)
        tls-cbtype (:ldap-tls-cbtype @options*)
        result {Context/INITIAL_CONTEXT_FACTORY "com.sun.jndi.ldap.LdapCtxFactory"
                Context/PROVIDER_URL provider-url ;"ldaps://ldap.hfg-karlsruhe.de:636/"
                Context/SECURITY_AUTHENTICATION security ;"simple"
                    "com.sun.jndi.ldap.connect.pool" connect-pool ;"false"
                    "com.sun.jndi.ldap.tls.cbtype" tls-cbtype ;"tls-server-end-point"
                }]
    (info "naming-ctx "
          'provider-url provider-url
          'security security 
          'result result)
    result))


(defn init
  ([options]
   (let [ldap-options (select-keys options options-keys)]
     (info "Initializing ldap options " ldap-options)
     (reset! options* ldap-options)
     )))


(def ^:private ^:const OID "1.3.6.1.4.1.4203.1.11.3")

(defprotocol WhoAmI
  (whoAmI [this]))

(deftype WhoAmIRequest []
  ExtendedRequest
  (getID [_]
    OID)
  (getEncodedValue [_]
    nil)
  (createExtendedResponse [_ id bytes offset length]
    (reify ExtendedResponse
      (getEncodedValue [_]
        bytes)
      (getID [_]
        id)
      WhoAmI
      (whoAmI [_]
        (String. bytes offset length "UTF-8")))))

(defn who-am-i [ctx]
  (whoAmI (.extendedOperation ^LdapContext ctx (->WhoAmIRequest))))

(defn ldap-auth [user password]
  (let [^String princ (format-ldap-auth user)
        env (Hashtable. ^Map (assoc (naming-ctx)
                                    Context/SECURITY_PRINCIPAL princ
                                    Context/SECURITY_CREDENTIALS password))]
    (info "ldap-auth " princ )
    (try
      (with-open [ctx (InitialLdapContext. env nil)]
        (let [^String me (who-am-i ctx)
              result (into {} (comp
                               (map (juxt (memfn ^Attribute getID) (memfn ^Attribute getAll)))
                               (map #(update % 0 keyword))
                               (map #(update % 1 (comp vec enumeration-seq))))
                           (enumeration-seq (.. ctx (getAttributes (string/replace-first me #"^(dn|u):" "")) (getAll))))]
          (info "ldap auth result: " result)
          result))
      (catch AuthenticationException e
        (warn "got authentication exception : " e)
        nil))))

#_(defn ldap-list-users []
  (let [env (Hashtable. ^Map naming-ctx)]
    (info "ldap-list-users: env " env)
    (with-open [ctx (InitialLdapContext. env nil)]
      (info "ldap-list-users: ctx " ctx)
      (let [all (enumeration-seq (.listBindings ctx "ou=people,dc=hfg-karlsruhe,dc=de"))
            userlist (for [u all] (.getObject u))]
        userlist
        ))))



(defn dc-filter [type email-or-login]
  (apply str "(&(" type "="  email-or-login "))"))

(defn dc-search [ctx filter]
  (.search ctx (dc-users) filter
           (.setSearchScope (SearchControls.) (SearchControls/SUBTREE_SCOPE))))

(defn ldap-has-user [email-or-login]
  (let [env (Hashtable. ^Map (naming-ctx))]
    (info 'ldap-has-user 'env env)
    (with-open [ctx (InitialLdapContext. env nil)]
      (let [has-login (.hasMore (dc-search ctx (dc-filter (dc-login) email-or-login)))
            has-mail (.hasMore (dc-search ctx (dc-filter (dc-email) email-or-login)))
            hasUser (or has-login has-mail)]
        hasUser)
      )))


