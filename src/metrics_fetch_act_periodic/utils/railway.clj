(ns metrics-fetch-act-periodic.utils.railway)




(defn apply-or-error [[value errors] func]
  (if (and errors (seq errors))
    [value errors]
    (try
      [(func value) nil]
      (catch Throwable throwable
        [nil [throwable]]))))



(defn ==> [value-in & processes]
  (reduce
   (fn [accum item]
     (let [value-before (first accum)
           [_key func] item
           [value errors] (apply-or-error accum func)]
       (if (seq errors)
         (reduced [value-before errors])
         [(assoc value-before _key value) nil])))
   [value-in nil]
   processes))
