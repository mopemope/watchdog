# watchdog

Simple Filesystem Events Monitoring

## Installation
-------------

    (defproject your-project "0.0.1-SNAPSHOT"
           :description "descriptions for your project"
           :dependencies [[org.clojars.mopemope/watchdog "0.0.1"]
                           ...]
           ...)

## Usage

    (use 'watchdog))

    (set-interval (* 1000))

    ;; watch-start [pathes handler & extentions]
    (watch-start "./"
      (fn [files]
        (do
          (println "create" (:create files))
          (println "modify" (:modify files))
          (println "delete" (:delete files)))))

    ;; watch only ".clj" and ".java" files
    (watch-start ["./" "your-path1" "your-path2]
      (fn [files]
        (do
          (println "create" (:create files))
          (println "modify" (:modify files))
          (println "delete" (:delete files)))) [#".*\.clj" #".*\.java"])

## File Event Data

    {:create [],  :modify [#<File ./project.clj>],  :delete []}


## License

Copyright (C) 2012 Yutaka Matsubara

Distributed under the Eclipse Public License, the same as Clojure.
