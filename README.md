Madek-AUTH
==========

Furture authentication service for Madek.


## Warning

Status: BETA.

DB migrations: not in final state yet

Complete rebase pending!


## Development

requirements: 
* linux, MacOS 
* asdf-vm

run the backend service:  

    ./bin/clj-dev server

run frontend watch compilation: 

    ./bin/cljs-watch
  
start test authentication system (yet incomplete): 

    ./bin/test-auth-system

run a test: 

    ./bin/rspec spec/features/sign-in_spec.rb

    

