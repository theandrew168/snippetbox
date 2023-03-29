(ns snippetbox.store.postgresql
  (:require [snippetbox.store.postgresql.snippet :refer [map->PostgreSQLSnippetStore]]))

(defn store [conn]
  {:snippet (map->PostgreSQLSnippetStore {:conn conn})})
