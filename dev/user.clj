(ns user
  (:require [cemerick.austin.repls :as arepl]))

(defn repl
  "Initialize browser connected REPL"
  []
  (arepl/exec :exec-cmds ["open" "-g"]))
