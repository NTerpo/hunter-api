(defproject hunter-api "0.1.0"
  :description "REST API to handle open datasets meta-data"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.9"]
                 [ring-middleware-format "0.4.0"]
                 [ring/ring-json "0.3.1"]
                 [com.taoensso/timbre "3.1.6"]
                 [com.novemberain/monger "2.0.0"]
                 [com.novemberain/validateur "2.3.1"]
                 [slingshot "0.10.3"]
                 [clj-time "0.6.0"]
                 [org.clojure/data.json "0.2.5"]]
  :main hunter-api.handler
  :uberjar-name "hunter-api.jar"
  :plugins [[lein-ring "0.8.12"]
            [codox "0.8.10"]]
  :ring {:handler hunter-api.handler/app}
  :profiles {:dev {:resource-paths ["resource-dev"]
                   :dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [clj-http "1.0.1"]]}
             :prod {:resource-paths ["resource-prod"]}
             :uberjar {:aot :all}})
