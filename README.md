Madek-AUTH
==========

Authentication service for Madek.


## AuthSystem PKI

To generate a key pair:

    openssl ecparam -name prime256v1 -genkey -noout -out tmp/key.pem
    openssl ec -in tmp/key.pem -pubout -out tmp/public.pem


### Passphrase Protected Keys

Optionally protecd the private key with a password:

    openssl ec -in tmp/key.pem -out key-protected.pem -aes256 -passin 'pass:MYSECRET'


The server takes an optional argument '--passwords-path' to a YAML encode file
with pairs of key and password. The key must be **equal to id** of the
authentication-system.






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
