(ns hunter-api.config
  (:require [monger.core :refer [connect get-db]]))

(def config {:conn (connect {:host "localhost" :port 27017})
             :db (get-db (connect {:host "localhost" :port 27017})
                         "hunter-datasets")
             :db-name "hunter-datasets"})
