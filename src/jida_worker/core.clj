(ns jida-worker.core
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clj-redis.client :as redis])
  (:gen-class))

(def default-redis-uri "redis://redistogo:dfcab96405c0df37b9e0088d69995010@gar.redistogo.com:9184/")
(def default-codeq-jar-path"/Users/wei/projects/clojure/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar")
(def default-datomic-uri "datomic:free://localhost:4334/git")
(def redis-conn (atom nil))

(defn db []
  (or @redis-conn
      (let [uri (or (System/getenv "REDISTOGO_URL") default-redis-uri)]
        (reset! redis-conn (redis/init :url uri)))))

(def working-dir "/tmp")
(defn process-repo [address]
  (let [[[_ dirname]] (re-seq #".*/(.*).git" address)]
    (with-sh-dir
      working-dir
      (println "Cloning " address "..")
      (sh "git" "clone" address))
    (with-sh-dir
      (str working-dir "/" dirname)
      (println "Importing into codeq..")
      (println (sh "java"
          "-server" "-Xmx1g"
          "-jar" (or (System/getenv "CODEQ_JAR_PATH") default-codeq-jar-path)
          (or (System/getenv "DATOMIC_URI") default-datomic-uri)))
      (println "Done."))))

(defn get-tasks []
  (println "Waiting for a task..")
  (let [[_ address] (redis/blpop (db) ["tasks"] 0)]
    (process-repo address)))

(defn -main [& m]
  (println (System/getenv))
  (dorun (repeatedly get-tasks)))