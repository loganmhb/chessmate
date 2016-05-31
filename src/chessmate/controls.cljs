(ns chessmate.controls
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))


(defui ControlPanel
  static om/IQuery
  (query [this] [:engine/agent :player/side])
  Object
  (render [this]
    (let [props (om/props this)]
      (dom/div #js {:style #js {:border "2px solid"
                                :float "left"
                                :margin "10px"
                                :padding "10px"}}
               "Player side:  "
               (dom/select #js {:defaultValue "White"}
                           (dom/option #js {:value "white"} "White")
                           (dom/option #js {:value "black"} "Black"))
               (dom/button #js {} "New Game")))))

(def control-panel (om/factory ControlPanel))
