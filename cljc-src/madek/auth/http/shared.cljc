(ns madek.auth.http.shared
  (:require
    [taoensso.timbre :refer [debug error info spy warn]]
    ))

(def ANTI_CRSF_TOKEN_COOKIE_NAME "madek.auth.anti-csrf-token")

(def HTTP_UNSAFE_METHODS #{:delete :patch :post :put})
(def HTTP_SAFE_METHODS #{:get :head :options :trace})
