(ns status-im2.contexts.activity-center.view
  (:require [utils.i18n :as i18n]
            [quo.react :as react]
            [quo2.core :as quo]
            [react-native.core :as rn]
            [react-native.safe-area :as safe-area]
            [status-im2.contexts.activity-center.notification-types :as types]
            [status-im2.contexts.activity-center.notification.admin.view :as admin]
            [status-im2.contexts.activity-center.notification.contact-request.view :as contact-request]
            [status-im2.contexts.activity-center.notification.contact-verification.view :as
             contact-verification]
            [status-im2.contexts.activity-center.notification.membership.view :as membership]
            [status-im2.contexts.activity-center.notification.mentions.view :as mentions]
            [status-im2.contexts.activity-center.notification.reply.view :as reply]
            [status-im2.contexts.activity-center.style :as style]
            [utils.re-frame :as rf]))

(defn filter-selector-read-toggle
  []
  (let [unread-filter-enabled? (rf/sub [:activity-center/filter-status-unread-enabled?])]
    [quo/filter
     {:pressed?       unread-filter-enabled?
      :blur?          true
      :override-theme :dark
      :on-press-out   #(rf/dispatch [:activity-center.notifications/fetch-first-page
                                     {:filter-status (if unread-filter-enabled?
                                                       :all
                                                       :unread)}])}]))

(defn empty-tab
  []
  (let [filter-status (rf/sub [:activity-center/filter-status])]
    [rn/view
     {:style               style/empty-container
      :accessibility-label :empty-notifications}
     [rn/view {:style style/empty-rectangle-placeholder}]
     [quo/text
      {:size   :paragraph-1
       :style  style/empty-title
       :weight :semi-bold}
      (i18n/label (if (= :unread filter-status)
                    :t/empty-notifications-title-unread
                    :t/empty-notifications-title-read))]
     [quo/text
      {:size  :paragraph-2
       :style style/empty-subtitle}
      (i18n/label (if (= :unread filter-status)
                    :t/empty-notifications-subtitle-unread
                    :t/empty-notifications-subtitle-read))]]))

(defn tabs
  []
  (let [filter-type       (rf/sub [:activity-center/filter-type])
        types-with-unread (rf/sub [:activity-center/notification-types-with-unread])]
    [quo/tabs
     {:size                32
      :scrollable?         true
      :blur?               true
      :override-theme      :dark
      :style               style/tabs
      :fade-end-percentage 0.79
      :scroll-on-press?    true
      :fade-end?           true
      :on-change           #(rf/dispatch [:activity-center.notifications/fetch-first-page
                                          {:filter-type %}])
      :default-active      filter-type
      :data                [{:id    types/no-type
                             :label (i18n/label :t/all)}
                            {:id                  types/admin
                             :label               (i18n/label :t/admin)
                             :accessibility-label :tab-admin
                             :notification-dot?   (contains? types-with-unread types/admin)}
                            {:id                  types/mention
                             :label               (i18n/label :t/mentions)
                             :accessibility-label :tab-mention
                             :notification-dot?   (contains? types-with-unread types/mention)}
                            {:id                  types/reply
                             :label               (i18n/label :t/replies)
                             :accessibility-label :tab-reply
                             :notification-dot?   (contains? types-with-unread types/reply)}
                            {:id                  types/contact-request
                             :label               (i18n/label :t/contact-requests)
                             :accessibility-label :tab-contact-request
                             :notification-dot?   (contains? types-with-unread types/contact-request)}
                            {:id                  types/contact-verification
                             :label               (i18n/label :t/identity-verification)
                             :accessibility-label :tab-contact-verification
                             :notification-dot?   (contains? types-with-unread
                                                             types/contact-verification)}
                            {:id                  types/tx
                             :label               (i18n/label :t/transactions)
                             :accessibility-label :tab-tx
                             :notification-dot?   (contains? types-with-unread types/tx)}
                            {:id                  types/membership
                             :label               (i18n/label :t/membership)
                             :accessibility-label :tab-membership
                             :notification-dot?   (contains? types-with-unread types/membership)}
                            {:id                  types/system
                             :label               (i18n/label :t/system)
                             :accessibility-label :tab-system
                             :notification-dot?   (contains? types-with-unread types/system)}]}]))

(defn header
  []
  [rn/view
   [quo/button
    {:icon                true
     :type                :blur-bg
     :size                32
     :accessibility-label :close-activity-center
     :override-theme      :dark
     :style               style/header-button
     :on-press            #(rf/dispatch [:hide-popover])}
    :i/close]
   [quo/text
    {:size   :heading-1
     :weight :semi-bold
     :style  style/header-heading}
    (i18n/label :t/notifications)]
   [rn/view {:style style/tabs-and-filter-container}
    [rn/view {:style style/tabs-container}
     [tabs]]
    [rn/view {:style style/filter-toggle-container}
     [filter-selector-read-toggle]]]])

(defn render-notification
  [{:keys [type] :as notification} index]
  [rn/view {:style (style/notification-container index)}
   (cond
     (= type types/contact-verification)
     [contact-verification/view notification {}]

     (= type types/contact-request)
     [contact-request/view notification]

     (= type types/mention)
     [mentions/view notification]

     (= type types/reply)
     [reply/view notification]

     (= type types/admin)
     [admin/view notification]

     (some types/membership [type])
     [membership/view notification]

     :else
     nil)])

(defn view
  []
  [:f>
   (fn []
     (react/effect! #(rf/dispatch [:activity-center.notifications/fetch-first-page]))
     [safe-area/consumer
      (fn [{:keys [top bottom]}]
        (let [notifications (rf/sub [:activity-center/filtered-notifications])
              window-width  (rf/sub [:dimensions/window-width])]
          [rn/view {:style (style/screen-container window-width top bottom)}
           [header]
           [rn/flat-list
            {:data                      notifications
             :content-container-style   {:flex-grow 1}
             :empty-component           [empty-tab]
             :key-fn                    :id
             :on-scroll-to-index-failed identity
             :on-end-reached            #(rf/dispatch [:activity-center.notifications/fetch-next-page])
             :render-fn                 render-notification}]]))])])
