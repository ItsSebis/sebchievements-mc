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

### nice_to_mace_you (vanilla)
- [ ] Craft a mace (heavy core + breeze rod) but don't hit anything: not granted.
- [ ] Hit a mob with a mace without falling (no smash): not granted.
- [ ] Hit a mob with a mace after falling 3+ blocks (smash attack): granted.
- [ ] Smash attack with a mace you never crafted (`/give`): not granted (crafting is required).

### commuter (vanilla)
- [ ] Enter the Nether, walk 1,000 blocks (= 8,000 Overworld blocks), return: not granted.
- [ ] Walk 1,250+ Nether blocks in one trip and return: granted. (`/tp` in the Nether does not count.)

### off_the_map (vanilla)
- [ ] `/tp` to x=19,990 in the Overworld: not granted. Walk to x=20,000: granted. Same for x<=-20,000, z>=20,000, z<=-20,000.
- [ ] Nether at x=20,000: not granted (Overworld only).

### far_end (vanilla)
- [ ] End at x=9,990: not granted. End at x/z beyond +-10,000: granted. Overworld at 10,000: not granted.

Note on /tp: the location triggers read the player's position, so `/tp` to the target does grant them. That is fine
for a private SMP, but remember it when testing; for commuter, teleporting does not build up travel distance.

## Phase 3: silly (plugin handlers)

Tip: `/adv debug <id>` shows stored state; `/adv progress` lists tracked ones; `/adv reset <player> <id>` clears state.
Timed ones pause while AFK (default `afk_seconds: 120` in config.yml; lower it to test quickly).

### professional_napper (counter, 100 nights)
- [ ] Sleep through the night with 2 players in bed, one stays awake: only the sleepers' `counter/professional_napper` rises (`/adv debug`).
- [ ] Skip a night in the Nether/End via a time-set command: no count. Creative player in bed: no count.
- [ ] Set `nights: 2` in config, restart: second skipped night grants it.

### its_fine (timed, 60s at <= 0.5 hearts)
- [ ] `/damage` yourself down to half a heart (e.g. `/effect give @s instant_damage`, regen off) and wait: `/adv progress` counts up.
- [ ] Heal above half a heart: timer resets to 0. Stand still past `afk_seconds`: timer pauses (not reset). Ride a boat: pauses. Creative: pauses.
- [ ] Die: resets. Log out and back in: keeps its value.
- [ ] Reaches 60s: granted once.

### hoarder
- [ ] Double chest with 53 stacks of 64 dirt and one stack of 63: close it, nothing. Fix it, close: granted.
- [ ] Single chest full of dirt: nothing. Double chest of 64 cobblestone: nothing.

### fashion_disaster
- [ ] Wear leather, iron, gold, diamond pieces: granted on the 4th equip. Four iron pieces: nothing. Elytra in the chest slot: nothing.

### creeper_whisperer
- [ ] Stand 2 blocks from a creeper while it explodes, behind a raised shield: granted (final damage 0).
- [ ] Take damage from the blast: nothing. 10 blocks away: nothing. Creative: nothing.

### self_own
- [ ] Shoot an arrow straight up and die when it lands (low health): granted. Killed by a skeleton's arrow: nothing.

### gravity_check
- [ ] Die from a fall with a water bucket in the hotbar: granted. Bucket only in main inventory or offhand: nothing. Die to a zombie with a bucket: nothing.

### staring_contest (timed, 10s)
- [ ] Carved pumpkin on head, look at an Enderman (`/summon enderman`): progress counts. Look away: resets. Pumpkin off: resets.
- [ ] Wearing a regular pumpkin block or no helmet: nothing.

### one_mans_trash (streak, 10 junk)
- [ ] Set `count: 3`, fish: three junk catches in a row grant it; a fish in between resets (`/adv progress`).
- [ ] Enchanted fishing rod catch resets the streak; a plain damaged rod counts as junk.
- [ ] AFK fishing (past `afk_seconds`): catches are ignored.

## Phase 4: social (plugin handlers)

Most of these need two players (or one player plus an alt account). `/adv debug <id> [player]` shows stored state.

### neighborly
- [ ] A drops an item (Q), B picks it up: B gets it. A picking up their own drop: nothing. Item from a chest or mob drop: nothing.
- [ ] `/adv debug neighborly` shows the partner UUID for both players (used by Betrayal).

### roommates
- [ ] A and B sleep in beds 3 blocks apart: both granted (whoever lies down second). 7 blocks apart: nothing.
- [ ] A sleeping and B in spectator nearby: nothing for B.

### betrayal
- [ ] A and B trade (neighborly), then A kills B: A granted. A kills C (never traded): nothing. B kills A: B granted (the link is both ways).

### group_project
- [ ] 5 players (or 4 + alts) within 16 blocks: all granted. With 3 others: nothing. A spectator among them does not count.

### bodyguard
- [ ] A zombie hits B, A kills it within 5s: A granted. After 6s: nothing. A kills a zombie that only hit A: nothing.

### medic
- [ ] B at 3 hearts, A throws a splash Healing (or Regeneration) at B: A granted. B at 5 hearts: nothing. Throwing at yourself: nothing.
- [ ] A splash of Strength on a hurt B: nothing.

### revenge
- [ ] B kills A, then A kills B: A granted. A kills someone else: nothing. After the revenge kill the memory is cleared (`/adv debug revenge`).

### witness
- [ ] B stands 10 blocks from A, A earns any advancement with a toast: B granted. 30 blocks away: nothing.
- [ ] A earning a recipe or a tab root, or an advancement of ours called Witness: nothing.

### twinning
- [ ] A and B both in full iron armor within 8 blocks: granted. A in iron, B in iron with a diamond helmet: nothing. One slot empty: nothing.

### dinner_party
- [ ] Hungry A, B, C each eat from the same cake within 60s: all three granted. Two players: nothing. Third bite after 60s: nothing.
- [ ] Full-hunger player right-clicking the cake does not count. Creative players do not count.
