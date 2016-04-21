(ns om-chessboard.game
  (:require [om.next :as om]
            [clojure.string :as str]))

(defn side-to-move [fen]
  (if (= "w" (second (str/split fen #" ")))
    :white
    :black))

(defn make-move
  "Using chess.js, determine if a move is legal. If it is, return the new
   board position. If not, return nil."
  [fen move]
  (println fen)
  (let [game  (js/Chess. fen)
        result (.move game (clj->js move))]
    (println "move result:" result)
    ;; Result is a move object, not a game position, so we have to get
    ;; the position. If result is null, the move was illegal and we shouldn't
    ;; update the game state.
    (when result (.fen game))))
