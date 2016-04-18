(defproject om-chessboard "0.1.0-SNAPSHOT"
  :description "A reactive chessboard implemented with Om Next"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha22"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]]
  :source-paths ["src/clj"]
  :test-paths ["spec/clj"])
