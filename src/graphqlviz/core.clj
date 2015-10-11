(ns graphqlviz.core
  (:require [clojure.data.json :as json]
            [tangle.core :refer :all]))

;;; GraphQL

(defn internal-type? [t]
  (let [name (:name t)]
    (and name (.startsWith name "__"))))

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
    "NON_NULL" (str "NotNull<" (describe-field-type (:ofType t)) ">")
    "LIST" (str "List<" (describe-field-type (:ofType t)) ">")
    (:name t)))

(defn type->edges [t]
  (remove nil? (map (fn [{:keys [type name description]}]
                      [(:name t) (:name (terminal-type type)) {:label (str name ": " (describe-field-type type))
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
                                     :graph {:rankdir :LR}
                                     :directed? true
                                     :node->id type->id
                                     :node->descriptor type->descriptor})
        svg (dot->svg dot)]
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
