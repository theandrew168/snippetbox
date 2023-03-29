(ns snippetbox.component.migrate
  (:require [com.stuartsierra.component :as component]
            [snippetbox.migrate :as migrate]))

(defrecord Migrate [database]
  component/Lifecycle

  (start [this]
    (let [conn (:connection database)]
      (println (format "Applying migrations..."))
      (migrate/migrate! conn)
      this))

  (stop [this]
    this))
