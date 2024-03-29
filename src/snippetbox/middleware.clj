(ns snippetbox.middleware
  (:require [clojure.repl :refer [pst]]
            [clojure.string :refer [upper-case]]
            [ring.middleware.params :as ring.params]
            [ring.middleware.session :as ring.session]
            [ring.middleware.session.cookie :as ring.cookie]
            [ring.util.request :as ring.request]
            [snippetbox.response :as response]
            [snippetbox.util :as util]))

(defn wrap-secure-headers [handler]
  (let [headers {"Referrer-Policy" "origin-when-cross-origin"
                 "X-Content-Type-Options" "nosniff"
                 "X-Frame-Options" "deny"
                 "X-XSS-Protection" "0"}]
    (fn [req]
      (let [resp (handler req)]
        (assoc-in resp [:headers] (merge headers (:headers resp)))))))

(defn wrap-access-log [handler]
  (fn [req]
    (let [addr (:remote-addr req)
          method (upper-case (name (:request-method req)))
          url (ring.request/request-url req)]
      (printf "%s - %s %s\n" addr method url)
      (flush)
      (handler req))))

(defn wrap-errors [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (do
             (pst e)
             (response/internal-server-error))))))

(defn apply-middleware [routes secret-key]
  (let [key (byte-array (util/hex->bytes secret-key))]
    (-> routes
        ring.params/wrap-params
        (ring.session/wrap-session
         {:store (ring.cookie/cookie-store {:key key})
          :cookie-attrs {:max-age (* 7 24 60 60) ;; 7 days
                         :secure true
                         :http-only true
                         :same-site :lax}})
        wrap-secure-headers
        wrap-access-log
        wrap-errors)))
