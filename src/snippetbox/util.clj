(ns snippetbox.util)

(defn hex->bytes [s]
  (let [pairs (map #(apply str %) (partition 2 s))]
    (map #(Integer/parseUnsignedInt % 16) pairs)))

(comment

  (hex->bytes "deadbeef")

  :rcf)
