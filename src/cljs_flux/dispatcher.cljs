(ns cljs-flux.dispatcher
  (:require [cljs.core.async :as async :refer [chan put! <!]])
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]]))

(defprotocol IDispatcher
  (^:export dispatch [this tag payload])
  (^:export unregister [this tag])
  (^:export register [this tag callback]
                     [this tag wait-for callback]))

(defn dispatcher []
  (let [chan (chan)
        callbacks (atom {})
        wait-fors (atom {})]
    (reify IDispatcher
      (register [this tag f]
        (register this tag [] f))
      (register [_ tag wait-for f]
        (swap! callbacks assoc tag f)
        (swap! wait-fors assoc tag wait-for))
      (unregister [_ tag]
        (swap! callbacks dissoc tag)
        (swap! wait-fors dissoc tag))
      (dispatch [_ tag payload]
        (let [tags (tree-seq @wait-fors (comp reverse @wait-fors) tag)]
          (doseq [t (rseq (vec tags))]
            (put! chan [(get @callbacks t) payload]))
          (go-loop [[callback payload :as v] (<! chan)]
            (when v
              (callback payload)
              (recur (<! chan)))))))))

(comment
  (def flights (dispatcher))
  (def store (atom {}))

  ;; register callback with ID :state
  (register flights :state
            (fn [{:keys [state]}]
              (when state
                (swap! store assoc :state state))))

  ;; register callback with ID :city
  ;; wait for :state
  (register flights :city [:state]
            (fn [{:keys [city]}]
              (when city
                (swap! store
                       assoc
                       :city city
                       :city-state (str city ", " (:state @store))))))

  ;; register callback with ID :price
  ;; waits for :city which will wait for :state
  (register flights :price [:city]
            (fn [{:keys [price]}]
              (when price
                (swap! store
                       assoc
                       :price price
                       :desc (str "For $" price " you can fly to " (:city-state @store))))))

  (dispatch flights :price {:price 100 :city "Atlanta" :state "GA"})

  (assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))
  (assert (= "Atlanta, GA" (:city-state @store)))
  (assert (= 100 (:price @store)))

  )
