Madek-AUTH
==========

Furture authentication service for Madek.


## Warning

Status: BETA.

DB migrations: not in final state yet

Complete rebase pending!

### TODO

* accept login in addition to email


## Development

Requirements: 
* linux, MacOS 
* asdf-vm

Run the backend service:  

    ./bin/clj-dev server

Start frontend watch compilation: 

    ./bin/cljs-watch

Start stylesheet watch compilation: 

    ./bin/css-watch

Start test authentication system: 

    ./bin/test-auth-system

Run a test: 

    ./bin/rspec 'spec/features/sign-in_sign-out/external-auth_spec.rb'

    

### Notes 

* OTP-library https://github.com/suvash/one-time



#### ZHdK Groups 



```
SELECT 
    groups.institutional_name group_inst_name, 
    groups.name group_name, 
    groups.type group_type, 
    groups.institutional_id group_inst_id,
    users.institutional_id user_inst_id,
    users.login user_login
FROM users
JOIN groups_users ON groups_users.user_id = users.id
JOIN groups ON groups_users.group_id = groups.id
WHERE login = 'tschank';

  login  |                   group_name                    |     group_type      |                   group_inst_name                    |      group_inst_id
---------+-------------------------------------------------+---------------------+------------------------------------------------------+-------------------------
 tschank | Services                                        | InstitutionalGroup  | SER.personal                                         | 14645.personal
 tschank | Services                                        | InstitutionalGroup  | SER.alle                                             | 14645.alle
 tschank | Zeiterfassung                                   | InstitutionalGroup  | Verteilerliste.Zeiterfassung                         | 100920.
 tschank | ZHdK (Zürcher Hochschule der Künste)            | AuthenticationGroup |                                                      |
 tschank | Services                                        | InstitutionalGroup  | SER.alle_angestellte                                 | 14645.alle_angestellte
 tschank | wtf                                             | Group               |                                                      |
 tschank | Teschd                                          | Group               |                                                      |
 tschank | Services, Informationstechnologie-Zentrum (ITZ) | InstitutionalGroup  | SER_ITZ.alle                                         | 14647.alle
 tschank | Services, Informationstechnologie-Zentrum (ITZ) | InstitutionalGroup  | SER_ITZ.personal                                     | 14647.personal
 tschank | Services, Informationstechnologie-Zentrum (ITZ) | InstitutionalGroup  | SER_ITZ.alle_angestellte                             | 14647.alle_angestellte
 tschank | Services, ITZ, Development & Integration        | InstitutionalGroup  | SER_ITZ_Development-and-Integration.alle_angestellte | 125257.alle_angestellte
 tschank | Services, ITZ, Development & Integration        | InstitutionalGroup  | SER_ITZ_Development-and-Integration.personal         | 125257.personal
 tschank | Services, ITZ, Development & Integration        | InstitutionalGroup  | SER_ITZ_Development-and-Integration.alle             | 125257.alle
 tschank | Angemeldete Personen                            | AuthenticationGroup |                                                      |
 tschank | Forschungsprojekt: Research Video               | Group               |                                                      |
 tschank | Zutrittsgruppe für Schlüsselpersonen (Corona)   | InstitutionalGroup  | Verteilerliste.ZuKo-Schluesselpersonen               | 221990.
 tschank | Madek-Team (Software-Entwicklung)               | Group               |                                                      |
 tschank | Support Medienarchiv der Künste (MadeK)         | Group               |                                                      |
(18 rows)
```
