# cljs-flux

**Experimental** ClojureScript implementation of [Facebook's Flux architecture](https://facebook.github.io/flux/)

[Github facebook/flux](https://github.com/facebook/flux)

## Overview

ClojureScript experiment surrounding Facebook's Flux architecture. Currently, Facebook only provides an implementation for `Dispatcher.js`. This repo contains a similar implementation.

## Usage

Refer to [Dispatcher.js](https://github.com/facebook/flux/blob/master/src/Dispatcher.js)

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
(assert (= "Atlanta, GA" (:city-state @store)))
(assert (= 100 (:price @store)))
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
