(ns hunter-api.util
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [monger.util :as util]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [cheshire.core :refer :all]))

(defn query-string->hashmap
  "Transform a string ~ 'foo=bar&baz=bah' in {\"foo\" \"bar\", \"baz\" \"bah\"}"
  [query-string]
  (into {}
        (map #(str/split % #"=")
             (str/split query-string #"&"))))

(defn ^:no-doc with-oid
  [ds]
  (assoc ds :_id (util/object-id)))

(defn ^:no-doc create-now
  [ds]
  (assoc ds :created-ds (time/now)))

(defn ^:no-doc modify-now
  [ds]
  (assoc ds :modified-ds (time/now)))


;;
;; Date formatter
;;

(def ^:no-doc multi-parser (f/formatter (time/default-time-zone)  "YYYY-MM-dd" "YYYY/MM/dd" "YYYY-MM-dd'T'HH:mm:ss.SSSSSS" "YYYY-MM-dd'T'HH:mm:ss"))

(defn date->valid-date
  "transforms date

* 'YYYY-MM-dd', 
* 'YYYY/MM/dd', 
* 'YYYY-MM-dd'T'HH:mm:ss.SSSSSS'
* 'YYYY-MM-dd'T'HH:mm:ss'

~> #<DateTime YYYY-MM-ddT00:00:00.000+02:00>"
  {:doc/format :markdown}
  [date]
  (if (nil? date)
    nil
    (f/parse multi-parser date)))

(defn normalize-dates
  "parses a dataset and apply transformation to get normalized dates"
  [ds]
  (if (and (contains? ds :created)
           (contains? ds :updated))
    (-> ds
        (conj {:created (date->valid-date (ds :created))})
        (conj {:updated (date->valid-date (ds :updated))}))
    ds))

;;
;; Elastic Search cleaning
;;

(defn stringify-date
  "Returns a string from a Joda Datetime"
  [date]
  (when date
    (.toString date "yyyy-MM-dd'T'HH:mm:ss:SSSSSS")))

(defn dataset->indexable-ds
  "Cleans the dates: Joda Datetime->string and removes the _id"
  [ds]
  (-> ds
      (update-in [:created-ds] stringify-date)
      (update-in [:modified-ds] stringify-date)
      (update-in [:created] stringify-date)
      (update-in [:updated] stringify-date)
      (dissoc :_id)))
