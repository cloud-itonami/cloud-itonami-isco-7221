# cloud-itonami-isco-7221

Open Occupation Blueprint for **ISCO-08 7221**: Blacksmiths, Hammersmiths and Forging Press Workers.

This repository designs a forkable OSS business for a forge-shop scheduling and logistics coordination practice: a forge-shop scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a blacksmithing/forging-press crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/forgeworker/` implements the
`ForgeWorkerActor` as a `langgraph.graph/state-graph`
(`forgeworker.actor`) wired to a `Forge Worker Advisor`
(`forgeworker.advisor`) and an independent `ForgeWorkerGovernor`
(`forgeworker.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 22 tests / 48 assertions green (`clojure -M:test`).
HARD invariants (always hold, never overridable): worker provenance,
forge-shop provenance, no-actuation (`:effect` must be `:propose`), a
closed op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a forging-execution decision
(e.g. deciding to proceed with a specific hammer-strike or
press-stroke) or override a forge-shop safety officer's judgment.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a forge-shop scheduling/logistics coordination robot performs crew scheduling, task/materials-usage/progress-record logging and forging-materials supply-order coordination for a blacksmithing/forging-press crew, under an actor that proposes actions and an independent **Forge Worker Governor** that gates them. The governor never
dispatches hardware itself, never performs forging work at the forge shop, and never finalizes a forging-execution decision or overrides a forge-shop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged heat-exposure/press-crush/equipment-condition concern, or an above-threshold supply order) require human sign-off. **This actor coordinates forge-shop scheduling/logistics only — it never performs forging work itself.**

## Core Contract

```text
crew roster + forge-shop registration + safety-reporting policy
        |
        v
Forge Worker Advisor -> Forge Worker Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a forging-execution decision, override a forge-shop safety officer's
judgment, suppress an operating record, or disclose sensitive data
without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7221`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
