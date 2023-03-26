(ns snippetbox.core
  (:require [integrant.core :as ig]
            [snippetbox.storage]
            [snippetbox.web])
  (:gen-class))

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
