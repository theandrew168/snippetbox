(ns snippetbox.core
  (:require [integrant.core :as ig]
            [snippetbox.storage]
            [snippetbox.web])
  (:gen-class))

;; config - none (defaults in code + config.edn)
;; database - config
;; app - config, database

;; data types (models) have three "versions":
;; 1. The inbound type (no ID, possible conversions, usually created via form)
;; 2. The outbound type (no ID, possible conversions, usually rendered via html)
;; 3. The domain type (all data, raw, used by the app for internal processing)

(defn -main [& args]
  (-> "config.edn"
      slurp
      ig/read-string
      (ig/init [:app/web])))

(comment

  (def system
    (-> "config.edn"
        slurp
        ig/read-string
        (ig/init [:app/web])))
  
  (ig/halt! system [:app/web])

  :rcf)
