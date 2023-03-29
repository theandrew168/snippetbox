(ns snippetbox.store.snippet)

(defprotocol SnippetStore
  (create [_ snippet])
  (read-by-id [_ id at])
  (list-recent [_ n at]))
