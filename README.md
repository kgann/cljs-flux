# cljs-flux

**Experimental** ClojureScript implementation of [Facebook's Flux architecture](https://facebook.github.io/flux/)

[Github facebook/flux](https://github.com/facebook/flux)

## Overview

ClojureScript experiment surrounding Facebook's Flux architecture. Currently, Facebook only provides an implementation for `Dispatcher.js`. This repo contains a similar implementation.

Refer to [Dispatcher.js](https://github.com/facebook/flux/blob/master/src/Dispatcher.js):

>  Dispatcher is used to broadcast payloads to registered callbacks. This is
different from generic pub-sub systems in two ways:
*   1) Callbacks are not subscribed to particular events. Every payload is
      dispatched to every registered callback.
*   2) Callbacks can be deferred in whole or part until other callbacks have
      been executed.

**Note** #2 is not completely satisfied. Callbacks can be deferred **only in whole**.

## Usage

```clojure
(require '[cljs-flux.dispatcher :refer :all])

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
                     :desc (str "For $" price " you can fly to "
                                 (:city-state @store))))))

(dispatch flights {:price 100 :city "Atlanta" :state "GA"})
(assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))

(unregister flights state-dispatch)

(dispatch flights {:city "Athens" :state "XXXX"})
(assert (= "GA" (:state @store)))
(assert (= "Athens, GA" (:city-state @store)))
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
