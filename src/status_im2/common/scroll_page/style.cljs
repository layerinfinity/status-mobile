(ns status-im2.common.scroll-page.style
  (:require [quo2.foundations.colors :as colors]
            [react-native.platform :as platform]
            [react-native.reanimated :as reanimated]))

(defn image-slider
  [size]
  {:top     (if platform/ios? 0 -64)
   :height  size
   :width   size
   :z-index 4
   :flex    1})

(defn blur-slider
  [animation top]
  (reanimated/apply-animations-to-style
   {:transform [{:translateY animation}]}
   {:z-index  5
    :top      top
    :position :absolute
    :height   (if platform/ios? 164 188)
    :right    0
    :left     0
    :flex     1}))

(defn scroll-view-container
  [border-radius]
  {:flex          1
   :position      :absolute
   :left          0
   :right         0
   :top           -48
   :overflow      :scroll
   :border-radius border-radius})

(defn sticky-header-title
  [animation]
  (reanimated/apply-animations-to-style
   {:opacity animation}
   {:position       :absolute
    :flex-direction :row
    :left           64
    :top            16}))

(def sticky-header-image
  {:border-radius 12
   :border-width  0
   :border-color  :transparent
   :width         24
   :height        24
   :margin-right  8})

(defn display-picture-container
  [animation]
  (reanimated/apply-animations-to-style
   {:transform [{:scale animation}]}
   {:border-radius    40
    :border-width     1
    :border-color     colors/white
    :position         :absolute
    :top              -40
    :left             17
    :padding          2
    :background-color (colors/theme-colors
                       colors/white
                       colors/neutral-90)}))

(def display-picture
  {:border-radius 50
   :border-width  0
   :border-color  :transparent
   :width         80
   :height        80})
