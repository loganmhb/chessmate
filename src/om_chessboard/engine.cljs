(ns om-chessboard.engine
  (:require [cljs.core.async :as async]))

(defn stockfish
  "Starts a Web Worker running Stockfish.js."
  []
  (let [stockfish (js/Worker. "js/stockfish.js")
        input-chan (async/chan 100)
        output-chan (async/chan 100)]
    (async/go-loop (.postMessage stockfish (async/<! input-chan)))
    (.onmessage stockfish (fn [e] (async/go (async/>! e output-chan))))))

;;; interfact primitives

(defn post-message [msg]
  (.postMessage stockfish msg))

(defn on-message [handler]
  (.onmessage stockfish handler))

(def input-chan (async/chan))

(def output-chan (async/chan))

