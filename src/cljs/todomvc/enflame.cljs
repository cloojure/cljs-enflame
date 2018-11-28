(ns todomvc.enflame ; #todo => re-state ???
  (:require
    [ajax.core :as ajax]
    [clojure.set :as set]
    [re-frame.core :as rf]
    [re-frame.db :as rfdb]
    [re-frame.events :as rfe]
    [re-frame.loggers :as rflog]
    [re-frame.router :as rfr]
    [tupelo.core :as t]
    [tupelo.char :as char]
  ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

;---------------------------------------------------------------------------------------------------
; #todo (definterceptor my-intc  ; added to :id field as kw
; #todo   "doc string"
; #todo   {:enter (fn [ctx] ...)       to match pedestal
; #todo    :leave (fn [ctx] ...) } )
; #todo event handlers: document that `state` is coeffects/effects (ignore the difference)
;     coeffects  =>  state-in
;       effects  =>  state-out

; #todo   maybe rename interceptor chain to intc-chain, proc-chain, transform-chain

; #todo   unify [:dispatch ...] effect handlers
; #todo     {:do-effects [  ; <= always a vector param, else a single effect
; #todo        {:effect/id :eff-tag-1  :par1 1  :par2 2}
; #todo        {:effect/id :eff-tag-2  :effect/delay {:value 200  :unit :ms} ;
; #todo         :some-param "hello"  :another-param :italics } ] }

; #todo make all routes define an intc chain.

; #todo [:delete-item 42] => {:event/id :delete-item :idx 42}
; #todo   {:event/id :set-timer  :units :ms :value 50 :action (fn [] (js/alert "Expired!") }

; #todo (dispatch-event {:event/id <some-id> ...} )   => event map
; #todo (add-task state {:effect/id <some-id> ...} )  => updated state

; #todo setup, prep, resources, augments, ancillary, annex, ctx, info, data
; #todo environment, adornments, supplements

; #todo teardown, completion, tasks, commands, orders

;---------------------------------------------------------------------------------------------------
; #todo Need a way to document event names and args
; #todo    (defevent set-showing [ctx])
; #todo event handlers take only params-map (fn [params :- tsk/Map] ...)  ; params => {:state <state>   :event <event>}

; #TODO CHANGE ALL EVENTS to be maps => {:id :set-showing   :new-filter-kw :completed ...}
; #TODO CHANGE ALL HANDLERS to be (defn some-handler [ctx event]   (with-map-vals event [id new-filter-kw] ...)
;---------------------------------------------------------------------------------------------------

;---------------------------------------------------------------------------------------------------
(defn event-val [event]  (-> event .-target .-value))

(defn fragment-value [fragment-id] @(rf/subscribe fragment-id)) ; #todo was (listen ...)

;---------------------------------------------------------------------------------------------------
(defonce ctx-trim-queue-stack (atom true))
(defn ctx-trim [ctx]
  "Removes `:queue` and `:stack` entries from the context map to declutter printing"
  (if @ctx-trim-queue-stack
    (dissoc ctx :queue :stack)
    ctx))

; #todo macro definterceptor (auto-set :id same as name)
(defn interceptor ; #todo need test
  "Creates a simple interceptor that accepts & returns state. Usage:

      (flame/interceptor { :id    :some-intc
                           :enter (fn [& args] ...)
                           :leave (fn [& args] ...) } )

  NOTE: enflame uses Pedestal-style `:enter` & `:leave` keys for the interceptor map.
  "
  [map-in]         ; #todo :- tsk/KeyMap
  {:id     (t/grab :id map-in)
   :before (t/grab :enter map-in)
   :after  (t/grab :leave map-in)})
; #todo allow one of :enter or :leave to be blank => identity
; #todo add :error key like pedestal?

(def event-dispatch-intc
  (interceptor      ; #todo need test
    {:id    :dispatch-all-intc
     :enter identity
     :leave (fn [ctx] ; #todo (with-result ctx ...)
              ;(println :dispatch-all-intc :enter (ctx-trim ctx))
              (let [dispatch-cmd      (let [cmd (:dispatch ctx)]
                                        (if cmd [cmd] []))
                    dispatch-n-cmds   (get ctx :dispatch-n [])
                    dispatch-all-cmds (get ctx :dispatch-all [])
                    dispatch-cmds     (t/glue dispatch-cmd dispatch-n-cmds dispatch-all-cmds)]
                ;(println :dispatch-all-intc :dispatch-cmds dispatch-cmds)
                (doseq [dispatch-cmd dispatch-cmds]
                  (if-not (vector? dispatch-cmd)
                    (rflog/console :error "dispatch-all-intc: bad dispatch-cmd=" dispatch-cmd)
                    (rfr/dispatch dispatch-cmd))))
              ;(println :dispatch-all-intc :leave)
              ctx)}))

(def app-state-intc
  (interceptor
    {:id    :app-state-intc
     :enter (fn [ctx]
              ;(js/console.info :app-state-intc-enter :begin (ctx-trim ctx))
              (let [ctx-out (-> ctx
                              (into {:data/type :enflame/context
                                     :app-state @rfdb/app-db
                                     ; KLUDGE: Move `:event` from :coeffects sub-map to parent `context` map.
                                     ; KLUDGE:   Allows us to use unmodified re-frame as "hosting" lib
                                     :event     (t/fetch-in ctx [:coeffects :event])})
                              (dissoc :coeffects))]
                ;(js/console.info :app-state-intc-enter :end (ctx-trim ctx-out))
                ctx-out))
     :leave (fn [ctx]
              ;(println :app-state-intc-leave :begin (ctx-trim ctx))
              (let [app-state (t/grab :app-state ctx)]
                (when-not (identical? @rfdb/app-db app-state)
                  (println :app-state-intc-leave "resetting rfdb/app-db atom...")
                  (reset! rfdb/app-db app-state))))}))

(def ajax-options-keys
  "Options map keys accepted by cljs-ajax"
  #{:body :cookie-policy :error-handler :finally :format :handler :headers :params
    :progress-handler :response-format :timeout :url-params :with-credentials})

(def ajax-intc
  (interceptor
    {:id    :ajax-intc
     :enter identity
     :leave (fn [ctx] ; #todo (with-result ctx ...)
              (let [ajax (:ajax ctx)]
                (t/spyx :ajax-intc-start ctx)
                (t/spyx :ajax-intc-start ajax)
                (when-not (nil? ajax)
                  (let [method            (t/grab :method ajax)
                        uri               (t/grab :uri ajax)
                        ajax-opts-present (set/intersection (set (keys ajax)) ajax-options-keys)
                        opts-map          (t/submap-by-keys ajax ajax-opts-present)]
                    (t/spy :ajax-intc-ready (t/vals->map method uri opts-map))
                    (condp = method
                      :get (ajax/GET uri opts-map)
                      :put (ajax/PUT uri opts-map)
                      :post (ajax/POST uri opts-map)
                      (throw (ex-info "ajax-intc: unrecognized :method" ajax))))))
              ctx)}))


;---------------------------------------------------------------------------------------------------
; #todo need macro  (definterceptor todos-done {:name ...   :enter ...   :leave ...} )

(defn event-handler-for!
  [event-id interceptor-chain handler-fn]
  (when-not (keyword? event-id) (throw (ex-info "illegal event-id" event-id)))
  (when-not (vector? interceptor-chain) (throw (ex-info "illegal interceptor-chain" interceptor-chain)))
  (when-not (every? map? interceptor-chain) (throw (ex-info "illegal interceptor" interceptor-chain))) ; #todo detail intc map
  (when-not (fn? handler-fn) (throw (ex-info "illegal handler-fn" handler-fn)))
  (let [handler-intc (interceptor
                       {:id    event-id
                        :enter (fn [ctx]
                                 (let [event   (t/grab :event ctx)
                                       ctx-out (handler-fn ctx event)]
                                   ctx-out))
                        :leave identity})]
    (rfe/register event-id
      [app-state-intc event-dispatch-intc interceptor-chain handler-intc])))

; #todo need plumatic schema:  event => [:kw-evt-name & args]
(defn dispatch-event [& args] (apply rf/dispatch args) )

(defn dispatch-event-sync [& args] (apply rf/dispatch-sync args) )

;****************************************************************
; Define built-in :app-state fragment
(rf/reg-sub :app-state (fn [app-state -query-] app-state)) ; loaded from rfdb/app-db ratom
;****************************************************************

; #todo macro to insert fragment as fn-name;  :sorted-todos => (fn sorted-todos-fn ...)
; #todo (flame/define-fragment! :sorted-todos ...) => (fn sorted-todos-fn ...)
(defn define-fragment!
  [fragment-id input-fragments tx-fn]
  (when-not (keyword? fragment-id) (throw (ex-info "fragment-id must be a keyword" fragment-id)))
  (when-not (vector? input-fragments) (throw (ex-info "input-fragments must be a vector" input-fragments)))
  (when-not (every? keyword? input-fragments) (throw (ex-info "fragment values must be keywords" input-fragments)))
  (when-not (fn? tx-fn) (throw (ex-info "tx-fn must be a function" tx-fn)))
  (let [sugar-forms (vec (apply concat
                           (for [input-fragment input-fragments]
                             [:<- [input-fragment]])))
        args-vec    (vec (concat [fragment-id] sugar-forms [tx-fn]))]
    (apply rf/reg-sub args-vec)))

; #todo need macro  (with-path state [:app-state :todos] ...) ; extract and replace in ctx

; #todo macro  (with-result some-val ...) always returns some-val (like identity-with-side-effects)

;---------------------------------------------------------------------------------------------------
; tracing interceptor (modified rfstd/debug

(def trace
  "An interceptor which logs/instruments an event handler's actions to
  `js/console.log`. See examples/todomvc/src/events.cljs for use.
  Output includes:
  1. the event vector
  2. orig app-state
  3. new app-state
  "
  (interceptor
    {:id    :trace
     :enter (fn trace-enter ; #todo => (with-result context ...)
              [ctx]
              (let [ctx (ctx-trim ctx)]
                (rflog/console :log "Handling re-frame event:" (t/grab :event ctx))
                (rflog/console :log :trace :enter ctx))
              ctx)

     :leave (fn trace-leave ; #todo => (with-result context ...)
              [ctx]
              (let [ctx (ctx-trim ctx)]
                ;(rflog/console :group "leaving trace-intc...")
                (rflog/console :log :trace :leave ctx)
                ;(rflog/console :groupEnd)
                )
              ctx)}))

(def trace-print
  "An interceptor which logs/instruments an event handler's actions to
  `js/console.log`. See examples/todomvc/src/events.cljs for use.
  Output includes:
  1. the event vector
  2. orig app-state
  3. new app-state
  "
  (interceptor
    {:id    :trace-print

     :enter (fn trace-enter
              [ctx]
              (let [ctx (ctx-trim ctx)]
                (println :trace "Handling re-frame event:" (t/grab :event ctx))
                (println :trace :enter ctx))
              ctx)

     :leave (fn trace-leave ; #todo => (with-result context ...)
              [ctx]
              (let [ctx (ctx-trim ctx)]
                (println :trace :leave ctx))
              ctx)}))

; #todo   => (event-handler-set!    :evt-name  (fn [& args] ...)) or subscribe-to  subscribe-to-event
; #todo   => (event-handler-clear!  :evt-name)
; #todo option for log wrapper (with-event-logging  logger-fn
; #todo                          (event-handler-set! :evt-name  (fn [& args] ...)))


