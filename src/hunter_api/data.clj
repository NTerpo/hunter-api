(ns hunter-api.data
  (:use [clojure.java.io])
  (:require [monger.collection :as collection]
            [monger.core :refer [connect get-db connect-via-uri]]
            [monger.result :refer [ok?]]
            [monger.operators :refer [$set]]
            [monger.joda-time]
            [monger.json]
            [monger.conversion :refer [from-db-object]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as res]
            [clojurewerkz.elastisch.query :as q]
            [hunter-api.util :refer [with-oid create-now modify-now normalize-dates dataset->indexable-ds destructure-query-string clean-hit clean-hits]]
            [validateur.validation :refer [presence-of valid? validation-set]]
            [slingshot.slingshot :refer [throw+]])
  (:import org.bson.types.ObjectId))

;;
;; Config
;;

(def config ;; used in development
  {:conn (connect {:host "localhost" :port 27017})
   :db (get-db (connect {:host "localhost" :port 27017}) "hdt")
   :db-name "hdt"})

(def index-name "hdt")

;;
;; Validation Functions
;;

(defmulti ^:no-doc validate*
  (fn [val val-type] val-type))

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

(defmethod validate* ::Ds
  [dataset _]
  (if-not (valid? (validation-set
                   (presence-of :title)
                   (presence-of :description)
                   (presence-of :publisher)
                   (presence-of :temporal)
                   (presence-of :spatial)
                   (presence-of :created)
                   (presence-of :updated)
                   (presence-of :tags)
                   (presence-of :uri)) dataset)
    (throw+ {:type ::invalid} "Invalid ds")))

(defn ^:no-doc validate
  "Execute a sequence of validation tests"
  [& tests]
  (doseq [test tests] (apply validate* test)))

;;
;; Mongo DB
;;

(defn create-dataset
  "Insert a dataset into the database"
  [ds & [alt-db]]
  (let [new-ds (-> ds
                   with-oid
                   modify-now
                   create-now
                   normalize-dates)
        conn (config :conn)
        db (if alt-db (get-db conn alt-db) (config :db))]
    {:pre [(validate [new-ds ::Dataset])
           (or (ok? (collection/insert db "ds" new-ds))
               (throw+ {:type ::failed} "Create Failed"))]}
    new-ds))

(defn get-dataset
  "Fetch a dataset by ID"
  [id & [alt-db]]
  (validate [id ::ObjectID])
  (let [conn (config :conn)
        db (if alt-db (get-db conn alt-db) (config :db))
        ds (collection/find-map-by-id db "ds" (ObjectId. id))]
    {:pre [(or (not (nil? ds))
               (throw+ {:type ::not-found} (str id " not found")))]}
    ds))

(defn delete-dataset
  "Delete a dataset by ID"
  [id & [alt-db]]
  (validate [id ::ObjectID])
  (let [conn (config :conn)
        db (if alt-db (get-db conn alt-db) (config :db))
        ds (get-dataset id (if alt-db
                             alt-db
                             (config :db-name)))]
    {:pre [(or (ok? (collection/remove-by-id db "ds" (ObjectId. id)))
               (throw+ {:type ::failed} "Detete Failed"))]}
    ds))

(defn update-dataset
  "Update or insert the dataset corresponding to the query"
  [args new-args & [alt-db]]
  (let [conn (config :conn)
        db (if alt-db (get-db conn alt-db) (config :db))
        ds-to-update (collection/find-maps db "ds" args)
        new-args (-> new-args modify-now normalize-dates)]
    {:pre [(or (>= 1 (count ds-to-update))
               (throw+ {:type ::invalid} "The given arguments are not sufficient to find only 0 or 1 dataset"))]}
    (collection/update db "ds" args {$set new-args} {:upsert true})))

(defn find-dataset
  "Returns the datasets corresponding to the query, sorted by :huntscore and then by updated date"
  [args & [alt-db]]
  (let [conn (config :conn)
        db (if alt-db (get-db conn alt-db) (config :db))
        result (collection/find-maps db "ds" args)]
    {:pre [(or (not (empty? result))
               (throw+ {:type ::not-found} "Not Found"))]}
    (map #(get-dataset (.toString (% :_id)) (if alt-db
                                              alt-db
                                              (config :db-name)))
         (sort-by :huntscore (sort-by :updated result)))))

;;
;; Elastic Search
;;

(defn index-dataset
  "Index a dataset to Elastic Search"
  [ds]
  {:pre [(nil? (validate [ds ::Ds]))]}
  (let [conn (esr/connect (System/getenv "SEARCHBOX_URL"))
        doc (if (contains? ds :created-ds)
              (dataset->indexable-ds ds)
              (dataset->indexable-ds
               (-> ds with-oid modify-now create-now normalize-dates)))]
    {:pre [(or (res/ok? (esd/create conn index-name "ds" doc))
               (throw+ {:type ::failed} "Indexation Failed"))]}))

(defn get-indexed-dataset
  "Fetch an indexed dataset by ID"
  [id]
  (let [conn (esr/connect (System/getenv "SEARCHBOX_URL"))]
    (esd/get conn index-name "ds" id)))

(defn delete-indexed-dataset
  "Delete an indexed dataset by ID"
  [id]
  (let [conn (esr/connect (System/getenv "SEARCHBOX_URL"))]
    (esd/delete conn index-name "ds" id)))

(defn search
  "Elastic Search Query.
  Given a string, returns a collection of hits"
  [s]
  (let [conn (esr/connect (System/getenv "SEARCHBOX_URL"))
        [q t s] (destructure-query-string s)
        res (esd/search conn index-name "ds"
                        :query
                        {:function_score
                         {:query
                          {:bool
                           {:should
                            [{:term {:title {:value q :boost 4.0}}}
                             {:term {:tags {:value q :boost 2.5}}}
                             {:query_string
                              {:query q
                               :default_field :description
                               :boost 1.1}}
                             {:term {:spatial {:value t :boost 4.0}}}
                             {:term {:spatial {:value s :boost 4.0}}}
                             {:term {:temporal {:value t :boost 4.0}}}
                             {:term {:temporal {:value s :boost 4.0}}}]
                            :minimum_number_should_match 1}}
                          :boost_mode "replace"
                          :script_score
                          {:script "search_ponderation *_score + hunt_ponderation * doc['huntscore'].value"
                           :params
                           {:search_ponderation 5
                            :hunt_ponderation 1}}}}
                        :size 50)
        n (res/total-hits res)
        hits (res/hits-from res)]
    {:pre [(or (not (empty? hits))
               (throw+ {:type ::not-found} "Not Found"))]}
    hits))
