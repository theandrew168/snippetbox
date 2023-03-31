(ns snippetbox.core
  (:require [com.stuartsierra.component :as component]
            [snippetbox.component.database :as database]
            [snippetbox.component.migrate :as migrate]
            [snippetbox.component.server :as server])
  (:gen-class))

;; data types (models) have three "versions":
;; 1. The inbound type (no ID, possible conversions, usually created via form)
;; 2. The outbound type (no ID, possible conversions, usually rendered via html)
;; 3. The domain type (all data, raw, used by the app for internal processing)

(defn default-config []
  {:database-uri "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"
   :secret-key "28548351a2d4b500a6ff94d72c6d3f1e"
   :port 5000
   :threads (.availableProcessors (Runtime/getRuntime))})

(defn system [config]
  (let [{:keys [database-uri secret-key port threads]} config]
    (component/system-map
     :database (database/map->Database {:uri database-uri})
     :migration (component/using
                 (migrate/map->Migrate {})
                 [:database])
     :webserver (component/using
                 (server/map->Server {:secret-key secret-key :port port :threads threads})
                 [:database]))))

(defn -main [& args]
  (let [config (default-config)
        sys (system config)]
    (component/start sys)))

(comment

  (def sys (system (default-config)))
  (alter-var-root #'sys component/start)
  (alter-var-root #'sys component/stop)

  :rcf)
