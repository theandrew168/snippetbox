(ns snippetbox.storage.postgresql
  (:require [snippetbox.storage.snippet :refer [->PostgreSQLSnippetStorage]]))

(defn init [conn]
  {:snippet (->PostgreSQLSnippetStorage conn)})
