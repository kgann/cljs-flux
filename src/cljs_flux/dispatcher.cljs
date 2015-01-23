(ns cljs-flux.dispatcher
  (:require [cljs.core.async :as async :refer [chan put! <!]])
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]]))

(defn- dep-order
  "Given a map of `id -> dependent ids'
  returns list of ids in dependency order.

  (def m {7 [6] 6 [] 5 [3 4] 3 [1] 4 [2 3] 1 [] 2 []})
  (dep-order m) => (1 3 2 4 5 6 7)"
  [m]
  (-> (reduce (fn [coll k]
                (if (some #{k} coll)
                  coll
                  (apply conj coll (tree-seq m m k))))
              [] (keys m))
      (rseq)
      (distinct)))

(defn- spawn-dispatch-handler
  "Spawn an infinite go block to handle all dispatched actions on chan"
  [chan]
  (go-loop [[callback payload] (<! chan)]
    (callback payload)
    (recur (<! chan))))

(defprotocol IDispatcher
  (dispatch [this payload])
  (unregister [this id])
  (register [this callback] [this wait-for callback]))

(defn dispatcher []
  (let [chan (chan)
        callbacks (atom {})
        wait-fors (atom {})
        dispatch-token (atom 0)]
    (spawn-dispatch-handler chan)
    (reify IDispatcher
      (register [this f]
        (register this [] f))
      (register [_ wait-for f]
        (let [id (swap! dispatch-token inc)]
          (swap! callbacks assoc id f)
          (swap! wait-fors assoc id wait-for)
          id))
      (unregister [_ id]
        (swap! callbacks dissoc id)
        (swap! wait-fors dissoc id))
      (dispatch [_ payload]
        (doseq [id (dep-order @wait-fors) :let [cb (get @callbacks id)] :when cb]
          (put! chan [cb payload]))))))

(comment
  (def flights (dispatcher))
  (def store (atom {}))

  ;; register callback and store dispatch token
  (def state-dispatch
    (register flights
              (fn [{:keys [state]}]
                (when state
                  (swap! store assoc :state state)))))

  ;; register callback and store dispatch token
  ;; waits for `state-dispatch'
  (def city-dispatch
    (register flights [state-dispatch]
              (fn [{:keys [city]}]
                (when city
                  (swap! store
                         assoc
                         :city city
                         :city-state (str city ", " (:state @store)))))))

  ;; register callback ignoring dispatch token
  ;; waits for `city-dispatch' which will wait for `state-dispatch'
  (register flights [city-dispatch]
            (fn [{:keys [price]}]
              (when price
                (swap! store
                       assoc
                       :price price
                       :desc (str "For $" price " you can fly to " (:city-state @store))))))

  (dispatch flights {:price 100 :city "Atlanta" :state "GA"})
  (assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))

  (unregister flights state-dispatch)

  (dispatch flights {:city "Athens" :state "XXXX"})
  (assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))
  (assert (= "Athens, GA" (:city-state @store)))

  )
