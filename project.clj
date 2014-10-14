(defproject hunter-api "0.1.0-SNAPSHOT"
  :description "REST API  to handle open datasets meta-data"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.9"]
                 [ring-middleware-format "0.2.2"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler hunter-api.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
