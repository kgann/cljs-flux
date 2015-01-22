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

;; register callback with ID :state
(register flights :state
          (fn [{:keys [state]}]
            (when state
              (swap! store assoc :state state))))

;; register callback with ID :city
;; waits for :state
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
                     :desc (str "For $" price " you can fly to "
                                 (:city-state @store))))))

(dispatch flights :price {:price 100 :city "Atlanta" :state "GA"})

(assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))
(assert (= "Atlanta, GA" (:city-state @store)))
(assert (= 100 (:price @store)))
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
