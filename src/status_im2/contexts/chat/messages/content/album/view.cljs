(ns status-im2.contexts.chat.messages.content.album.view
  (:require [quo2.core :as quo]
            [quo2.foundations.colors :as colors]
            [react-native.core :as rn]
            [react-native.fast-image :as fast-image]
            [status-im2.contexts.chat.messages.content.album.style :as style]
            [status-im2.constants :as constants]
            [utils.re-frame :as rf]
            [status-im2.contexts.chat.messages.content.image.view :as image]))

(def rectangular-style-count 3)

(defn border-tlr
  [index]
  (when (= index 0) 12))

(defn border-trr
  [index count album-style]
  (when (or (and (= index 1) (not= count rectangular-style-count))
            (and (= index 0) (= count rectangular-style-count) (= album-style :landscape))
            (and (= index 1) (= count rectangular-style-count) (= album-style :portrait)))
    12))

(defn border-blr
  [index count album-style]
  (when (or (and (= index 0) (< count rectangular-style-count))
            (and (= index 2) (> count rectangular-style-count))
            (and (= index 1) (= count rectangular-style-count) (= album-style :landscape))
            (and (= index 0) (= count rectangular-style-count) (= album-style :portrait)))
    12))

(defn border-brr
  [index count]
  (when (or (and (= index 1) (< count rectangular-style-count))
            (and (= index (- (min count constants/max-album-photos) 1)) (> count 2)))
    12))

(defn find-size
  [size-arr album-style]
  (if (= album-style :landscape)
    {:width (first size-arr) :height (second size-arr) :album-style album-style}
    {:width (second size-arr) :height (first size-arr) :album-style album-style}))

(defn album-message
  [{:keys [albumize?] :as message}]
  (let [shared-element-id (rf/sub [:shared-element-id])
        first-image       (first (:album message))
        album-style       (if (> (:image-width first-image) (:image-height first-image))
                            :landscape
                            :portrait)
        images-count      (count (:album message))
        ;; album images are always square, except when we have 3 images, then they must be rectangular
        ;; (portrait or landscape)
        portrait?         (and (= images-count rectangular-style-count) (= album-style :portrait))]
    (if (and albumize? (> images-count 1))
      [rn/view
       {:style (style/album-container portrait?)}
       (map-indexed
        (fn [index item]
          (let [images-size-key (if (< images-count constants/max-album-photos) images-count :default)
                size            (get-in constants/album-image-sizes [images-size-key index])
                dimensions      (if (not= images-count rectangular-style-count)
                                  {:width size :height size}
                                  (find-size size album-style))]
            [rn/touchable-opacity
             {:key            (:message-id item)
              :active-opacity 1
              :on-press       (fn []
                                (rf/dispatch [:chat.ui/update-shared-element-id (:message-id item)])
                                (js/setTimeout #(rf/dispatch [:navigate-to :lightbox
                                                              {:messages (:album message) :index index}])
                                               100))}
             [fast-image/fast-image
              {:style     (style/image dimensions index portrait?)
               :source    {:uri (:image (:content item))}
               :native-ID (when (and (= shared-element-id (:message-id item))
                                     (< index constants/max-album-photos))
                            :shared-element)}]
             (when (and (> images-count constants/max-album-photos)
                        (= index (- constants/max-album-photos 1)))
               [rn/view
                {:style (merge style/overlay
                               {:border-bottom-right-radius (border-brr index images-count)})}
                [quo/text
                 {:weight :bold
                  :size   :heading-2
                  :style  {:color colors/white}}
                 (str "+" (- images-count (dec constants/max-album-photos)))]])]))
        (:album message))]
      [:<>
       (map-indexed
        (fn [index item]
          [image/image-message index item])
        (:album message))])))
