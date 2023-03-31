(ns snippetbox.storage)

(defprotocol Storage
  (create-snippet [_ snippet])
  (read-snippet-by-id [_ id at])
  (list-recent-snippets [_ n at])
  (create-account [_ account]))
