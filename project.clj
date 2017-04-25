(defproject macroz/graphqlviz "0.5.0"
  :description "Visualize GraphQL schemas using Graphviz"
  :url "https://github.com/Macroz/graphqlviz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [macroz/tangle "0.1.8"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.1.0"]
                 [org.clojure/tools.cli "0.3.3"]]
  :main graphqlviz.core
  :aot :all
  )
