(ns strigui.playground
  (:require [strigui.core :as gui]))

(defn main []
  (gui/window! 600 600 "Strigui")
  (gui/label "welcome" "Welcome to Strigui" {:x 190 :y 100
                                             :color [:green]
                                             :font-size 20 :font-style [:bold]
                                             :can-move? true})
  (gui/button "click" "Click me" {:x 400 :y 200 :z 10 :color [:white :black]})
  (gui/input "input" "" {:x 100 :y 150 :color [:white :red] :min-width 420 :focus true})
  (gui/update! "click" [:events :mouse-clicked] (fn [wdg]
                                                  (gui/close-window))))