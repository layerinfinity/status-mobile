(ns status-im2.contexts.share.events
  (:require [taoensso.timbre :as log]
            [utils.re-frame :as rf]))

(rf/defn open-profile-share-view
  {:events [:share/open-profile]}
  [{:keys [db]}]
  {:dispatch [:show-popover
              {:view                       :profile-share
               :style                      {:margin 0}
               :disable-touchable-overlay? true
               :blur-view?                 true
               :blur-view-props            {:blur-amount 20
                                            :blur-type   :dark}}]})
