(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!
 {:figwheel-options {}
  :build-ids ["dev"]
  :all-builds
  [{:id "dev"
    :figwheel true
    :source-paths ["src"]
    :compiler {:main 'om-chessboard.core
               :asset-path "compiledjs"
               :output-to "resources/public/compiledjs/main.js"
               :output-dir "resources/public/compiledjs"
               :verbose true}}]})

(ra/cljs-repl)
