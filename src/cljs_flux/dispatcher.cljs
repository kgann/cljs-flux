(ns cljs-flux.dispatcher)

(def ^:private prefix "ID_")

(defprotocol IDispatcher
  (register [this callback]
    "Registers a callback to be invoked with every dispatched payload. Returns
    a token that can be used with `wait-for`.")
  (unregister [this id]
    "Removes a callback based on its token.")
  (wait-for [this ids]
    "Waits for the callbacks specified to be invoked before continuing execution.")
  (dispatch [this payload]
    "Dispatches a payload to all registered callbacks")
  (-invoke-callback [this id]
    "Invoke the registered callback."))

(defrecord Dispatcher [registered dispatching? pending-payload token]
  IDispatcher
  (register [_ f]
    (let [id (str prefix (swap! token inc))]
      (swap! registered assoc-in [id :callback] f)
      id))

  (unregister [_ id]
    (swap! registered dissoc id))

  (-invoke-callback [_ id]
    (let [callback (:callback (get @registered id))]
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
        pending?
        (throw (js/Error. (str "Circular dependency detected while waiting for `" id "'.")))

        (some? callback)
        (-invoke-callback this id)

        :else
        (throw (js/Error. (str \` id "' does not map to a registered callback."))))))

  (dispatch [this payload]
    (when @dispatching?
      (throw (js/Error. "Cannot dispatch in the middle of a dispatch.")))
    (let [ids (sort (keys @registered))]
      (reset! pending-payload payload)
      (reset! dispatching? true)
      (doseq [id ids]
        (swap! registered assoc-in [id :pending?] false)
        (swap! registered assoc-in [id :handled?] false))
      (try
        (doseq [id ids :when (not (get-in @registered [id :pending?]))]
          (-invoke-callback this id))
        (finally
          (reset! dispatching? false)
          (reset! pending-payload nil))))))

(defn dispatcher
  "Create and return Flux dispatcher instance"
  []
  (map->Dispatcher {:registered (atom {})
                    :dispatching? (atom false)
                    :pending-payload (atom nil)
                    :token (atom 0)}))
