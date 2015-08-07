# cljs-flux

ClojureScript implementation of [Facebook's Flux architecture](https://facebook.github.io/flux/)

[Github facebook/flux](https://github.com/facebook/flux)

## Installation

Add the following dependency to your `project.clj` file for the latest release:

    [cljs-flux "0.1.2"]

## Overview

Currently, Facebook provides an implementation for [Dispatcher.js](https://github.com/facebook/flux/blob/master/src/Dispatcher.js). This repo contains a similar implementation.

>  Dispatcher is used to broadcast payloads to registered callbacks. This is
different from generic pub-sub systems in two ways:
*   1) Callbacks are not subscribed to particular events. Every payload is
      dispatched to every registered callback.
*   2) Callbacks can be deferred in whole or part until other callbacks have
      been executed.

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
;; will wait for `state-dispatch'
(def city-dispatch
  (register flights
            (fn [{:keys [city]}]
              (when city
                (wait-for flights [state-dispatch])
                (swap! store
                       assoc
                       :city city
                       :city-state (str city ", " (:state @store)))))))

;; register callback and store dispatch token
;; will wait for `city-dispatch' which will wait for `state-dispatch'
(def price-dispatch
  (register flights
            (fn [{:keys [price]}]
              (when price
                (wait-for flights [city-dispatch])
                (swap! store
                       assoc
                       :price price
                       :desc (str "For $" price " you can fly to " (:city-state @store)))))))

(dispatch flights {:price 100 :city "Atlanta" :state "GA"})
(assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))

(unregister flights price-dispatch)

(dispatch flights {:city "Athens" :state "OH" :price "FREE"})
(assert (= "For $100 you can fly to Atlanta, GA" (:desc @store)))
(assert (= "Athens, OH" (:city-state @store)))
```

## License

Copyright © 2015 Kyle Gann

Distributed under the Eclipse Public License, the same as Clojure.
