(ns chessmate.engine
  (:require [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn stockfish
  "Starts a Web Worker running Stockfish.js, posting messages from the
  provided channel. Returns a channel where it will deliver messages
  from Stockfish."
  []
  (let [stockfish (js/Worker. "js/stockfish.js")
        input-chan (async/chan 100)
        output-chan (async/chan 100)]
    (set! (.-onmessage stockfish)
          (fn [e]
            (go (async/>! output-chan (.-data e)))))
    (go-loop []
      (.postMessage stockfish (async/<! input-chan))
      (recur))
    {:output-chan output-chan
     :input-chan input-chan}))

(defn send! [engine msg]
  (go (async/>! (:input-chan engine) msg)))

(defn setup-engine! [engine]
  (send! engine "setoption name MultiPV value 50")
  (send! engine "isready"))

(defn find-best-move! [engine position]
  (send! engine (str "position fen " position))
  (send! engine "go depth 10"))
