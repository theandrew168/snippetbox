(ns snippetbox.store.postgresql
  (:require [snippetbox.store :refer [map->Store]]
            [snippetbox.store.postgresql.snippet :refer [map->PostgreSQLSnippetStore]]))

(defn store [conn]
  (map->Store
   {:snippet (map->PostgreSQLSnippetStore {:conn conn})}))
