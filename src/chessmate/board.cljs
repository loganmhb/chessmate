(ns chessmate.board
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [clojure.string :as str])  )

(defn image-for-piece [component fen-char size file rank]
  (if (= fen-char ".")
    nil
    (let [color (if (= (.toUpperCase fen-char) fen-char) "w" "b")
          kind (.toUpperCase fen-char)]
      (dom/img #js {:src (str "img/chesspieces/wikipedia/" color kind ".png")
                    :draggable true
                    :onDragStart (fn []
                                   (om/update-state! component merge
                                                     {:moving-from (str file rank)}))
                    :style #js {:height size
                                :width size}}))))

(defn square [component size color rank file piece]
  (let [size-prop (str size "px")
        color-prop ({:black "#b58863"
                     :white "#f0d9b5"} color)
        key (str file rank)]
    (dom/div #js {:key key
                  :id key
                  :className (str "square-" key)
                  :onDragOver
                  (fn [e]
                    (.preventDefault e))
                  :onDrop
                  (fn [e]
                    (.preventDefault e)
                    (let [{:keys [moving-from]} (om/get-state component)
                          moving-to (str file rank)]
                      (om/transact! component
                                    `[(player/attempt-move {:from ~moving-from
                                                            :to ~moving-to})])))
                  :style #js {:background color-prop
                              :width size-prop
                              :height size-prop
                              :display "inline"
                              :float "left"}}
             (image-for-piece component piece size file rank))))

(defn expand-numbers [fen-str]
  (str/replace fen-str #"([1-8])" #(apply str (repeat (js/parseInt (first %)) "."))))

(defn fen->rows [fen-position]
  (-> fen-position
      (str/split #" ")
      first
      expand-numbers
      (str/split "/")))

(defn row [component rank pattern width pieces]
  (let [square-size (quot width 8)]
    (dom/div #js {:style #js {:height square-size
                              :width width}
                  :key rank}
             (map square
                  (repeat component)
                  (repeat square-size)
                  (cycle pattern)
                  (repeat rank)
                  "abcdefgh"
                  pieces))))

(defui Chessboard
  static om/IQuery
  (query [this] [:chessboard/position])
  Object
  (render [this]
    (let [props (om/props this)
          width 400] ;; FIXME: responsive width
      (println props)
      (dom/div #js {:style #js {:border "2px solid"
                                :width width
                                :height width}}
               (take 8 (map row
                            (repeat this)
                            (reverse (range 1 9))
                            (cycle [[:white :black] [:black :white]])
                            (repeat 400)
                            (fen->rows (:chessboard/position props))))))))


(def chessboard (om/factory Chessboard))
