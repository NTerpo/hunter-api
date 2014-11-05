(ns hunter-api.data
  (:require [monger.collection :as collection]
            [monger.core :refer [connect get-db]]
            [monger.result :refer [ok?]]
            [monger.util :as util]
            [monger.joda-time]
            [monger.json]
            [monger.conversion :refer [from-db-object]]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [validateur.validation :refer [presence-of valid? validation-set]]
            [slingshot.slingshot :refer [throw+]])
  (:import org.bson.types.ObjectId))

;;
;;; Database connection details
;;

(def mongo-options
  {:host "localhost"
   :port 27017
   :db "hunter-datasets"
   :datasets-collection "ds"})

;;
;;; Utility Functions
;;

(defn with-oid
  [ds]
  (assoc ds :_id (util/object-id)))

(defn create-now
  [ds]
  (assoc ds :created-ds (time/now)))

(defn modify-now
  [ds]
  (assoc ds :modified-ds (time/now)))

;;
;;; Validation Functions
;;

(defmulti validate* (fn [val val-type] val-type))

(defmethod validate* ::ObjectID
  [id _]
  (if-not (and
           (not (nil? id))
           (string? id)
           (re-matches #"[0-9a-f]{24}" id))
    (throw+ {:type ::invalid} "Invalid ID")))

(defmethod validate* ::Dataset
  [dataset _]
  (if-not (valid? (validation-set
                   (presence-of :_id)
                   (presence-of :created-ds)
                   (presence-of :modified-ds)
                   (presence-of :title)
                   (presence-of :description)
                   (presence-of :publisher)
                   (presence-of :temporal)
                   (presence-of :spatial)
                   (presence-of :created)
                   (presence-of :updated)
                   (presence-of :tags)
                   (presence-of :uri)) dataset)
    (throw+ {:type ::invalid} "Invalid Dataset")))

(defn validate
  "Execute a sequence of validation tests"
  [& tests]
  (doseq [test tests] (apply validate* test)))

;;
;; Date formatter
;;

(def multi-parser (f/formatter (time/default-time-zone)  "YYYY-MM-dd" "YYYY/MM/dd" "YYYY-MM-dd'T'HH:mm:ss.SSSSSS"))

(defn date->valid-date
  "transforms date 'YYYY-MM-dd', 'YYYY/MM/dd' ~> #<DateTime YYYY-MM-ddT00:00:00.000+02:00>"
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
;; Database Access Functions
;;

(defn create-dataset
  "Insert a dataset into the database"
  [ds db]
  (let [new-ds (-> ds
                   with-oid
                   modify-now
                   create-now
                   normalize-dates)
        conn (connect mongo-options)
        db (get-db conn db)]
    {:pre [(validate [new-ds ::Dataset])
           (or (ok? (collection/insert db
                                       (mongo-options :datasets-collection)
                                       new-ds))
               (throw+ {:type ::failed} "Create Failed"))]}
    new-ds))

(defn get-dataset
  "Fetch a dataset by ID"
  [id db]
  (validate [id ::ObjectID])
  (let [conn (connect mongo-options)
        db (get-db conn db)
        ds (collection/find-map-by-id db (mongo-options :datasets-collection) (ObjectId. id))]
    {:pre [(or (not (nil? ds))
               (throw+ {:type ::not-found} (str id " not found")))]}
    ds))

(defn delete-dataset
  "Delete a dataset by ID"
  [id db]
  (validate [id ::ObjectID])
  (let [conn (connect mongo-options)
        ds (get-dataset id db)
        db (get-db conn db)]
    {:pre [(or (ok? (collection/remove-by-id db (mongo-options :datasets-collection) (ObjectId. id)))
               (throw+ {:type ::failed} "Detete Failed"))]}
    ds))

(defn find-dataset
  "Fetch a dataset by tags"
  [args db]
  (let [conn (connect mongo-options)
        db2 (get-db conn db)
        result (collection/find-maps db2 (mongo-options :datasets-collection) args)]
    {:pre [(or (not (empty? result))
               (throw+ {:type ::not-found} "Not Found"))]}
    (get-dataset (.toString ((last (sort-by :updated result)) :_id)) db)))
