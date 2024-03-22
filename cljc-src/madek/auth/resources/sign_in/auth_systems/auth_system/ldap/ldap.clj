(ns madek.auth.resources.sign-in.auth-systems.auth-system.ldap.ldap
  (:import [java.util Map Hashtable]
           [javax.naming Context AuthenticationException]
           [javax.naming.directory Attribute SearchControls]
           [javax.naming.ldap InitialLdapContext LdapContext ExtendedRequest ExtendedResponse Rdn])
  (:require
  
   [clojure.string :as string]
   [taoensso.timbre :refer [debug info warn]])
  )

(defn format-ldap-auth [username]
  (format "uid=%s,ou=people,dc=hfg-karlsruhe,dc=de" (Rdn/escapeValue username)))

(def naming-ctx {Context/INITIAL_CONTEXT_FACTORY "com.sun.jndi.ldap.LdapCtxFactory"
                 Context/PROVIDER_URL "ldaps://ldap.hfg-karlsruhe.de:636/"
                 Context/SECURITY_AUTHENTICATION "simple"
                 "com.sun.jndi.ldap.connect.pool" "false"
                 "com.sun.jndi.ldap.tls.cbtype" "tls-server-end-point"
                 })

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
        env (Hashtable. ^Map (assoc naming-ctx
                                    Context/SECURITY_PRINCIPAL princ
                                    Context/SECURITY_CREDENTIALS password))]
    (info "ldap-auth " princ "\nenv " env)
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
        (warn "got exception : " e)
        nil))))

(defn ldap-list-users []
  (let [env (Hashtable. ^Map naming-ctx)]
    (info "ldap-list-users: env " env)
    (with-open [ctx (InitialLdapContext. env nil)]
      (info "ldap-list-users: ctx " ctx)
        
      (let [all (enumeration-seq (.listBindings ctx "ou=people,dc=hfg-karlsruhe,dc=de"))
            ;userlist (for [u all] (.getAttributes (.getObject u)))
            userlist (for [u all] (.getObject u))
            ]
        (info "ldap-list-users: " all "\n users \n "userlist)
        userlist
        ))))

(defn search-controls []
  (let [sc (SearchControls. )
        sc2 (.setSearchScope sc (SearchControls/SUBTREE_SCOPE ))]
    sc2))

(defn ldap-has-user [email-or-login]
  (let [env (Hashtable. ^Map naming-ctx)]
    (info 'ldap-has-user 'env env)
    (with-open [ctx (InitialLdapContext. env nil)]
      (let [filter-mail (apply str "(&(mail=" email-or-login "))");
            filter-login (apply str "(&(uid=" email-or-login "))");
            has-login (.hasMore (.search ctx "ou=people,dc=hfg-karlsruhe,dc=de" filter-login (search-controls)))
            has-mail (.hasMore (.search ctx "ou=people,dc=hfg-karlsruhe,dc=de" filter-mail (search-controls)))
            hasUser (or has-login has-mail)]
        hasUser)
      )))


