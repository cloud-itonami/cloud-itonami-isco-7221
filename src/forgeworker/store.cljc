(ns forgeworker.store
  "SSoT for the ISCO-08 7221 forge-shop scheduling/logistics
  coordination actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md
  Actors section; README's 'Robotics premise' — a forge-shop
  scheduling/logistics coordination robot performs crew scheduling,
  task/materials-usage/progress-record logging and forging-materials
  supply-order coordination for a blacksmithing/forging-press crew
  under this advisor/governor pair, which never dispatches hardware
  itself, never performs forging work itself, and never finalizes a
  forging-execution decision (e.g. a specific hammer-strike or
  press-stroke) or overrides a forge-shop safety officer's judgment —
  that remains the forge-shop safety officer's exclusive judgment).
  Modeled on cloud-itonami-isco-7111's housebuilder.store (the closest
  domain shape in this wave — same physical-safety-domain scaffold).

  Domain:

    worker    — a registered forge-shop crew member (blacksmith,
                hammersmith or forging press worker)
                (:worker-id, :name)
    forgeshop — a registered forge shop / forging press line {:shop-id
                :name :max-supply-cost number}. `:max-supply-cost` is
                an informational registered ceiling used only to
                decide whether a `:coordinate-supply-order` proposal
                escalates to human sign-off (the governor never blocks
                a within-threshold order outright; it only decides
                commit vs. escalate).
    record    — a committed operating record (a logged
                task/materials-usage/progress entry, a scheduled crew
                operation, a flagged safety concern, or a coordinated
                supply order) — written ONLY via commit-record!.
    ledger    — append-only audit trail, commit or hold.")

(defprotocol Store
  (worker [s worker-id])
  (forge-shop [s shop-id])
  (records-of [s worker-id])
  (ledger [s])
  (register-worker! [s worker])
  (register-forge-shop! [s shop])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (worker [_ worker-id] (get-in @a [:workers worker-id]))
  (forge-shop [_ shop-id] (get-in @a [:forge-shops shop-id]))
  (records-of [_ worker-id] (filter #(= worker-id (:worker-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-worker! [s w]
    (swap! a assoc-in [:workers (:worker-id w)] w) s)
  (register-forge-shop! [s sh]
    (swap! a assoc-in [:forge-shops (:shop-id sh)] sh) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:workers {} :forge-shops {} :records [] :ledger []}
                                    seed)))))
