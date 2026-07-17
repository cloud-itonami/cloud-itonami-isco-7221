(ns forgeworker.governor
  "ForgeWorkerGovernor — the independent safety/scope layer gating
  every forge-shop scheduling/logistics proposal an advisor may make
  for a blacksmithing/forging-press crew. The governor never dispatches
  hardware itself, never performs forging work at the forge shop, and
  never finalizes a forging-execution decision (e.g. deciding to
  proceed with a specific hammer-strike or press-stroke) or overrides a
  forge-shop safety officer's judgment — those are permanently out of
  this actor's scope and remain the forge-shop safety officer's
  exclusive judgment (README's 'Robotics premise': this actor
  coordinates FORGE-SHOP SCHEDULING/LOGISTICS ONLY — it never performs
  forging work itself). Modeled on cloud-itonami-isco-7111's
  housebuilder.governor (the closest domain shape in this wave — same
  physical-safety-domain scaffold).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. worker provenance     — the crew member must be independently
                                verified/registered before any action.
    2. forge-shop provenance — the forge shop must be independently
                                verified/registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs forging work itself; it
                                only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                forging-execution decision (e.g.
                                deciding to proceed with a specific
                                hammer-strike or press-stroke), or to
                                override a forge-shop safety officer's
                                judgment, is a hard, permanent block
                                (checked both against the proposed :op
                                and, defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"proceed with the
                                forging operation\" / \"authorize the
                                press operation\" / \"override the
                                forge-shop safety officer's judgment\",
                                never as bare nouns like \"forging\",
                                \"press\" or \"safety\", so the check
                                can never self-trip on the advisor's own
                                routine rationale text, e.g. \"logged
                                work record for worker …\" or
                                \"scheduled crew operation for
                                furnace-heating task …\" or \"…routed
                                for forge-shop safety officer review\" —
                                all three legitimately contain those
                                bare nouns but none is a finalization
                                action, and all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a heat-exposure / press-crush /
                                equipment-condition concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [forgeworker.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-forging-decision :authorize-press-operation
    :proceed-with-forging-operation
    :override-forge-shop-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("forging", "press", "hammer", "safety") — so this can never match
;; inside the mock advisor's own default rationale text (which
;; legitimately contains those bare nouns, e.g. "furnace-heating task"
;; / "forge-shop safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the forging operation" "proceed with the press operation"
   "finalize the forging decision" "authorize the press operation"
   "finalize the press stroke decision"
   "override the forge-shop safety officer's judgment"
   "override the forge shop safety officer's judgment"
   "override forge-shop safety officer judgment"
   "override forge shop safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower-case (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal worker-record shop-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? worker-record)
      (conj {:rule :no-worker
             :detail "未登録 worker への提案は不可（worker record は独立して検証・登録済みでなければならない）"})

      (nil? shop-record)
      (conj {:rule :no-shop
             :detail "未登録 forge shop への提案は不可（forge shop record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は現場作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "鍛造作業の実行判断（ハンマー打撃/プレス工程等）の確定・forge-shop safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `forgeworker.store/Store`. Pure — never mutates
  the store, never dispatches a forge-shop operation."
  [request _context proposal store]
  (let [worker-record (store/worker store (:worker-id request))
        shop-record (some->> (:shop-id proposal) (store/forge-shop store))
        hard (hard-violations proposal worker-record shop-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
