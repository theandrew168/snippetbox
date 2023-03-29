(ns snippetbox.store.postgresql
  (:require [snippetbox.store.snippet :refer [map->PostgreSQLSnippetStore]]))

(defn store [conn]
  {:snippet (map->PostgreSQLSnippetStore {:conn conn})})
