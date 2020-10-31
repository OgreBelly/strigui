(ns strigui.box
  (:require [clojure2d.core :as c2d]
            [clojure.set :as s]
            [strigui.events :as e]
            [strigui.window :as wnd]
            [strigui.widget :as wdg]))

(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)

(defprotocol Box 
  "collection of functions around redrawing boxes, managing the border etc. ..."
  (draw-hover [this canvas] "draws the hover effect")
  (draw-clicked [this canvas] "draws the clicked effect"))

(def ^:private default-font-size 15)

(def boxes-clicked (atom #{}))

(def boxes-focused (atom #{}))

(defn box-coord 
  "Computes the full box coordinates.
  Returns the vector [x y border-width border-heigth]"
  [canvas text {:keys [^long x ^long y ^long min-width]}]
  (let [text-box (c2d/with-canvas-> canvas
                  (c2d/text-bounding-box text))
      text-width (nth text-box 2)
      text-heigth  (nth text-box 3)
      btn-w (* text-width 1.8)
      border-width (if (and (number? min-width) (< btn-w min-width)) min-width btn-w)
      border-heigth (* text-heigth 1.8)]
      [x y border-width border-heigth]))

(defn box-draw-text 
  "Draws the text of the box"
  [canvas text {:keys [^long x ^long y color ^long min-width align font-style font-size]}]
  (let [style (if (empty? font-style) :bold (first font-style))
        size (if (number? font-size) font-size default-font-size)
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y :min-width min-width})
        [_ text-y text-width _] (c2d/with-canvas-> canvas
                          (c2d/set-font-attributes size style)
                          (c2d/text-bounding-box text))
        background-color (if (> (count color) 0) (first color) :grey)
        foreground-color (if (> (count color) 1) (nth color 1) :black)
        x-offset (if (and (number? min-width) (= min-width border-width))
                   (/ (- border-width text-width) 2.0)
                   (* border-width 0.12))]
      (c2d/with-canvas-> canvas
        (c2d/set-color background-color)
        (c2d/rect x y border-width border-heigth)
        (c2d/set-font-attributes size style)
        (c2d/set-color foreground-color)
        (c2d/text text (+ x x-offset) (- y (* text-y 1.5))))))

(defn box-draw
  "canvas - clojure2d canvas
  text - text displayed inside the input
  x - x coordinate of top left corner
  y - y coordinate of top left corner
  color - vector consisting of [background-color font-color]
  min-width - the minimum width"
  ([args] (apply box-draw args))
  ([canvas text args]
  (let [{:keys [^long x ^long y color ^long min-width]} args
        [_ _ border-width border-heigth] (box-coord canvas text {:x x :y y :min-width min-width})
        background-color (if (> (count color) 0) (first color) :grey)]
    (c2d/with-canvas-> canvas
      (c2d/set-color background-color)
      (c2d/rect x y border-width border-heigth))
    (box-draw-text canvas text args)
    [x y border-width border-heigth])))

(defn box-border 
  ([canvas color strength x y w h] 
    (box-border canvas color strength x y w h true))
  ([canvas color strength x y w h no-fill]
  (when (> strength 0)
      (c2d/with-canvas-> canvas
        (c2d/set-color color)
        (c2d/rect (- x strength) (- y strength) (+ w (* 2 strength)) (+ h (* 2 strength)) no-fill))
      (box-border canvas color (- strength 1) x y w h no-fill))))

(defn box-draw-border 
  ([^strigui.box.Box box canvas] (box-draw-border box canvas :black 1))
  ([^strigui.box.Box box canvas color] (box-draw-border box canvas color 1))
  ([^strigui.box.Box box canvas color strength] (box-draw-border box canvas color strength false))
  ([^strigui.box.Box box canvas color strength fill]
  (let [[x y w h] (wdg/coord box)]
    (box-border canvas color strength x y w h (not fill)))))

(defn box-draw-hover 
  [^strigui.box.Box box canvas] 
  (box-draw-border box canvas :black 2)
  box)

(defn box-redraw 
  [^strigui.box.Box box canvas] 
  (let [coord (wdg/coord box)]
    (when (not-empty coord)
    (box-draw-border box canvas :white 2)
      (box-draw-border box canvas :black 1)
      (wdg/draw box canvas)
      box)))

(defn box-remove-drawn 
  [^strigui.box.Box box canvas]
  (box-draw-border box canvas :white 1 true))

(defn swap-focused!
  [box]
  (let [focus @boxes-focused
        new (if (contains? focus box)
                (s/difference focus #{box})
                (s/union focus #{box}))] 
    (reset! boxes-focused new)))

(defn focused? 
  [box]
  (contains? @boxes-focused box))

(defmethod wdg/widget-global-event :mouse-released 
  [_ canvas & args]
  (map #(draw-hover %1 canvas) @boxes-clicked)
  (reset! boxes-clicked  #{}))

(defmethod wdg/widget-global-event :mouse-pressed-on-empty-space
  [_ canvas & args]
  (println "mouse pressed on empty space")
  (reset! boxes-focused #{}))

(defmethod wdg/widget-global-event :key-pressed
  [_ canvas & args]
  (let [char (first args)
        code (nth args 1)
        new-focused-inputs (doall (map #(e/key-pressed %1 char code) @boxes-focused))]
    (when (not-empty new-focused-inputs)
      (println (str "key pressed: " char))
      (doall (map #(wdg/unregister canvas %1) @boxes-focused))
      (println new-focused-inputs)
      (doall (map #(wdg/register canvas %1) new-focused-inputs))
      (doall (map #(box-draw-text canvas (wdg/value %1) (wdg/args %1)) new-focused-inputs))
      (reset! boxes-focused (set new-focused-inputs)))))