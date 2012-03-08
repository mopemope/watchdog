(ns watchdog
  (:gen-class)
  (:use [clojure.data]
        [clojure.java.io]
        [clojure.set :only (union)]
        [clojure.inspector :only (atom?)])
  (:import (java.io File)))

; (set! *warn-on-reflection* true)

(def ^:dynamic *watch-interval* (ref (* 1000 5)))

(declare file-diff)

(defn set-interval [interval]
  (dosync (ref-set *watch-interval* interval)))

(defn- mkvec [x]
  (if (atom? x)
    (vector x)
    x))

(defprotocol FileMatch
  (match [pattern file]))

(defrecord FileInfo [path ctime file]
  Diff
  (diff-similar [a b] (file-diff a b)))

(extend-protocol FileMatch
  java.util.regex.Pattern
  (match [^java.util.regex.Pattern pattern ^File file]
    (re-matches pattern (.getName file)))
  java.lang.String
  (match [^String pattern ^File file]
    (.endsWith ^String (.getName file) pattern)))


(defn- file-diff [a b]
  (if (and (= (:ctime a) (:ctime b)) (= (:path a) (:path b)))
    [nil nil a] [a b nil]))

(defn- create-filter-fn [patterns]
  (fn [^File file]
    (loop [p patterns]
      (if (nil? p)
        false
        (if (match (first p) file)
          true
          (recur (next p)))))))

(defn- create-fileinfo [^File f]
  (when (.isFile f)
    (FileInfo. (.getCanonicalPath f) (.lastModified f) f)))

(defn- create-fileinfos [path exts]
  (map create-fileinfo
       (filter (create-filter-fn exts) (file-seq (as-file path)))))

(defn- observe-files [paths exts]
  (let [exts (mkvec exts)
        paths (mkvec paths)]
    (loop [paths paths result nil]
      (if (nil? paths)
        result
        (recur (next paths)
               (union result (set (create-fileinfos (first paths) exts))))))))

(defn- difference [s1 s2]
  (reduce (fn [result item]
            (if (contains? s2 item)
              (disj result item)
              result))
          s1 s1))

(defn- intersection [s1 s2]
  (reduce (fn [result item]
            (if (some #(= (:path %) (:path item)) s2)
              result
              (disj result item)))
          s1 s1))

(defn- to-vec [sets]
  (vec (map (fn [x] (:file x)) sets)))

(defn- check-updatefiles [diffs]
  (let [a (first diffs)
        b (second diffs)
        c (last diffs)]
      (when (or (not (nil? a)) (not (nil? b)))
        (let [up (intersection b a)]
          {:create (to-vec (difference b up))
           :modify (to-vec up)
           :delete (to-vec (difference a (intersection a b)))}))))


(defn- get-diff [old new]
  (check-updatefiles (diff old new)))


(defn- watch-files [path f ext]
  (loop [old nil]
    (if (nil? old)
      (recur (observe-files path ext))
      (do
        (Thread/sleep @*watch-interval*)
        (let [targets (observe-files path ext)
              data (get-diff old targets)]
          (if (not (nil? data))
            (f data))
          (recur targets))))))

(defn watch-start
  ([path f]
    (watch-files path f #".*"))
  ([path f ext]
    (watch-files path f ext)))


