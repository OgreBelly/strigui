(ns strigui.button
  (:require [clojure2d.core :as c2d]
            [strigui.box :as b]
            [strigui.events :as e]
            [strigui.widget :as wdg]))

(defrecord Button [name value coordinates args]
  wdg/Widget
  (coord [this] (:coordinates this)) ;; could be a mapping if the record would look different
  (text [this] (:value this))
  (args [this] (:args this))
  (box-name [this] (:name this))
  (redraw [this canvas] (b/box-redraw this canvas))
  (draw [this canvas]
    (b/box-draw-border this canvas) 
    (b/box-draw canvas (:value this) (:args this))))

(extend-protocol b/Box
  Button
  (draw-hover [this canvas] (b/box-draw-hover this canvas))
  (draw-clicked [this canvas] 
    (let [[x y w h] (:coordinates this)] 
      (b/box-draw-border canvas :blue 2 x y w h)
                                  this)))
(extend-protocol e/Actions
  Button
  (clicked [this] (e/button-clicked this)))

(defn button
  "context - map consiting of clojure2d canvas and clojure2d window
   name - name of the button
   text - text displayed inside the button
   args - map of properties:
      x - x coordinate of top left corner
      y - y coordinate of top left corner
      color - vector consisting of [background-color font-color]
      min-width - the minimum width"
  [context name text args]
  (let [canvas (:canvas context)
        arg [canvas text args]
        coord (apply b/box-coord arg)]
    (Button. name text coord args)))

(defmethod widget-event [strigui.button.Button :mouse-moved] 
  [widget _ canvas]
  (draw-hover widget canvas))

(defmethod widget-event [strigui.button.Button :mouse-clicked]
  [widget _ canvas]
  (clicked widget)
  (draw-clicked widget canvas))