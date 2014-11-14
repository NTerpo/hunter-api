(ns hunter-api.config)

(comment (defmacro with-connection
           [& body]
           `(let [conn# (connect mongo-options)
                  db# ]
              ~@body)))
