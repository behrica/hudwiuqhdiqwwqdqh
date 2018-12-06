(defproject country-card-map "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[refactor-nrepl "2.4.0"]
            [cider/cider-nrepl "0.19.0-SNAPSHOT"]
            [lein-jupyter "0.1.16"]
            
            ]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [rojure "0.2.0"]
                 [com.rpl/specter "1.1.1"]
                 [behrica/specter-x "0.1.1"]

                 [ dk.ative/docjure "1.14.0-SNAPSHOT"]
                 [semantic-csv "0.2.1-alpha1"]])


