(ns snippetbox.validate)

(defn field [m key pred message]
  (let [invalid? (not (pred (get m key)))
        has-error? (get-in m [:errors key])]
    (if (and invalid? (not has-error?))
      (assoc-in m [:errors key] message)
      m)))

(defn snippet-form [form]
  (-> form
      (field :title not-empty "This field cannot be blank")
      (field :title #(<= (count %) 100) "This field cannot be more than 100 characters long")
      (field :content not-empty "This field cannot be blank")
      (field :expires #{1 7 365} "This field must equal 1, 7, or 365")))

(defn register-form [form]
  (-> form
      (field :name not-empty "This field cannot be blank")
      (field :email not-empty "This field cannot be blank")
      (field :email #(re-matches #".+\@.+\..+" %) "This field must be a valid email address")
      (field :password not-empty "This field cannot be blank")
      (field :password #(>= (count %) 8) "This field must be at least 8 characters long")))

(defn login-form [form]
  (-> form
      (field :email not-empty "This field cannot be blank")
      (field :email #(re-matches #".+\@.+\..+" %) "This field must be a valid email address")
      (field :password not-empty "This field cannot be blank")))

(comment

  (field {:title ""} :title not-empty "must not be empty")
  (snippet-form {:title "" :content "" :expires 2})

  :rcf)
