(ns todomvc.enflame ; #todo => re-state ???
  (:require
    [re-frame.core :as rf]
    [re-frame.loggers :as rflog]
    [re-frame.interceptor :as rfi]))


(defn dissoc-in
  "A sane version of dissoc-in that will not delete intermediate keys.
   When invoked as (dissoc-in the-map [:k1 :k2 :k3... :kZ]), acts like
   (clojure.core/update-in the-map [:k1 :k2 :k3...] dissoc :kZ). That is, only
   the map entry containing the last key :kZ is removed, and all map entries
   higher than kZ in the hierarchy are unaffected."
  [the-map keys-vec ]
  (let [num-keys     (count keys-vec)
        key-to-clear (last keys-vec)
        parent-keys  (butlast keys-vec)]
    (cond
      (zero? num-keys) the-map
      (= 1 num-keys) (dissoc the-map key-to-clear)
      :else (update-in the-map parent-keys dissoc key-to-clear))))

;---------------------------------------------------------------------------------------------------
(def ascii-code-return 13) ; #todo => tupelo.ascii
(def ascii-code-escape 27)
(defn event-val [event]  (-> event .-target .-value))

(defn from-topic [topic] @(rf/subscribe topic)) ; #todo was (listen ...)

(defn get-in-strict [map path]
  (let [result (get-in map path ::not-found)]
    (when (= result ::not-found)
      (throw (ex-info "get-in-strict: path not found" {:map map :path path})))
    result))

(defn get-and-swap! ; #todo => tupelo.core.cljc
  "Gets the current value of an atom,  pplies clojure.core/swap! to an atom,
  Returns the OLD value."
  [tgt-atom swap-fn]
  (loop [old-val @tgt-atom
         new-val (swap-fn old-val)]
    (if (compare-and-set! tgt-atom old-val new-val)
      old-val
      (recur old-val new-val))))

;---------------------------------------------------------------------------------------------------

; #todo need macro  (definterceptor todos-done {:name ...   :enter ...   :leave ...} )

(defn event-handler-for!
  [event-id interceptor-chain handler]
  (when-not (keyword? event-id) (throw (ex-info "illegal event-id" event-id)))
  (when-not (vector? interceptor-chain) (throw (ex-info "illegal interceptor-chain" interceptor-chain)))
  (when-not (every? map? interceptor-chain) (throw (ex-info "illegal interceptor" interceptor-chain))) ; #todo detail intc map
  (when-not (fn? handler) (throw (ex-info "illegal handler" handler)))
  (rf/reg-event-fx event-id interceptor-chain handler))

(defn dispatch-event [& args] (apply rf/dispatch args) )

(defn dispatch-event-sync [& args] (apply rf/dispatch-sync args) )

(defn define-topic! [& forms] (apply rf/reg-sub forms))

; #todo need macro  (with-path ctx [:db :todos] ...) ; extract and replace in ctx
; #todo need macro  (with-db ctx ...) ; hardwired for path of [:db]

; #todo maybe macro  (with-result some-val ...) always returns some-val (like identity-with-side-effects)

; #todo remember this (modify into `(definterceptor trim-event { ... } )`
(comment
  (def trim-event
    (re-frame.core/->interceptor ; takes a naked map
      :id     :trim-event
      :before (fn [context]
                (let [trim-fn (fn [event] (-> event rest vec))]
                  (update-in context [:coeffects :event] trim-fn)))))
  )

 ; #todo definterceptor-state (auto-set :id same as name)
(defn interceptor-state ; #todo need test
  "Creates a simple interceptor that accepts & returns state. Usage:

      (interceptor-state { :id    :some-intc
                           :enter (fn [& args] ...)
                           :leave (fn [& args] ...) } )

  NOTE: enflame uses Pedestal-style `:enter` & `:leave` keys for the interceptor map.
  "
  [map-in]         ; #todo :- tsk/KeyMap
  (js/console.log :map-in  map-in)
  (let [enter-fn  (get-in-strict map-in [:enter])
        leave-fn  (get-in-strict map-in [:leave])
        before-fn (fn [ctx] (update-in ctx [:coeffects] enter-fn))
        after-fn  (fn [ctx] (update-in ctx [ :effects]  leave-fn))]
  {:id (get-in-strict map-in [:id])
   :before before-fn
   :after  after-fn}))
; #todo allow one of :enter or :leave to be blank => identity
; #todo add :error key like pedestal?

;---------------------------------------------------------------------------------------------------
; tracing interceptor (modified rfstd/debug

(def trace
  "An interceptor which logs/instruments an event handler's actions to
  `js/console.log`. See examples/todomvc/src/events.cljs for use.
  Output includes:
  1. the event vector
  2. orig db
  3. new db
  "
  (rfi/->interceptor ; #todo convert to interceptor-state
    :id     ::trace
    :before (fn debug-before
              [context]
              (rflog/console :log "Handling re-frame event:" (rfi/get-coeffect context :event))
              context)

    :after  (fn debug-after ; #todo => (with-result context ...)
              [context]
              (let [event   (rfi/get-coeffect context :event)
                    db-orig (rfi/get-coeffect context :db)
                    db-new  (rfi/get-effect   context :db ::not-found)]
                (do (rflog/console :group "db for:" event)
                    (rflog/console :log :before db-orig)
                    (rflog/console :log :after db-new)
                    (rflog/console :groupEnd))
                context))))

