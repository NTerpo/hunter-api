(ns hunter-api.config
  (:require [monger.core :refer [connect-via-uri]]))

(def config
  (let [uri (System/genenv "MONGOHQ_URL")
        {:keys [conn db]} (connect-via-uri uri)]
    {:conn conn
     :db db
     :db-name "hunter-datasets"}))
