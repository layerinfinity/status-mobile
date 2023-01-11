(ns status-im2.contexts.communities.discover.view
  (:require [i18n.i18n :as i18n]
            [oops.core :as oops] ;; TODO move to status-im2
            [quo2.core :as quo]
            [quo2.foundations.colors :as colors]
            [react-native.core :as rn]
            [react-native.safe-area :as safe-area]
            [reagent.core :as reagent]
            [status-im.react-native.resources :as resources]
            [status-im.ui.screens.communities.community :as community]
            [status-im.ui.components.react :as react]
            [react-native.platform :as platform]
            [status-im2.common.scroll-page.view :as scroll-page]
            [status-im2.contexts.communities.overview.style :as style]
            [utils.re-frame :as rf]))

(def negative-scroll-position-0 (if platform/ios? -44 0))

(def mock-community-item-data  ;; TODO: remove once communities are loaded with this data.
  {:data {:community-color "#0052FF"
          :status          :gated
          :locked?         true
          :cover           (resources/get-image :community-cover)
          :tokens          [{:id    1
                             :group [{:id         1
                                      :token-icon (resources/get-image :status-logo)}]}]
          :tags            [{:id        1
                             :tag-label (i18n/label :t/music)
                             :resource  (resources/get-image :music)}
                            {:id        2
                             :tag-label (i18n/label :t/lifestyle)
                             :resource  (resources/get-image :lifestyle)}
                            {:id        3
                             :tag-label (i18n/label :t/podcasts)
                             :resource  (resources/get-image :podcasts)}]}})

(defn render-fn
  [community-item _ _ {:keys [featured? width view-type]}]
  (let [item (merge community-item
                    (get mock-community-item-data :data)
                    {:featured featured?})]
    (if (= view-type :card-view)
      [quo/community-card-view-item (assoc item :width width)
       #(rf/dispatch [:navigate-to :community-overview (:id item)])]
      [quo/communities-list-view-item
       {:on-press      (fn []
                         (rf/dispatch [:communities/load-category-states (:id item)])
                         (rf/dispatch [:dismiss-keyboard])
                         (rf/dispatch [:navigate-to :community (:id item)]))
        :on-long-press #(rf/dispatch [:bottom-sheet/show-sheet
                                      {:content (fn []
                                                  ;; TODO implement with quo2
                                                  [community/community-actions item])}])}
       item])))

(defn screen-title
  []
  [rn/view
   {:height           56
    :padding-vertical 12
    :justify-content  :center}
   [quo/text
    {:accessibility-label :communities-screen-title
     :weight              :semi-bold
     :size                :heading-1}
    (i18n/label :t/discover-communities)]])

(defn featured-communities-header
  [communities-count]
  [rn/view
   {:flex-direction  :row
    :height          30
    :padding-top     8
    :margin-bottom   8
    :justify-content :space-between}
   [rn/view
    {:flex-direction :row
     :align-items    :center}
    [quo/text
     {:accessibility-label :featured-communities-title
      :weight              :semi-bold
      :size                :paragraph-1
      :style               {:margin-right 6}}
     (i18n/label :t/featured)]
    [quo/counter {:type :grey} communities-count]]
   [quo/icon :i/info
    {:container-style {:align-items     :center
                       :justify-content :center}
     :resize-mode     :center
     :size            20
     :color           (colors/theme-colors
                       colors/neutral-50
                       colors/neutral-40)}]])

(defn discover-communities-segments
  [selected-tab]
  [rn/view
   {:style {:padding-vertical 12
            :margin-bottom    4
            :margin-top       12
            :height           56}}
   [quo/tabs
    {:size           32
     :on-change      #(reset! selected-tab %)
     :default-active :joined
     :data           [{:id :all :label (i18n/label :t/all) :accessibility-label :all-communities-tab}
                      {:id :open :label (i18n/label :t/open) :accessibility-label :open-communities-tab}
                      {:id                  :gated
                       :label               (i18n/label :t/gated)
                       :accessibility-label :gated-communities-tab}]}]])


