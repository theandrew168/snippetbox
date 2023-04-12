(ns snippetbox.util-test
  (:require [clojure.test :refer [deftest is]]
            [snippetbox.util :as util]))

(deftest test-hex->bytes
  (is (= '(222 173 190 239) (util/hex->bytes "deadbeef"))))
