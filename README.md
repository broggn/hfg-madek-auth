Madek-AUTH
==========

Authentication service for Madek.




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


## Copyright and license

Madek is (C) Zürcher Hochschule der Künste (Zurich University of the Arts).

Madek is Free Software under the GNU General Public License (GPL) v3, see the included LICENSE file for license details.

Visit our main website at http://www.zhdk.ch and the IT center
at http://itz.zhdk.ch
