(ns test
  (:require  [clojure.test :as t]))

(def df-byrows [{:a "" :b 2} {:a "1" :b 2} {:a "":b 2}{:a "" :b 2}])
(def df-bycols {:a ["" "1" "" ""] :b [2 2 2 2] } )


;; mutate full column
(map  #(assoc %1 :a %2)   df-byrows (fill-empty-forward (map :a df-byrows)))
(assoc df-bycols :a (fill-empty-forward (:a df-bycols)))

;; mutate row wise

(map #(assoc % :c (str (:a %)  "/" (:b %)))  df-byrows)
(assoc df-bycols :c  (map  #(str %1 "/" %2 ) (:a df-bycols)   (:b df-bycols)))

(update)
