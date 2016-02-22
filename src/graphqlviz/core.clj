(ns graphqlviz.core
  (:require [clojure.data.json :as json]
            [tangle.core :refer :all]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

;;; GraphQL

(defn internal-type? [t]
  (let [name (:name t)]
    (or (and name (.startsWith name "__"))
        #_(= "QueryType" name))))

(defn scalar? [t]
  (= "SCALAR" (:kind t)))

(defn enum? [t]
  (= "ENUM" (:kind t)))

(defn enum-values [t]
  (:enumValues t))

(defn terminal-type [t]
  (if (:ofType t)
    (recur (:ofType t))
    t))

(defn introspection-query []
  {:query (slurp (io/resource "introspection.query"))})



;;; Configuration

(def config (atom {:expand-args false
                   :expand-arg-types false}))


;;; Visualization

(defn relation-field? [f]
  (not (scalar? (terminal-type (:type f)))))

(defn type->id [t]
  (:name t))

(defn describe-field-type [t]
  (case (:kind t)
    "NON_NULL" (str (describe-field-type (:ofType t)) "!")
    "LIST" (str "[" (describe-field-type (:ofType t)) "]")
    (:name t)))

(defn arg->str [a]
  (str (:name a)
       (if (:expand-arg-types @config)
         (str ": " (describe-field-type (:type a)))
         "")))

(defn format-args [args]
  (if (:expand-args @config)
    (apply str (interpose ", " (map arg->str args)))
    "..."))

(defn get-field-label [field]
  (let [{:keys [type name description args]} field]
    (str name
         (if-not (empty? args)
           (str "(" (format-args args) ")")
           "")
         ": "
         (describe-field-type type))))

(defn field-to-edge [t field]
  (let [{:keys [type name description]} field]
    [(:name t) (:name (terminal-type type)) {:label (get-field-label field)
                                             :labeltooltip (str description)}]))

(defn type->edges [t]
  (->> (:fields t)
       (filter relation-field?)
       (map (partial field-to-edge t))))

(defn field->str [t f]
  (let [target (str "<" (:name t) "_" (:name f) ">")]
    (str (:name f) ": " (describe-field-type (:type f)))))

(defn edge-matches? [e s]
  (>= (.indexOf (second e) s) 0))

(defn relates-to? [t name]
  (some #(edge-matches? % name) (:edges t)))

(defn connection-type? [t]
  (let [pointed-to-type-name (string/replace (:name t) "Connection" "")]
    (and (string/ends-with? (:name t) "Connection")
         (or (relates-to? t pointed-to-type-name)
             (relates-to? t "PageInfo")))))

(defn edge-type? [t]
  (let [pointed-to-type-name (string/replace (:name t) "Edge" "")]
    (and (string/ends-with? (:name t) "Edge")
         (relates-to? t pointed-to-type-name))))

(defn page-info-type? [t]
  (string/ends-with? (:name t) "PageInfo"))

(defn stereotype [t]
  (cond (enum? t) "&laquo;enum&raquo;"
        (connection-type? t) "&laquo;connection&raquo;"
        (edge-type? t) "&laquo;edge&raquo;"
        (page-info-type? t) "&laquo;page info&raquo;"
        :else ""))

(defn type-description [t]
  [[:TR [:TD {:BGCOLOR "#E535AB" :COLSPAN 2} [:FONT {:COLOR "white"} [:B (:name t)] [:BR] (stereotype t)]]]])

(defn scalar-field-description [t f]
  [:TR [:TD {:ALIGN "left" :BORDER 0} (:name f) ": " (describe-field-type (:type f))]])

(defn enum-value-description [v]
  [:TR [:TD {:ALIGN "left" :BORDER 0 :COLSPAN 2} (:name v)]])

(defn type->descriptor [t]
  (let [scalar-fields (remove relation-field? (:fields t))]
    {:label (into [:TABLE {:CELLSPACING 0 :BORDER 1}]
                  (concat (type-description t)
                          (map (partial scalar-field-description t) scalar-fields)
                          (map enum-value-description (enum-values t))))}))

(defn render [nodes edges filename]
  (println "Generating graph from" (count nodes) "nodes and" (count edges) "edges")
  (let [dot (graph->dot nodes edges {:node {:shape :none :margin 0}
                                     :graph {:label filename :rankdir :LR}
                                     :directed? true
                                     :node->id type->id
                                     :node->descriptor type->descriptor})]
    (println "Writing DOT" (str filename ".dot"))
    (spit (str filename ".dot") dot)
    (println "Writing SVG" (str filename ".svg"))
    (spit (str filename ".svg") (dot->svg dot))
    ))

(defn slurp-json [filename]
  (-> filename
      (slurp)
      (json/read-str :key-fn keyword)))

(defn add-edge-info [node edges]
  (assoc node :edges edges))

(defn assoc-nodes-edges [nodes edges-by-name]
  (vec (for [node nodes]
         (assoc node :edges (edges-by-name (:name node))))))

(defn interesting-node? [n]
  (not (or (scalar? n)
           (page-info-type? n)
           (edge-type? n)
           (connection-type? n))))

(defn interesting-edge? [e nodes-by-name]
  (let [pointed-to-node (first (nodes-by-name (second e)))]
    (interesting-node? pointed-to-node)))

(defn load-schema [filename]
  (let [schema (slurp-json filename)
        types (:types (:__schema (:data schema)))
        nodes (remove internal-type? types)
        nodes-by-name (group-by :name nodes)
        edges (mapcat type->edges nodes)
        edges-by-name (group-by first edges)
        nodes (filter interesting-node? nodes)
        edges (filter #(interesting-edge? % nodes-by-name) edges)
        nodes (assoc-nodes-edges nodes edges-by-name)]
    [nodes edges]))

(defn fetch-schema [input output]
  (if (.startsWith input "http")
    (let [options (merge {:content-type "application/json"
                          :body (json/write-str (introspection-query))}
                         (:auth @config))]
      (println "Fetching schema from" input)
      (let [response (http/post input options)]
        (spit (str output ".json") (-> response
                                       (:body)
                                       (json/read-str :key-fn keyword)
                                       (json/write-str)))))
    (do (println "Loading schema from" input)
        (spit (str output ".json") (slurp input)))))

(defn process-schema [output]
  (let [[nodes edges] (load-schema (str output ".json"))]
    (render nodes edges output)))

(def cli-options
  [["-a" "--auth AUTH" "Type of auth: basic, digest or oauth2"
    :parse-fn (comp keyword string/trim)]
   ["-u" "--user USERNAME" "Username for authentication"]
   ["-p" "--password PASSWORD" "Password for authentication"]
   ["-o" "--oauth TOKEN" "oAuth2 token for authentication"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [parsed-options (parse-opts args cli-options)
        options (:options parsed-options)
        args (:arguments parsed-options)
        auth-config (case (:auth options)
                      :basic {:auth {:basic-auth [(:username options) (:password options)]}}
                      :digest {:auth {:digest-auth [(:username options) (:password options)]}}
                      :oauth {:auth {:oauth-token (:oauth-token options)}}
                      {})]
    (swap! config merge auth-config)
    (if (= (count args) 2)
      (let [input (first args)
            output (second args)]
        (fetch-schema input output)
        (process-schema output)
        (println "Done!")
        (shutdown-agents))
      (if (:help options)
        (do (println "Usage:")
            (println "  graphqlviz <url-or-file> <output-name> <options>")
            (println "\nOptions:") 
            (println (:summary parsed-options)))
        (println (:errors parsed-options))))))
