# Playtest guide

One session, riskiest first. Nothing here has been played yet: the build, the unit tests, the datapack load and the
reward item data are verified, but no handler has been triggered by a real player. Expect to find bugs, and note
them in the "Result" column as you go.

Per-advancement detail and edge cases live in `docs/testing.md`. This page is the order to do it in, the exact
setup, and what each step proves.

## Setup (10 minutes)

1. `gradle deploy` then `scripts/dev-server.sh restart` (it refuses while players are online). Join `~/mc-server`
   and make sure you are an op (`ops.json`).
2. Switch to survival: `/gamemode survival`. Creative and spectator are deliberately excluded by most handlers,
   so testing in creative is the most common way to see "nothing happens".
3. Open a second client or an alt account for the social advancements (Part 5). Everything else is solo.
4. Optional but strongly recommended: shorten the long timers. Edit `~/mc-server/plugins/Sebchievements/config.yml`
   (the deployed copy, not the repo default), set the values below, restart, and **put the real values back
   afterwards** (they are listed in the repo's `src/main/resources/config.yml`).

   ```yaml
   afk_seconds: 20
   advancements:
     its_fine: { seconds: 15, max_health: 1.0 }
     untouchable: { seconds: 30 }
     staring_contest: { seconds: 5, range: 32 }
     iron_will: { seconds: 30 }
     frozen_human: { seconds: 10 }
     free_diver: { seconds: 20, reset_on_surface: false }
     caveman: { ticks: 1200, max_jump_ticks: 400 }
     pacifist: { nights: 1, night_start: 13000, night_end: 23000, slack_ticks: 40 }
     one_mans_trash: { count: 1, junk: [LILY_PAD, BOWL, FISHING_ROD, LEATHER, LEATHER_BOOTS, ROTTEN_FLESH, STICK, STRING, POTION, BONE, INK_SAC, TRIPWIRE_HOOK] }
     professional_napper: { nights: 2 }
   ```
   Keep the other sections as they are (replace only these keys; do not delete the rest of the file).

Useful commands all session: `/adv progress`, `/adv debug <id>`, `/adv grant|revoke|reset <you> <id>`,
`/gamerule doDaylightCycle false`, `/time set 13000`, `/effect give @s ...`, `/summon ...`, `/kill`, `/tp`.

---

## Part 1: rewards actually arrive (5 min) - do this first

This is the only path no automated check covers. If it is broken, every later grant looks wrong.

| # | Do | Expect | Result |
|---|---|---|---|
| 1.1 | `/adv grant <you> tax_evasion` | Toast + chat message, and a **Tax Refund** emerald in your inventory with a grey "Sebchievement: Tax Evasion" line | |
| 1.2 | `/adv revoke <you> tax_evasion`, then grant it again | You get a second emerald (a re-grant gives the reward again, by design) | |
| 1.3 | Fill all 36 inventory slots, grant `tax_evasion` once more | The emerald drops at your feet instead of vanishing | |
| 1.4 | `/adv grant <you> actual_sniper` | Bow "Long Shot" with Power VII, Infinity and Mending on the same bow; try shooting it | |
| 1.5 | `/adv grant <you> twinning` | **Two** red leather caps named "Matching Hats" | |
| 1.6 | `/adv grant <you> jockey_jamboree` | Saddle and carrot on a stick, once | |
| 1.7 | `/adv test` | No item (the smoke test has no reward); tab roots give nothing either | |

Also check the SebStats page shows these as earned for you (`localhost:8123`), under "SMP" once that jar is deployed.

## Part 2: framework (5 min)

| # | Do | Expect | Result |
|---|---|---|---|
| 2.1 | Join fresh (or relog) | All four tabs appear, no toast or chat spam | |
| 2.2 | `/adv progress` | Lists the timed/streak/counter advancements with a current value, e.g. "It's Fine: 0s / 15s" | |
| 2.3 | `/adv debug its_fine` as op, then as a non-op | Op sees state; non-op is refused | |
| 2.4 | Tab-complete `/adv grant <you> ` | Offers catalog ids | |
| 2.5 | `/adv reset <you> its_fine` | "Reset tracked state" | |

---

## Part 3: timing and event-order risks (30 min) - most likely to have bugs

These depend on server tick order or on real-world conditions, which unit tests cannot cover.

### 3.1 Creeper Whisperer (needs a creeper)
Setup: `/summon creeper ~3 ~ ~` (stay 2-3 blocks away), survival.
- With a shield raised the whole time: it should grant (final damage 0). **Watch for it granting late or not at all:**
  it relies on the creeper's damage event arriving after the explosion event and before the next tick.
- Without a shield, take the blast: nothing. Repeat from 10 blocks: nothing.

### 3.2 Staring Contest
Setup: `/give @s carved_pumpkin`, wear it on your head, `/summon enderman ~5 ~ ~`.
- Stare at it: `/adv progress` should climb 1 per second. Look away for 2 seconds: back to 0.
- Without the pumpkin: nothing. A regular pumpkin block on your head: nothing.
- Stand AFK for `afk_seconds` while staring: should pause, not reset.

### 3.3 Dinner Party (two players)
Setup: place a cake (`/give @s cake`), make both players hungry (`/effect give @s hunger 20 255`, wait).
- Both take a bite within 60 seconds: with `players: 3` you need three people; set `dinner_party.players: 2` in the
  config to test with two. Everyone who ate should get it at once.
- A full-hunger player right-clicking the cake must not count. **This is a guess at how the game reports a bite**,
  so if a clearly valid bite does not register, say so.

### 3.4 It's Fine and Untouchable
- It's Fine: get to half a heart (`/effect give @s instant_damage 1 0` a few times; stop at 1 hp), disable regen by
  staying fed or using `/effect give @s saturation 0`. Hold for 15 s: granted. Heal up: resets. Die: resets.
- Untouchable: wait 30 s: granted. Take any damage (fall 4 blocks): resets. Block a hit fully with a shield:
  must NOT reset.
- Both: ride a boat (`/give @s oak_boat`): paused, not reset. Stand still past 20 s: paused.

### 3.5 Frozen Human and Free Diver
- Frozen Human: stand in powder snow until the blue hearts and frost show. Must stay frozen 10 s. Step out: resets.
  **Watch for it ticking while you are not actually fully frozen** (it uses the game's own "fully frozen" flag).
- Free Diver: dive without aids; `/adv progress` should climb only while your eyes are underwater and **pause (not
  reset) when you surface**. Then test that Water Breathing (`/effect give @s water_breathing 60`), a Respiration
  helmet and a turtle helmet each stop the progress.

### 3.6 Totem Chain and Food Critic
- Totem Chain: `/give @s totem_of_undying 3`, hold one in the offhand, `/damage @s 100` three times within a minute
  re-equipping each time. Third pop grants. Two pops then wait 60 s then a third: nothing.
- Food Critic: `/give @s pufferfish poisonous_potato rotten_flesh spider_eye`, eat all four within 60 s. `/adv progress`
  should switch to a 30 s countdown, then grant. Die during the countdown: cancelled.

### 3.7 Sky Camper and Caveman (game-time based)
- Sky Camper: `/gamerule doDaylightCycle true`, `/time set 12990`, `/tp ~ 210 ~` onto a platform. Stay up there
  the entire night (`/time` must not be set again). **It needs the whole night, so it takes ~8 minutes**: speed it
  up by changing `night_start/night_end` in the config to a 1,000-tick window (for example 13000 to 14000, slack 40).
  Dropping below Y 200 or logging out must reset it.
- Caveman: underground, `/adv progress` climbs; surface (or a glass roof) resets; the Nether counts nothing.
  With `ticks: 1200` it takes one minute of game time.

### 3.8 Pacifist (streak, night bookkeeping)
Setup: `nights: 1`. `/time set 12900` in the Overworld, stay awake, moving at least every 20 s, and do not kill
anything. At dawn it should grant. Then repeat failing on purpose, each of these should leave no streak:
kill a mob (even a cow), log out mid-night, sleep through the night, go to the Nether mid-night, stand AFK.
`/adv debug pacifist` shows the last counted night's day number.

---

## Part 4: bosses and raids (30-60 min, only if you want to) - high effort, medium risk

Do these after the rest; they take the longest and their logic is the simplest.

- **Naked and Afraid:** `/summon wither`, strip all armor slots, kill it (a bow from a safe spot is fine).
  Then repeat wearing one armor piece for a single hit: that Wither must not count for you.
- **Warden Bully:** `/summon warden`, unarmored, melee only. One Warden hit kills an unarmored player, so use a
  1x1 pit/trap. Wearing any armor for one hit, or one arrow, disqualifies you for that Warden.
- **Whittled Down:** in the End, `/give @s wooden_sword`, bring the dragon down with beds/crystals, land the **final**
  blow with the wooden sword. A final blow with anything else must not grant.
- **Wither Storm:** kill three Withers inside 10 minutes (3 skulls each).
- **One-Man Army:** `/effect give @s bad_omen 1000 4` (Bad Omen V), enter a village alone, win the raid. Another
  player within 64 blocks at any moment must disqualify it (a spectator must not).

Quick sanity for the cheap ones without the fight: `/adv grant <you> <id>` still works, and `/adv debug <id>` shows state.

## Part 5: social, two players (20 min)

| Advancement | Do | Expect |
|---|---|---|
| Neighborly | A drops an item (Q), B picks it up | B is granted; `/adv debug neighborly` shows the partner |
| Betrayal | After trading, A kills B (PvP) | A granted. Killing someone you never traded with: nothing |
| Revenge | B kills A, then A kills B | A granted on the second kill |
| Roommates | Beds 3 blocks apart, both sleep | Both granted. 7 blocks apart: nothing |
| Group Project | Everyone within 16 blocks (it needs 4 others, so 5 players, or set `others: 1` to test) | All granted |
| Bodyguard | A zombie hits B, A kills it within 5 s | A granted |
| Medic | B at 3 hearts, A splashes Healing on B | A granted. B above 4 hearts: nothing |
| Witness | B stands 10 blocks from A, A earns an advancement with a toast | B granted |
| Twinning | Both in the same four armor items, within 8 blocks | Granted. Different item in one slot: nothing |

## Part 6: the long ones (spot checks only)

- **Iron Will** (`seconds: 30`): climbs, pauses when AFK or in creative, resets on death, keeps its value across a relog.
- **Professional Napper** (`nights: 2`): sleep through two nights with a bed; `/adv progress` shows 1/2 then grants.
- **Roads Were a Mistake:** build 60+ blocks of blue ice (`/fill`), ride a boat along it. `/adv progress` rises; leaving
  the boat resets. Lower `blocks` to 50 to see it grant.
- **Long Way Home / Off the Map / Far End / Commuter:** `/tp` far away and `/kill` (Long Way Home) or just arrive
  (Off the Map: Overworld only; Far End: End only). Commuter cannot be teleported: it needs real Nether travel.
- **Top to Bottom:** stand on a block at the world's height limit then at the lowest bedrock within 5 minutes.
- **Dimension Hopper:** Overworld to Nether to End inside 10 minutes.
- **Fresh Start%:** die, respawn, craft a diamond pickaxe within 10 minutes. A `/give` pickaxe must not count.
- **One Man's Trash** (`count: 1`): fish until a junk item comes up; a fish before it resets nothing at count 1 so
  also test with `count: 2`.
- Silly one-liners: Hoarder (fill a double chest with 64-stacks of dirt), Fashion Disaster (four armor materials),
  Self-Own (shoot an arrow straight up and die from it), Gravity Check (die from a fall with a water bucket in the hotbar).

---

## When something does not work

1. `scripts/dev-server.sh log` and look for `Sebchievements` lines and stack traces.
2. `/adv debug <id>` shows the stored state; `/adv reset <you> <id>` clears it so you can retry.
3. Check you are in survival, not AFK past `afk_seconds`, and in the right dimension (many are Overworld only).
4. Note the exact steps and what you saw. Include the time of day for the night-based ones.

## After the session

- Restore the real thresholds in `plugins/Sebchievements/config.yml` (or copy the repo's `config.yml` over it).
- Before the first real release, remove the `test` advancement from the catalog.
- If anything felt too easy or too hard, adjust the grades in `docs/difficulty.md` and the catalog.
