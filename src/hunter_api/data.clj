(ns hunter-api.data
  (:require [monger.collection :as collection]
            [monger.core :refer [connect get-db]]
            [monger.result :refer [ok?]]
            [monger.util :as util]
            [monger.joda-time]
            [monger.json]
            [clj-time.core :as time]
            [validateur.validation :refer [presence-of valid? validation-set]]
            [slingshot.slingshot :refer [throw+]])
  (:import org.bson.types.ObjectId))

;;; Database connection details

(def mongo-options
  {:host "localhost"
   :port 27017
   :db "hunter-datasets"
   :datasets-collection "ds"})

;;; Utility Functions

(defn with-oid
  [ds]
  (assoc ds :_id (util/object-id)))

(defn create-now
  [ds]
  (assoc ds :created-ds (time/now)))

(defn modify-now
  [ds]
  (assoc ds :modified-ds (time/now)))

;;; Validation Functions

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
                   (presence-of :producer)
                   (presence-of :temporal-coverage)
                   (presence-of :spatial-coverage)
                   (presence-of :created)
                   (presence-of :last-modified)
                   (presence-of :tags)
                   (presence-of :uri)) dataset)
    (throw+ {:type ::invalid} "Invalid Dataset")))

(defn validate
  "Execute a sequence of validation tests"
  [& tests]
  (doseq [test tests] (apply validate* test)))

;;; Database Access Functions

(defn create-dataset
  "Insert a dataset into the database"
  ([ds]
      (let [new-ds (-> ds
                       with-oid
                       modify-now
                       create-now)
            conn (connect mongo-options)
            db (get-db conn (mongo-options :db))]
        (validate [new-ds ::Dataset])
        (if (ok? (collection/insert db
                                    (mongo-options :datasets-collection)
                                    new-ds))
          new-ds
          (throw+ {:type ::failed} "Create Failed"))))
  ([ds db]
     (let [new-ds (-> ds
                       with-oid
                       modify-now
                       create-now)
            conn (connect mongo-options)
            db (get-db conn db)]
        (validate [new-ds ::Dataset])
        (if (ok? (collection/insert db
                                    (mongo-options :datasets-collection)
                                    new-ds))
          new-ds
          (throw+ {:type ::failed} "Create Failed")))))

(defn get-dataset
  "Fetch a dataset by ID"
  ([id]
     (validate [id ::ObjectID])
     (let [conn (connect mongo-options)
           db (get-db conn (mongo-options :db))
           ds (collection/find-map-by-id db (mongo-options :datasets-collection) (ObjectId. id))]
       (if (nil? ds)
         (throw+ {:type ::not-found} (str id " not found"))
         ds)))
  ([id db]
     (validate [id ::ObjectID])
     (let [conn (connect mongo-options)
           db (get-db conn db)
           ds (collection/find-map-by-id db (mongo-options :datasets-collection) (ObjectId. id))]
       (if (nil? ds)
         (throw+ {:type ::not-found} (str id " not found"))
         ds))))

(defn find-dataset
  "Fetch a dataset by tags"
  [& args]
  (let [conn (connect mongo-options)
        db (get-db conn (mongo-options :db))
        result (collection/find db (mongo-options :datasets-collection) {})]))

(defn delete-dataset
  "Delete a dataset by ID"
  ([id]
      (validate [id ::ObjectID])
      (let [conn (connect mongo-options)
            db (get-db conn (mongo-options :db))
            ds (get-dataset id)]
        (if (ok? (collection/remove-by-id db (mongo-options :datasets-collection) (ObjectId. id)))
          ds
          (throw+ {:type ::failed} "Detete Failed"))))
  ([id db]
      (validate [id ::ObjectID])
      (let [conn (connect mongo-options)
            ds (get-dataset id db)
            db (get-db conn db)]
        (if (ok? (collection/remove-by-id db (mongo-options :datasets-collection) (ObjectId. id)))
          ds
          (throw+ {:type ::failed} "Detete Failed")))))
