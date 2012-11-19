(defproject jida-worker "0.1.0-SNAPSHOT"
  :plugins [[lein-swank "1.4.0"]]
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0-beta1"]
                 [org.cloudhoist/codeq "0.1.0-SNAPSHOT"]
                 [clj-redis "0.0.12"]
                 [fs/fs "1.3.2"]]
  :main jida-worker.core)
