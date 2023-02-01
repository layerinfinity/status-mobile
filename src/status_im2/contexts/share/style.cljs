(ns status-im2.contexts.share.style
  (:require [quo2.foundations.colors :as colors]))

(def screen-padding 20)
(def qr-container-radius 16)
(def emoji-hash-container-radius 16)

(def header-button
  {:margin-bottom 12
   :margin-left   screen-padding})

(def header-heading
  {:padding-horizontal screen-padding
   :padding-vertical   12
   :color              colors/white})

(defn screen-container
  [window-width top bottom]
  {:flex           1
   :width          window-width
   :padding-top    (if (pos? top) (+ top 12) 12)
   :padding-bottom bottom})

(def tabs
  {:padding-left screen-padding})

(defn qr-code-container [window-width & wallet-tab]
  {:padding-vertical 20
   :border-radius qr-container-radius
   :margin-top (if wallet-tab 8 20)
   :margin-bottom 4
   :margin-horizontal (* window-width 0.053)
   :width :89.3%
   :background-color colors/white-opa-5
   :flex-direction :column
   :justify-content :center
   :align-items :center})

(def profile-address-column
  {:align-self :flex-start})


(def profile-address-label
  {:align-self :flex-start
   :padding-horizontal 20
   :padding-top 10})

(def copyable-text-container-style
  {:background-color :transparent
   :width :100%})

(defn profile-address-content [max-width]
  {:color colors/white
   :align-self :flex-start
   :padding-horizontal 20
   :padding-top 2
   :font-weight :500
   :font-size 16
   :max-width max-width})

(def address-share-button-container
  {:padding 8
   :position :absolute
   :background-color colors/white-opa-5
   :border-radius 10
   :right 14
   :top 10})

(def emoji-hash-container
  {:border-radius emoji-hash-container-radius
   :padding-vertical :1%
   :margin-horizontal :5.3%
   :width :89.3%
   :background-color colors/white-opa-5
   :flex-direction :column
   :justify-content :center
   :align-items :center})

(def profile-address-container
  {:flex-direction :row
   :margin-top 6
   :width :98%})

(def emoji-hash-label
  {:color colors/white-opa-40
   :align-self :flex-start
   :padding-horizontal 20})

(defn set-custom-width [section-width]
  {:width section-width})

(defn emoji-hash-content [max-width]
  {:color colors/white
   :align-self :flex-start
   :padding-horizontal 20
   :padding-top 4
   :padding-bottom 8
   :font-weight :500
   :font-size 15
   :max-width max-width})

(def emoji-share-button-container
  {:padding 8
   :position :absolute
   :background-color colors/white-opa-5
   :border-radius 10
   :right 14
   :top 2})
