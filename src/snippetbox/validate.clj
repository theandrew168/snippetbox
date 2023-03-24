(ns snippetbox.validate)

(defn field [m key pred message]
  (if (pred (get m key))
    m
    (assoc-in m [:errors key] message)))

(defn snippet [snippet]
  (-> snippet
      (field :title not-empty "This field cannot be blank")
      (field :title #(<= (count %) 100) "This field cannot be more than 100 characters long")
      (field :content not-empty "This field cannot be blank")
      (field :expires #{1 7 365} "This field must equal 1, 7, or 365")))

(comment

  (field {:title ""} :title not-empty "must not be empty")
  (snippet {:title "" :content "" :expires 2})

  :rcf)