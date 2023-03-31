(ns snippetbox.handler
  (:require [java-time.api :as jt]
            [snippetbox.bcrypt :as bcrypt]
            [snippetbox.render :as render]
            [snippetbox.response :as response]
            [snippetbox.storage :as storage]
            [snippetbox.validate :as validate]))

;; types of handlers:
;; render one thing (ReadView)
;; render multiple things (ListView)
;; render create form (CreateView)
;; process form (???)
;; custom (everything else)

(defn index [store _]
  (let [snippets (storage/list-recent-snippets store 3 (jt/instant))]
    (response/ok (render/index snippets))))

(defn view-snippet [store {:keys [session params]}]
  (let [flash (:flash session)
        now (jt/instant)
        id (parse-long (:id params))
        snippet (storage/read-snippet-by-id store id now)]
    (if snippet
      (let [session (assoc session :flash nil)]
        (-> (render/view-snippet snippet flash)
            response/ok
            (assoc :session session)))
      (response/not-found))))

(defn create-snippet [_ _]
  (let [form {:expires 365}]
    (-> form
        render/create-snippet
        response/ok)))

(defn- form->snippet [{:keys [title content expires]}]
  (let [created (jt/instant)
        expires (jt/plus created (jt/days expires))]
    {:title title :content content :created created :expires expires}))

(defn create-snippet-form [store {:keys [form-params session]}]
  (let [form {:title (get form-params "title")
              :content (get form-params "content")
              :expires (parse-long (get form-params "expires"))}
        form (validate/snippet-form form)]
    (if (not-empty (:errors form))
      (-> form
          render/create-snippet
          response/unprocessable-content)
      (let [snippet (form->snippet form)
            res (storage/create-snippet store snippet)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)
            session (assoc session :flash "Snippet successfully created!")]
        (-> (response/see-other url)
            (assoc :session session))))))

(defn register [_ _]
  (let [form {}]
    (-> form
        render/register
        response/ok)))

(defn- form->account [{:keys [name email password]}]
  (let [password (bcrypt/encrypt password)
        created (jt/instant)]
    {:name name :email email :password password :created created}))

(defn register-form [store {:keys [form-params session]}]
  (let [form {:name (get form-params "name")
              :email (get form-params "email")
              :password (get form-params "password")}
        form (validate/register-form form)]
    (if (not-empty (:errors form))
      (-> form
          render/register
          response/unprocessable-content)
      (let [account (form->account form)
            {:keys [exists?]} (storage/create-account store account)]
        (if exists?
          (-> form
              (assoc-in [:errors :email] "Email address is already in use")
              render/register
              response/unprocessable-content)
          (let [session (assoc session :flash "Your registration was successful. Please log in.")]
            (-> (response/see-other "/user/login")
                (assoc :session session))))))))

(defn login [store req]
  (response/ok "TODO: login form!"))

(defn login-form [store req]
  (response/ok "TODO"))

(defn logout-form [store req]
  (response/ok "TODO"))
