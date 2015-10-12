(ns graphqlviz.core
  (:require [clojure.data.json :as json]
            [tangle.core :refer :all]))

;;; GraphQL

(defn internal-type? [t]
  (let [name (:name t)]
    (or (and name (.startsWith name "__"))
        #_(= "QueryType" name))))

(defn scalar? [t]
  (= "SCALAR" (:kind t)))

(defn terminal-type [t]
  (if (:ofType t)
    (recur (:ofType t))
    t))


;;; Visualization

(defn relation-field? [f]
  (not (scalar? (terminal-type (:type f)))))

(defn uninteresting-type? [t]
  (internal-type? t))

(defn type->id [t]
  (:name t))

(defn describe-field-type [t]
  (case (:kind t)
    "NON_NULL" (str (describe-field-type (:ofType t)) "!")
    "LIST" (str "[" (describe-field-type (:ofType t)) "]")
    (:name t)))

(defn format-args [args]
  (apply str (interpose ", " (map (fn [a] (str (:name a) ": " (describe-field-type (:type a)))) args))))

(defn get-field-label [field]
  (let [{:keys [type name description args]} field]
    (str name
         (if-not (empty? args)
           (str "(" (format-args args) ")")
           "")
         ": "
         (describe-field-type type))))

(defn type->edges [t]
  (remove nil? (map (fn [{:keys [type name description] :as field}]
                      [(:name t) (:name (terminal-type type)) {:label (get-field-label field)
                                                               :labeltooltip (str description)}])
                    (filter relation-field? (:fields t)))))

(defn field->str [t f]
  (let [target (str "<" (:name t) "_" (:name f) ">")]
    (str (:name f) ": " (describe-field-type (:type f)))))

(defn type->descriptor [t]
  (let [scalar-fields (remove relation-field? (:fields t))]
    {:label (into [(:name t)]
                  (map (partial field->str t)
                       scalar-fields))}))

(defn render [nodes edges filename]
  (let [dot (graph->dot nodes edges {:node {:shape :record}
                                     :graph {:label filename :rankdir :LR}
                                     :directed? true
                                     :node->id type->id
                                     :node->descriptor type->descriptor})
        svg (dot->svg dot)]
    (spit (str filename ".dot") dot)
    (spit (str filename ".svg") svg)
    ))

(defn load [filename]
  (-> filename
      (slurp)
      (json/read-str :key-fn keyword)))

(defn load-file [filename]
  (let [schema (load filename)
        types (->> (:types (:__schema schema))
                   (remove uninteresting-type?))
        nodes (remove scalar? types)
        edges (mapcat type->edges types)]
    [nodes edges]))

(let [[nodes edges] (load-file "/home/markku/dev/hsl/digitransit-ui/build/schema.json")]
  (render nodes edges "output"))
