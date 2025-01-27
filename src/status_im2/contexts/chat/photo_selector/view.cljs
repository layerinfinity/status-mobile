(ns status-im2.contexts.chat.photo-selector.view
  (:require
   [react-native.platform :as platform]
   [status-im2.constants :as constants]
   [utils.i18n :as i18n]
   [react-native.safe-area :as safe-area]
   [quo2.components.notifications.info-count :as info-count]
   [quo2.core :as quo]
   [quo2.foundations.colors :as colors]
   [react-native.core :as rn]
   [react-native.linear-gradient :as linear-gradient]
   [reagent.core :as reagent]
   [status-im2.contexts.chat.photo-selector.style :as style]
   [status-im.utils.core :as utils]
   [quo.react]
   [utils.re-frame :as rf]))

(defn on-press-confirm-selection
  [selected]
  (rf/dispatch [:chat.ui/clear-sending-images])
  (doseq [item @selected]
    (rf/dispatch [:chat.ui/camera-roll-pick item]))
  (reset! selected [])
  (rf/dispatch [:navigate-back]))

(defn bottom-gradient
  [selected-images insets selected]
  (when (or (seq @selected) (seq selected-images))
    [linear-gradient/linear-gradient
     {:colors [:black :transparent]
      :start  {:x 0 :y 1}
      :end    {:x 0 :y 0}
      :style  (style/gradient-container insets)}
     [quo/button
      {:style               {:align-self        :stretch
                             :margin-horizontal 20
                             :margin-top        12}
       :on-press            #(on-press-confirm-selection selected)
       :accessibility-label :confirm-selection}
      (i18n/label :t/confirm-selection)]]))

(defn clear-button
  [selected]
  (when (seq @selected)
    [rn/touchable-opacity
     {:on-press            #(reset! selected [])
      :style               (style/clear-container)
      :accessibility-label :clear}
     [quo/text {:weight :medium} (i18n/label :t/clear)]]))

(defn remove-selected
  [coll item]
  (vec (remove #(= (:uri item) (:uri %)) coll)))

(defn image
  [item index _ {:keys [window-width selected]}]
  [rn/touchable-opacity
   {:active-opacity      1
    :on-press            (fn []
                           (if (some #(= (:uri item) (:uri %)) @selected)
                             (swap! selected remove-selected item)
                             (if (>= (count @selected) constants/max-album-photos)
                               (rf/dispatch [:toasts/upsert
                                             {:id              :random-id
                                              :icon            :info
                                              :icon-color      colors/danger-50-opa-40
                                              :container-style {:top (when platform/ios? 20)}
                                              :text            (i18n/label :t/only-6-images)}])
                               (swap! selected conj item))))
    :accessibility-label (str "image-" index)}
   [rn/image
    {:source {:uri (:uri item)}
     :style  (style/image window-width index)}]
   (when (some #(= (:uri item) (:uri %)) @selected)
     [rn/view {:style (style/overlay window-width)}])
   (when (some #(= (:uri item) (:uri %)) @selected)
     [info-count/info-count
      {:style               style/image-count
       :accessibility-label (str "count-" index)}
      (inc (utils/first-index #(= (:uri item) (:uri %)) @selected))])])

(defn album-title
  [photos? selected-album]
  [rn/touchable-opacity
   {:style               (style/title-container)
    :active-opacity      1
    :accessibility-label :album-title
    :on-press            #(rf/dispatch (if photos?
                                         [:open-modal :album-selector]
                                         [:navigate-back]))}
   [quo/text {:weight :medium} selected-album]
   [rn/view {:style (style/chevron-container)}
    [quo/icon (if photos? :i/chevron-down :i/chevron-up)
     {:color (colors/theme-colors colors/neutral-100 colors/white)}]]])

(defn photo-selector
  []
  [:f>
   (fn []
     (let [selected-images (rf/sub [:chats/sending-image])
           selected-album  (or (rf/sub [:camera-roll/selected-album]) (i18n/label :t/recent))
           selected        (reagent/atom [])]
       (rn/use-effect
        (fn []
          (rf/dispatch [:chat.ui/camera-roll-get-photos 20 nil selected-album])
          (if selected-images
            (reset! selected (vec (vals selected-images)))
            (reset! selected [])))
        [selected-album])
       [safe-area/consumer
        (fn [insets]
          (let [window-width       (:width (rn/get-window))
                camera-roll-photos (rf/sub [:camera-roll/photos])
                end-cursor         (rf/sub [:camera-roll/end-cursor])
                loading?           (rf/sub [:camera-roll/loading-more])
                has-next-page?     (rf/sub [:camera-roll/has-next-page])]
            [rn/view {:style {:flex 1}}
             [rn/view
              {:style style/buttons-container}
              [album-title true selected-album]
              [clear-button selected]]
             [rn/flat-list
              {:key-fn                  identity
               :render-fn               image
               :render-data             {:window-width window-width :selected selected}
               :data                    camera-roll-photos
               :num-columns             3
               :content-container-style {:width          "100%"
                                         :padding-bottom (+ (:bottom insets) 100)
                                         :padding-top    80}
               :on-end-reached          #(rf/dispatch [:camera-roll/on-end-reached end-cursor
                                                       selected-album loading?
                                                       has-next-page?])}]
             [bottom-gradient selected-images insets selected]]))]))])


