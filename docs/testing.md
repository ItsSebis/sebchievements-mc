# Manual test checklist

Setup: `gradle deploy` (see CLAUDE.md "Project decisions" for the JDK/Gradle paths), then
`scripts/dev-server.sh restart` (refuses while players are online). Join `~/mc-server` as an op.

Unit tests (no server): `gradle :buildSrc:test test`.

## Framework (Phase 1)

- [ ] Join: the four tabs (Social, Silly, Challenges, Exploration) appear in the advancement screen without a toast or chat message.
- [ ] `/adv test` as op: toast + chat announcement for "Test Advancement"; second run says already earned.
- [ ] `/adv revoke <you> test`, then `/adv test` again: earned again.
- [ ] `/adv grant <you> tax_evasion`: granted; `/adv revoke <you> tax_evasion`: revoked.
- [ ] `/adv grant <you> nope`: "Unknown advancement id".
- [ ] Tab completion of `<id>` lists catalog ids.
- [ ] As a non-op: `/adv progress` works; `/adv grant`, `/adv debug`, `/adv test` are not offered and are rejected.
- [ ] `/adv progress` with no timed/streak advancements: "No timed or streak advancements yet."
- [ ] `/adv debug test`: prints `(no stored state)`.
- [ ] Startup log shows `Registered N of N plugin handlers` and no SEVERE lines.
- [ ] Website (`:8123`): `smp:silly/test` and `smp:silly/tax_evasion` listed under "SMP" (needs the SebStats label fix deployed).

## Per advancement

Add one section per advancement as it is implemented (CLAUDE.md section 8, step 6): setup, steps, expected
result, and what must NOT count (the `anti_cheese` note).

### tax_evasion (vanilla)
- [ ] Kill a Wandering Trader (`/summon wandering_trader`): advancement granted.
- [ ] Kill a regular villager: nothing.
