(ns hunter-api.util
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [monger.util :as util]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [cheshire.core :refer :all]))

(defn query-string->hashmap
  "Transform a string ~ 'foo=bar&baz=bah' in {\"foo\" \"bar\", \"baz\" \"bah\"}"
  [s]
  (into {}
        (map #(str/split % #"=")
             (str/split s #"&"))))

(defn query-string->string
  "Transform a string ~ 'q=foo+bar' in 'foo bar'"
  [s]
  (-> s
      (str/split #"q=")
      last
      (str/replace "+" " ")))

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

(defn ^:no-doc check-for-timezone-transition
  [s]
  (if (re-find #"2015-03-29T02" s) "2015-03-29T03:00:00.000000" s))

(def ^:no-doc multi-parser (f/formatter (time/default-time-zone)  "YYYY-MM-dd" "YYYY/MM/dd" "YYYY-MM-dd'T'HH:mm:ss.SSSSSS" "YYYY-MM-dd'T'HH:mm:ss"))

(defn date->valid-date
  "transforms date

* 'YYYY-MM-dd', 
* 'YYYY/MM/dd', 
* 'YYYY-MM-dd'T'HH:mm:ss.SSSSSS'
* 'YYYY-MM-dd'T'HH:mm:ss'

~> #<DateTime YYYY-MM-ddT00:00:00.000+02:00>"
  {:doc/format :markdown}
  [s]
  (when s
    (let [date (check-for-timezone-transition s)]
      (f/parse multi-parser date))))

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

;;
;; Elastic Search Query & Responses
;;

(defn destructure-query-string
  "Given a query returns a vector with the query itself,
  the penultimate word and the last word. Used to check
  if it's the geo or temporal query"
  [q]
  (let [q (str/trim q)
        a (str/split q #" ")
        t (last a)
        s (if (> 1 (count a)) (last (butlast a)) t)]
    [q s t]))

(defn clean-hit
  "Given an ES hit, returns the dataset + the id"
  [m]
  (let [ds (:_source m)
        id (:_id m)]
    (assoc ds :_id id)))

(defn clean-hits
  "Clean a collection of hits"
  [coll]
  (vec (map clean-hit coll)))
