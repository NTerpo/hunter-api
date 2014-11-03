(ns hunter-api.util
  (:require [clojure.data.json :as json]
            [clojure.string :as str]))

(defn query-string->hashmap
  "Transform a string ~ 'foo=bar&baz=bah' in {\"foo\" \"bar\", \"baz\" \"bah\"}"
  [query-string]
  (into {}
        (map #(str/split % #"=")
             (str/split query-string #"&"))))
