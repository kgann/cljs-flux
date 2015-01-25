(ns cljs-flux.dispatcher)

(def ^:private prefix "ID_")

(defprotocol IDispatcher
  (register [this callback])
  (unregister [this id])
  (dispatch [this payload])
  (wait-for [this ids])
  (invoke-callback [this id]))

(defn dispatcher []
  (let [registered (atom {})
        dispatching? (atom false)
        pending-payload (atom nil)
        token (atom 0)]
    (reify IDispatcher
      (register [_ f]
        (let [id (str prefix (swap! token inc))]
          (swap! registered assoc-in [id :callback] f)
          id))

      (unregister [_ id]
        (swap! registered dissoc id))

      (invoke-callback [_ id]
        (let [callback (get-in @registered [id :callback])]
          (swap! registered assoc-in [id :pending?] true)
          (callback @pending-payload)
          (swap! registered assoc-in [id :handled?] true)))

      (wait-for [this ids]
        (when-not @dispatching?
          (throw (js/Error. "wait-for must be invoked while dispatching.")))
        (doseq [id ids
                :let [{:keys [pending? handled? callback]} (@registered id)]
                :when (not handled?)]
          (cond
            (and pending? (not handled?))
            (throw (js/Error. (str "Circular dependency detected while waiting for `" id "'.")))

            (some? callback)
            (invoke-callback this id)

            :else
            (throw (js/Error. (str "`" id "' does not map to a registered callback."))))))

      (dispatch [this payload]
        (let [ids (keys @registered)]
          (reset! pending-payload payload)
          (reset! dispatching? true)
          (doseq [id ids]
            (swap! registered assoc-in [id :pending?] false)
            (swap! registered assoc-in [id :handled?] false))
          (try
            (doseq [id ids]
              (invoke-callback this id))
            (finally
              (reset! dispatching? false)
              (reset! pending-payload nil))))))))
