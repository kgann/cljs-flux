(ns cljs-flux.dispatcher-test
  (:require-macros [cemerick.cljs.test :refer [is deftest testing]])
  (:require [cemerick.cljs.test :as t]
            [cljs-flux.dispatcher :refer [dispatcher register unregister dispatch wait-for]]))

(deftest registering-callbacks
  (let [d (dispatcher)
        store (atom {})]
    (testing "invoking all registered callbacks"
      (register d (fn [_] (swap! store assoc :foo "FOO")))
      (register d (fn [_] (swap! store assoc :bar "BAR")))
      (dispatch d {})
      (is (= {:foo "FOO" :bar "BAR"} @store)))))

(deftest unregistering-callbacks
  (let [d (dispatcher)
        store (atom {})
        foo (register d (fn [_] (swap! store assoc :foo "FOO")))
        bar (register d (fn [_] (swap! store assoc :bar "BAR")))]
    (testing "unregister"
      (unregister d bar)
      (dispatch d {})
      (is (= {:foo "FOO"} @store)))))

(deftest callback-deferment
  (let [d (dispatcher)
        store (atom "")
        foo (register d (fn [_] (swap! store str "foo")))
        bar (register d (fn [_] (wait-for d [foo]) (swap! store str "bar")))]
    (testing "wait-for"
      (dispatch d {})
      (is (= "foobar" @store)))
    (testing "wait-for outside of callback"
      (is (thrown-with-msg? js/Error #"while dispatching" (wait-for d [999]))))
    (testing "wait-for invalid token"
      (register d (fn [_] (wait-for d [999])))
      (is (thrown-with-msg? js/Error #"does not map" (dispatch d {}))))))

(deftest circular-dependencies
  (let [d (dispatcher)
        store (atom "")]
    (testing "circular dependency"
      (register d (fn [_] (wait-for d ["ID_2"])))
      (register d (fn [_] (wait-for d ["ID_1"])))
      (is (thrown-with-msg? js/Error #"Circular dependency" (dispatch d {}))))))

(deftest dispatching
  (let [d (dispatcher)]
    (testing "dispatching while dispatching"
      (register d #(dispatch d %))
      (is (thrown-with-msg? js/Error #"Cannot dispatch" (dispatch d {}))))))

(deftest exception-handling
  (let [d (dispatcher)
        store (atom nil)]
    (testing "callback exceptions do not leave dispatcher in unexpected state"
      (register d (fn [{:keys [throw?]}]
                    (if throw?
                      (throw (js/Error. "Whoops"))
                      (reset! store "FOO"))))
      (is (thrown? js/Error (dispatch d {:throw? true})))
      (dispatch d {:throw? false})
      (is (= "FOO" @store)))))
