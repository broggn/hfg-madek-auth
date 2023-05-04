Madek-AUTH
==========

Furture authentication service for Madek.


## Warning

Status: BETA.

DB migrations: not in final state yet

Complete rebase pending!


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

    ./bin/rspec spec/features/sign-in_spec.rb

    

### Notes 

* OTP-library https://github.com/suvash/one-time
