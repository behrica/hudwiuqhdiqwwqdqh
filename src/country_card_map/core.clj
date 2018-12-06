(ns country-card-map.core
  (:require [rojure.core :refer [r-set! r-eval r-get get-r]]
            [clojure.core.matrix.dataset :as ds]
            [com.rpl.specter :as sp]
            [dk.ative.docjure.spreadsheet :as spr]
            )
  )

(defn camel-case->dash-sep [^String s]
  (when s
    (let [with-dashes (.replaceAll s
                        "([a-z])([A-Z])" "$1-$2")]
      (.toLowerCase with-dashes))))


(defn fix-dashes
  [s]
  (-> s
      (clojure.string/replace #"-+" "-")
       (clojure.string/replace #"-$" "")
      )
)

(defn ->nice-keyword [^String s]
  (when s
    (-> (.trim s)
      (.replaceAll "[^a-zA-Z]" "-")
      (.replaceAll "\\s+" "-")
                                       ;camel-case->dash-sep
      .toLowerCase
      fix-dashes
      keyword)))
 
(defn ->nice-keywords [scol]
  (map ->nice-keyword scol))

(defn use-first-as-column-names [rows]
  (let [column-names (map ->nice-keyword (vals (first rows)))
        column-names (replace {nil :ministry} column-names)
        rest-rows (rest rows)
        renames  (zipmap (keys (first rest-rows)) column-names)]
    (map #(clojure.set/rename-keys % renames) rest-rows ) 
    
    )
  )

(defn extract-tables [filename country]
  (->>
   (with-open [r (get-r)]
     (r-set! r "fileName" [filename])
     (r-eval r "source(\"/home/carsten/Dropbox/sources/countryCardMap/extractTables.R\")")
     (r-get r "tbls")
     )
   (map ds/row-maps)
  ))


(defn fill-empty-forward [v]
  (if (= "" (first v))
    (concat [""]
     (fill-empty-forward (rest v))
     )
    
    (if (some #(= "" %) v) 
      (recur (concat (take 1 v)
                     (map #(if (= "" %2) %1 %2)
                          (take (- (count v) 1)  v)
                          (next v))))
      v))
)
  
; (assert (not (= "" (first v))))


(defn fill-column-forward [column v]
   (map  #(assoc % column %2) v (fill-empty-forward (map column v ) )))


(defn fill-all-columns-forward [v]
  (let [fillers (map #(partial fill-column-forward %) (keys (first v)))
        comp-filler (apply comp fillers)]
    (comp-filler v)))

(defn save-trim [s]
  (if (nil? s)
    nil
    (->
     s
     (clojure.string/trim-newline)
     (clojure.string/trim)
     )
    )
  )


(defn trim-all [v]
  (sp/transform [sp/ALL sp/MAP-VALS] save-trim v))

(defn cr->whitespace [s]
  (if (nil? s)
    nil
    (clojure.string/replace s #"[\n\r]" " ")))

(defn cr->whitespace-all [v]
  (sp/transform [sp/ALL sp/MAP-VALS] cr->whitespace v))




(defn spread-national-authority [m]
  (let  [splits (->> (clojure.string/split (:national-central-authority-collecting-data m) #"\s+/|/\s+")
                     (map save-trim))
         col-names  (map #(keyword (str "national-central-authority-collecting-data-level" "-" %)) (range 1 6))
         splits (take 5 (concat splits (repeat 5 "")))
         ]
    (apply merge (map #(assoc m %1 %2 ) col-names splits )))
  )

(defn read-country-file-data [filename country]
  (println filename)
  (let [tables (->> (extract-tables filename country)
                    (map rest)
                    (map trim-all)
                    (map cr->whitespace-all)
                    (map use-first-as-column-names)
                    (map fill-all-columns-forward)
                    (sp/transform [sp/ALL sp/ALL] #(assoc % :country country))
                    (sp/transform [sp/ALL sp/ALL]  spread-national-authority  ))
        tables (zipmap [:animal-population :establishments :diseases] tables)
        
        ]
    tables
    ))


(defn safe-trim[s]
  (if (nil? s)
    nil
    ((comp clojure.string/trim clojure.string/trim-newline) s)
    )
  )


(defn sheet-from-TOTAL [sheet-name column-map]
  (let [all-nil (apply merge (map #(hash-map %1 %2) (vals column-map) (repeat nil) ))]
    (->>
     (spr/load-workbook "./input/TOTAL.xlsx")
     (spr/select-sheet sheet-name)
     (spr/select-columns column-map)
     rest
     (sp/transform [sp/ALL sp/MAP-VALS ] safe-trim)
     (map #(merge all-nil %))
     )))


(def mapping
  [
   [:population "Population"
                   {:A :country
                    :B :animal-categories
                    :C :national-data-base
                    :D :individual-registration
                    :E :contact-person
                    :F :level-4
                    :G :level-3
                    :H :level-2
                    :I :level-1}]


    [:establishments "Establishments"
                   {:A :country
                    :B :type
                    :C :animal-category
                    :D :national-database
                    :E :individual-identification-number
                    :F :name
                    :G :level-4
                    :H :level-3
                    :I :level-2
                    :J :level-1}]

   [:diseases "Diseases"
                   {:A :country
                    :B :type
                    :C :disease
                    :D :national-data-base
                    :E :contact-person
                    :F :level-4
                    :G :level-3
                    :H :level-2
                    :I :level-1}]
  
   ]

  )


(defn read-totals []
  (apply merge (map (fn[[key sheet-name column-mapping]]
                        (hash-map key (sheet-from-TOTAL sheet-name column-mapping))

                        ) mapping))

  )



(def columns-to-use [:country :level-1 :level-2 :level-3 :level-4])

(defn not-empty? [s]
  (not (empty? s))
  )

(defn create-edges [v]
  (let [edges (->> v
                   (map #(select-keys % columns-to-use))
                   (map #(filter some? (map (fn[column] (get % column )    )   columns-to-use  )))    
                   (map #(partition 2 1 %))
                   (apply concat)
                   distinct
                   (map #(hash-map :from (first %)
                                   :to (second %)))
                   (filter #(and (not-empty? (:from %))
                                 (not-empty? (:to %))
                                 (not (= "na" (:from %)))
                                 (not (= "na" (:to %)))

                                 ))
                   )

        
        nodes (distinct (flatten (map (juxt :from :to) edges)))]
    edges
    )
  
  )

(defn create-notes [edges]
  (->> edges
       (map (juxt :from :to))
       flatten
       distinct
       (map #(hash-map :id %
                       :title %))  
   ))

(defn get-total-graph []
  (let [totals (read-totals)
        edges (map (fn[[k v]] (create-edges v)   ) totals)
        edges (apply concat edges)
        nodes (create-notes edges)
        ;; edges (->> diseases
        ;;      (map #(select-keys % columns-to-use))
        ;;      (map #(map (fn[column] (get % column )    )   columns-to-use  ))    
        ;;      (map #(partition 2 1 %))
        ;;      (apply concat)
        ;;      distinct
        ;;      (map #(hash-map :from (first %)
        ;;                      :to (second %))))
        ;; nodes (distinct (flatten (map (juxt :from :to) edges)))
        ]
    {:edges edges
     :nodes nodes}
    
    ))


