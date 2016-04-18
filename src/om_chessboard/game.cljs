(ns om-chessboard.game
  (:require [om.next :as om]))

(defn make-move
  "Using chess.js, determine if a move is legal. If it is, return the new
   board position. If not, return nil."
  [fen move]
  (let [game (js/Chess.)
        result (doto game
                 (.load fen)
                 (.move (clj->js move)))]
    ;; Result is a move object, not a game position, so we have to get
    ;; the position. If result is null, the move was illegal and we shouldn't
    ;; update the game state.
    (when result (.fen game))))
