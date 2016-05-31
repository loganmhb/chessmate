(ns chessmate.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [clojure.string :as str]
            [cljs.core.async :as async]
            [chessmate.engine :as engine]
            [chessmate.ratings :as ratings]
            [chessmate.game :as game]
            [chessmate.board :as board]
            [chessmate.controls :as controls]
            [clojure.set :as set])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(enable-console-print!)


(def start-pos "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")


(def app-state (atom {:chessboard/position start-pos
                      :player/side :white
                      :engine/evaluations {}
                      :engine/agent {:s 0.165
                                     :c 0.4}}))


(def engine (engine/stockfish))

(declare reconciler)


(defmulti handle-uci first)


(defmethod handle-uci :default
  [data])


(defmethod handle-uci "bestmove"
  [_]
  ;; bestmove indicates that the engine is done searching and it is
  ;; time to make a move, but we ignore the actual best move in favor
  ;; of a random choice based on the model agent's simulated ability.
  (om/transact! reconciler `[(engine/make-move) :chessboard/position]))


(defn parse-score [data]
  (let [[units score & more] data]
    [(if (= units "cp")
       (js/parseInt score)
       (* (js/parseInt score) 100))   ; other option is "mate" in y moves
     (if (#{"upperbound" "lowerbound"} (first more))
       (drop 1 more)
       more)]))


(defn parse-info [data]
  (loop [parsed-data {}
         remaining-data data]
    (if (empty? remaining-data)
      parsed-data
      (let [k (first remaining-data)]
        (condp = k
          ;; 'pv' comes at the end followed by a list of moves
          "pv" (assoc parsed-data "pv" (rest remaining-data))
          ;; 'score' is followed by units ('cp') then the score
          "score" (let [[score rest-data] (parse-score (rest remaining-data))]
                    (recur (assoc parsed-data "score" score)
                           rest-data))
          ;; default
          (recur (assoc parsed-data k (first (rest remaining-data)))
                 (drop 2 remaining-data)))))))


(defmethod handle-uci "info"
  [[_ & data]]
  ;; TODO: Currently the engine records the evaluations for its *own* move options;
  ;; it needs to record them for the *player's* move options.
  (let [move-info (parse-info data)]
    (when (get move-info "score")
      (om/transact! reconciler
                    `[(engine/update-evaluation
                       {:position ~(:chessboard/position @app-state)
                        :evaluation ~move-info})]))))


(defn listen-for-engine-moves! []
  (go-loop []
    (let [uci-message (str/split (async/<! (:output-chan engine)) #" ")]
      (handle-uci uci-message)
      (recur))))


(defn read [{:keys [state] :as env} key params]
  {:value (get @state key :not-found)})


(defmulti mutate om/dispatch)


(defmethod mutate 'player/attempt-move
  [{:keys [state] :as env} key params]
  (let [position (:chessboard/position @state)
        player-side (:player/side @state)]
    ;; Only allow a move from dragging a piece if it's the player's turn
    ;; and the move is legal
    (if-let [new-pos (and (= (game/side-to-move position) player-side)
                          (game/make-move position params))]
      {:value [:chessboard/position]
       :action #(do
                  (engine/find-best-move! engine new-pos)
                  (swap! state assoc :chessboard/position new-pos))})))


(defmethod mutate 'engine/update-evaluation
  [{:keys [state] :as env} key {:keys [evaluation position] :as params}]
  (let [move (first (get evaluation "pv"))
        _ (when (nil? move)
            (println "NIL MOVE!!!!")
            (println evaluation))
        score (get evaluation "score")]
    {:value [:engine/evaluations]
     :action #(swap! state assoc-in [:engine/evaluations position move] score)}))


(defmethod mutate 'engine/make-move
  ;; Should only be called when we get a "bestmove" from Stockfish
  [{:keys [state] :as env} key params]
  {:value [:chessboard/position]
   :action #(let [position (:chessboard/position @state)
                  evaluations (map (fn [[move evaluation]]
                                     {:move move
                                      :evaluation evaluation})
                                   (get-in @state [:engine/evaluations
                                                   position]))
                  move (ratings/pick-move-for-agent (:engine/agent @state)
                                                    evaluations)]
              (swap! state assoc :chessboard/position
                     (game/make-move position {:from (subs move 0 2)
                                               :to (subs move 2 4)})))})

(defmethod mutate 'controls/new-game
  [{:keys [state] :as env} key params]
  (let [player-side (:player/side params)]
    {:value [:chessboard/position :engine/evaluations]
     :action #(do (swap! state merge {:engine/evaluations {}
                                      :chessboard/position start-pos
                                      :player/side player-side})
                  (when (= player-side :black)
                    (engine/find-best-move! engine start-pos)))}))


(def parser (om/parser {:read read :mutate mutate}))


(def reconciler (om/reconciler {:state app-state :parser parser}))


(defui Root
  static om/IQuery
  (query [this] `[:chessboard/position :engine/agent :player/side])
  Object
  (render [this]
    (let [props (om/props this)]
      (dom/div nil
               (board/chessboard {:chessboard/position (:chessboard/position props)
                                  :player/side (:player/side props)})
               (controls/control-panel props)))))

(om/add-root! reconciler Root (gdom/getElement "app"))

(listen-for-engine-moves!)

(engine/setup-engine! engine)