(defn featured-list
  [communities view-type]
  (let [view-size (reagent/atom 0)]
    (fn []
      [rn/view
       {:style     {:flex-direction :row
                    :overflow       :hidden
                    :width          "100%"
                    :margin-bottom  24}
        :on-layout #(swap! view-size
                           (fn []
                             (oops/oget % "nativeEvent.layout.width")))}
       (when-not (= @view-size 0)
         [rn/flat-list
          {:key-fn                            :id
           :horizontal                        true
           :keyboard-should-persist-taps      :always
           :shows-horizontal-scroll-indicator false
           :separator                         [rn/view {:width 12}]
           :data                              communities
           :render-fn                         render-fn
           :render-data                       {:featured? true
                                               :width     @view-size
                                               :view-type view-type}}])])))


(defn discover-communities-header
  [{:keys [featured-communities-count
           featured-communities
           view-type
           selected-tab]}]
  [react/animated-view
   [screen-title]
   [featured-communities-header featured-communities-count]
   [featured-list featured-communities view-type]
   [quo/separator]
   [discover-communities-segments selected-tab]])

(defn other-communities-list
  [communities view-type scroll-height]
  [rn/flat-list
   {:key-fn                          :id
    :keyboard-should-persist-taps    :always
    :shows-vertical-scroll-indicator false
    :separator                       [rn/view {:margin-bottom 16}]
    :data                            communities
    :scroll-event-throttle           8
    :render-fn                       render-fn
    :render-data                     {:featured? false
                                      :width     "100%"
                                      :view-type view-type}
    :on-scroll                       (fn [event]
                                       (reset! scroll-height (int
                                                              (oops/oget
                                                               event
                                                               "nativeEvent.contentOffset.y"))))}])


(defn discover-communities-list
  [selected-tab view-type]
  (let [ids-by-user-involvement (rf/sub [:communities/community-ids-by-user-involvement])
        all-communities         (rf/sub [:communities/sorted-communities])
        tab                     @selected-tab
        scroll-height              (reagent/atom negative-scroll-position-0)]
    [rn/view {:style {:flex               1
                      :padding-horizontal 20}}
     (case tab
       :all
       [other-communities-list all-communities view-type scroll-height]

       :open
       [other-communities-list (:open ids-by-user-involvement) view-type scroll-height]

       :gated
       [other-communities-list (:gated ids-by-user-involvement) view-type scroll-height]

       [quo/information-box
        {:type :error
         :icon :i/info}
        (i18n/label :t/error)])]))

(defn communities-list-component-fn
  [communities]
  [rn/view
   (map-indexed (fn [inner-index community]
                  [rn/view
                   {:key        (str inner-index (:name community))
                    :margin-top 16}
                   [quo/community-card-view-item community]])
                communities)])

(def discover-communities-list-component (memoize communities-list-component-fn))

(defn render-page-content
  [communities]
  (fn []
    [rn/view {:padding-horizontal 20}
     [discover-communities-list-component communities]]))

(defn render-sticky-header
  []
  (fn [scroll-height]
    (when (> scroll-height 48)
      [rn/blur-view
       {:blur-amount   32
        :blur-type     :xlight
        :overlay-color (if platform/ios? colors/white-opa-70 :transparent)
        :style         style/blur-channel-header}])))

(defn discover-communities-view
  []
  (let [view-type    (reagent/atom :card-view)
        selected-tab (reagent/atom :all)
        featured-communities       (rf/sub [:communities/featured-communities])
        featured-communities-count (count featured-communities)
        scroll-component     (scroll-page/scroll-page-for-discover
                              (fn []
                                [rn/view {:margin-top         (+ 112)}
                                 [discover-communities-header
                                  {:featured-communities-count featured-communities-count
                                   :featured-communities       featured-communities
                                   :view-type                  @view-type
                                   :selected-tab               selected-tab}]])
                              nil
                              (i18n/label :t/discover-communities))
        all-communities       (rf/sub [:communities/sorted-communities])]
    (fn []
      (let [page-component (memoize (render-page-content all-communities))
            sticky-header  (memoize (render-sticky-header))]
        (fn []
          (scroll-component
           sticky-header
           page-component))))))

(defn discover
  []
  (fn []
    [safe-area/consumer
     (fn []
       [rn/view
        {:style {:flex               1
                 :background-color   (colors/theme-colors
                                      colors/white
                                      colors/neutral-95)
                 :position :absolute
                 :top      (if platform/ios? 0 44)
                 :width    "100%"
                 :height   "110%"}}
        [discover-communities-view]])]))
