(ns status-im2.contexts.communities.discover.view
  (:require [i18n.i18n :as i18n]
            [oops.core :as oops] ;; TODO move to status-im2
            [quo2.core :as quo]
            [quo2.foundations.colors :as colors]
            [quo2.components.separator :as separator]
            [react-native.core :as rn]
            [react-native.safe-area :as safe-area]
            [reagent.core :as reagent]
            [status-im.react-native.resources :as resources]
            [status-im.ui.screens.communities.community :as community]
            [utils.re-frame :as rf]))

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
    :padding-vertical 12}
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
  [:<>
   [quo/separator]
   [rn/view
    {:style {:padding-vertical   12
             :margin-bottom      4
             :margin-top         12
             :height             56}}
    [quo/tabs
     {:size           32
      :on-change      #(reset! selected-tab %)
      :default-active :joined
      :data           [{:id :all   :label (i18n/label :t/all)   :accessibility-label :all-communities-tab}
                       {:id :open  :label (i18n/label :t/open)  :accessibility-label :open-communities-tab}
                       {:id :gated :label (i18n/label :t/gated) :accessibility-label :gated-communities-tab}]}]]])


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

(defn other-communities-list
  [communities view-type selected-tab]
  [rn/flat-list
   {:key-fn                            :id
    :keyboard-should-persist-taps      :always
    :shows-vertical-scroll-indicator   false
    :separator                         [rn/view {:margin-bottom 16}]
    :data                              communities
    :header                            (discover-communities-segments selected-tab)
    :sticky-header-indices             [0]
    :render-fn                         render-fn
    :render-data                       {:featured? false
                                        :width     "100%"
                                        :view-type view-type}}])

(defn discover-communities-list
  [selected-tab view-type]
  (let [ids-by-user-involvement (rf/sub [:communities/community-ids-by-user-involvement])
        all-communities         (rf/sub [:communities/sorted-communities])
        tab                     @selected-tab]
    [rn/view
     {:style {:flex             1
              :padding-vertical 12}}
     (case tab
       :all
       [other-communities-list all-communities view-type]

       :open
       [other-communities-list (:open ids-by-user-involvement) view-type]

       :gated
       [other-communities-list (:gated ids-by-user-involvement) view-type]

       [quo/information-box
        {:type :error
         :icon :i/info}
        (i18n/label :t/error)])]))

(defn discover
  []
  (let [view-type (reagent/atom :card-view)
        selected-tab (reagent/atom :all)]
    (fn []
      (let [featured-communities       (rf/sub [:communities/featured-communities])
            featured-communities-count (count featured-communities)]
        [safe-area/consumer
         (fn []
           [rn/view
            {:style {:flex               1
                     :padding-horizontal 20
                     :background-color   (colors/theme-colors
                                          colors/neutral-30
                                          colors/neutral-90)}}
            [quo/button
             {:icon     true
              :type     :grey
              :size     32
              :style    {:margin-vertical 12}
              :on-press #(rf/dispatch [:navigate-back])}
             :i/close]
            [screen-title]
            [featured-communities-header featured-communities-count]
            [featured-list featured-communities @view-type]
            [discover-communities-list selected-tab @view-type]])]))))
