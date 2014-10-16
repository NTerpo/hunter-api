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
   :db "datasets"
   :datasets-collection "ds"})

(def conn (connect mongo-options))
(get-db conn (mongo-options :db)) ;; to be tested!

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


;;; Database Access Functions

