(ns hunter-api.config
  (:require [monger.core :refer [connect-via-uri]]))

(def config
  (let [uri (System/genenv "MONGOHQ_URL")
        {:keys [conn db]} (connect-via-uri "mongodb://127.0.0.1/monger-test4")]
    {:conn conn
     :db db
     :db-name "hunter-datasets"}))
