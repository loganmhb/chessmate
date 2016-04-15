(ns om-chessboard.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(defui Chessboard
  Object
  (render [this]
    (dom/p nil "Hello world!")))

(def chessboard (om/factory Chessboard))

(js/ReactDOM.render (chessboard) (gdom/getElement "app"))
