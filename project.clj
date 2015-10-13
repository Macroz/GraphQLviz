(defproject graphqlviz "0.1.0-SNAPSHOT"
  :description "Visualize GraphQL schemas using Graphviz"
  :url "https://github.com/Macroz/graphqlviz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [macroz/tangle "0.1.8"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.0.0"]]
  :main graphqlviz.core
  :aot :all
  )
