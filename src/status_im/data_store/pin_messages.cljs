(ns status-im.data-store.pin-messages
  (:require [clojure.set :as set]
            [status-im.data-store.messages :as messages]
            [utils.re-frame :as rf]
            [taoensso.timbre :as log]))

(defn <-rpc
  [message]
  (-> message
      (merge (messages/<-rpc (message :message)))
      (set/rename-keys {:pinnedAt :pinned-at
                        :pinnedBy :pinned-by})
      (dissoc :message)))

(defn pinned-message-by-chat-id-rpc
  [chat-id
   cursor
   limit
   on-success
   on-error]
  {:json-rpc/call [{:method     "wakuext_chatPinnedMessages"
                    :params     [chat-id cursor limit]
                    :on-success (fn [result]
                                  (let [result (set/rename-keys result
                                                                {:pinnedMessages
                                                                 :pinned-messages})]
                                    (on-success (update result :pinned-messages #(map <-rpc %)))))
                    :on-error   on-error}]})

(rf/defn send-pin-message
  [cofx pin-message]
  {:json-rpc/call [{:method     "wakuext_sendPinMessage"
                    :params     [(messages/->rpc pin-message)]
                    :on-success #(log/debug "successfully pinned message" pin-message)
                    :on-error   #(log/error "failed to pin message" % pin-message)}]})
