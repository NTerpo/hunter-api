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

(comment 
  (def conn (connect mongo-options))
  (get-db conn (mongo-options :db)))

;;; Utility Functions

(defn with-oid
  [ds]
  (assoc ds :_id (util/object-id)))

(defn create-now
  [ds]
  (assoc ds :created (time/now)))

(defn modify-now
  [ds]
  (assoc ds :modified (time/now)))

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
                   (presence-of :created)
                   (presence-of :modified)
                   (presence-of :date)) dataset)
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
