(ns madek.auth.html.dom
  (:require
    [cuerdas.core :as str]
    [goog.dom :as dom]
    [goog.dom.dataset :as dataset]
    [madek.auth.utils.json :as json]
    [madek.auth.utils.url :as url]
    [taoensso.timbre :refer [debug info warn error spy]]))

(defn data-attribute
  "Retrieves JSON and urlencoded data attribute with attribute-name
  from the first element with element-name."
  [element-name attribute-name]
  (debug 'data-attribute [element-name attribute-name])
  (try (-> (.getElementsByTagName js/document element-name)
           (aget 0)
           (dataset/get (str/camel attribute-name))
           url/decode
           json/decode
           cljs.core/js->clj
           clojure.walk/keywordize-keys)
       (catch js/Object e
         (warn e)
         nil)))
