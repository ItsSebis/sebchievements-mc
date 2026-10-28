# SMP Advancements – Paper plugin + bundled datapack

Custom advancements for a private Minecraft SMP running on Paper. The project ships as **one plugin jar** that contains its own datapack (advancement definitions) plus the Java logic that awards advancements vanilla triggers can't detect.

The #1 design goal besides correctness: **adding a new advancement later must be as cheap as possible.** Ideally one YAML entry for vanilla-trigger advancements, and one YAML entry + one small class for plugin-driven ones.

---

## 0. Before writing any code: ask Sebi

Ask these in one batch, then record the answers in the "Project decisions" section at the bottom of this file:

1. Exact Paper version on the server (output of `/version`). Match `paper-api` and the Java toolchain to it. Don't guess.
2. Java or Kotlin? (Default: Java.)
3. Language of advancement titles/descriptions (English, German, or both via translation keys)?
4. Namespace (default: `smp`).
5. Typical player count (affects proximity checks like Group Project).

---

## 1. Hard rules (apply to every advancement, now and in the future)

- **No soft locks.** Every advancement must be achievable by any player at any time. No "first player to…", no one-time world events, no lifetime records that can be permanently ruined. Streaks are fine as long as failing resets them.
- **Anti-cheese is part of the spec.** Every timed or streak advancement must state in its YAML entry what does *not* count (see the global policy in §5). BlazeandCave's pack had to patch challenges that could be cheesed by sitting in boats, minecarts, rideable mobs, water or lava, so think about this up front.
- **Player-visible progress.** Every timed or streak advancement must report progress via `/adv progress` (§6).
- **No hardcoded thresholds.** Distances, durations and counts live in `config.yml`, keyed by advancement id. Descriptions shown in-game come from the catalog and must match the defaults.
- **Persistence.** Per-player state goes in the player's PersistentDataContainer via a single `PlayerDataStore` wrapper. No ad-hoc NBT, no external DB.
- **Performance.** No per-player work every tick. Repeating checks run at most every 20 ticks unless an advancement genuinely needs finer resolution, and they share one scheduler (§4).

---

## 2. Architecture

### 2.1 Single source of truth: the catalog

`src/main/catalog/advancements.yml` (path flexible) is the **only** place an advancement's metadata lives. A Gradle task generates the datapack JSON from it at build time, and the generated datapack is packed into the plugin jar.

Example entries:

```yaml
tabs:
  social:      { title: "Social", icon: minecraft:cake, background: minecraft:block/oak_planks }
  silly:       { title: "Silly", icon: minecraft:carved_pumpkin, background: minecraft:block/hay_block_side }
  challenges:  { title: "Challenges", icon: minecraft:wither_skeleton_skull, background: minecraft:block/blackstone }
  exploration: { title: "Exploration", icon: minecraft:compass, background: minecraft:block/stone }

advancements:
  - id: tax_evasion
    tab: silly
    parent: root            # root of the tab
    title: "Tax Evasion"
    description: "Kill a Wandering Trader"
    icon: minecraft:emerald
    frame: task             # task | goal | challenge
    hidden: false
    mode: vanilla           # criteria written inline, no Java needed
    criteria:
      kill:
        trigger: minecraft:player_killed_entity
        conditions:
          entity:                     # 26.3 format: a loot condition, NOT a bare { type: ... } predicate
            type: minecraft:entity_properties
            entity: this
            predicate: { minecraft:entity_type: minecraft:wandering_trader }

  - id: group_project
    tab: social
    parent: root
    title: "Group Project"
    description: "Be within 16 blocks of 5 other players"
    icon: minecraft:bell
    frame: goal
    mode: plugin            # generator emits a single minecraft:impossible criterion named "done"
    anti_cheese: "Spectators don't count"
```

Further catalog fields (implemented in Phase 1):
- `kind: timed | streak`: marks an advancement as tracked. Requires `anti_cheese`, requires `mode: plugin`, and its handler must implement `ProgressReporting` (checked at startup). Generated into `AdvancementIds.TRACKED`.
- `criteria: [a, b, c]` on a `mode: plugin` entry: custom criterion names instead of the single `done` (e.g. Jockey Jamboree). Use `HandlerContext.grantCriterion`.
- `requirements: [[a, b], [c]]` on a `mode: vanilla` entry: explicit OR-groups of criterion names (e.g. Off the Map).
- `parent` may name another advancement id instead of `root`.

**Verify every vanilla trigger/condition JSON against a vanilla advancement from the server jar** (`unzip -p ~/mc-server/versions/26.3/paper-26.3.jar data/minecraft/advancement/<path>.json`). The 26.x formats differ from older versions, and one malformed advancement makes the whole server refuse to start (datapack registry error), not just skip that advancement.

### 2.2 The generator (build time)

A Gradle task (in `buildSrc` or as a small included build) that:

1. Parses the catalog.
2. Validates it and **fails the build** on any of:
   - duplicate ids
   - unknown parent
   - unknown tab
   - invalid frame
   - `mode: vanilla` without criteria
   - timed or streak entries without `anti_cheese`
3. Emits for each tab a root advancement (hidden criterion `minecraft:impossible`, granted on join, with the tab background) and one JSON file per advancement under `data/<namespace>/advancement/<tab>/<id>.json`.
4. Emits `pack.mcmeta` with the correct pack format for the server version. Look it up for the exact version, don't guess.
5. Emits a generated constants class (`AdvancementIds`) so Java code never uses raw id strings.

### 2.3 Shipping the datapack

Preferred: register the bundled datapack through Paper's lifecycle API (datapack discovery in a `paper-plugin.yml` bootstrapper). **Verify against the current Paper docs for the server's version before implementing.**

Fallback if that doesn't work: on enable, copy the datapack into the main world's `datapacks/` folder and tell the admin a restart is needed.

### 2.4 Plugin-driven advancements

```
AdvancementHandler (interface)
  String id()                      // from AdvancementIds
  void register(HandlerContext ctx) // subscribe to events / services
```

- `HandlerContext` gives access to: `grant(Player)`, `PlayerDataStore`, the shared services (§4), and the config section for this id.
- Granting always goes through one `Granter` that calls `player.getAdvancementProgress(adv).awardCriteria("done")`, no-ops if already done, and logs.
- Handlers are registered in **one explicit list** (`Handlers.ALL`). Adding a handler = one class + one line.
- **Startup validation:** every `mode: plugin` id has exactly one handler, and every handler id exists in the loaded advancements (`Bukkit.getAdvancement`). Log loudly and disable the offending handler, not the whole plugin.

### 2.5 Package layout (suggestion)

```
core/        Granter, PlayerDataStore, HandlerContext, Handlers.ALL, validation
services/    shared trackers (§4)
handlers/
  social/  silly/  challenges/  exploration/
command/     /adv
```

---

## 3. Phases (stop and report after each one)

**Phase 0 – Proof of concept (do this first, nothing else).**
Minimal plugin + bundled datapack with one tab root and one `mode: plugin` advancement awarded by `/adv test`. Then verify all three of these:

1. It shows up in the in-game advancement screen.
2. The chat announcement and toast work.
3. It appears on Sebi's existing advancement website plugin. That plugin is *said* to support datapack advancements; this is where we find out.

One public plugin author reported Paper limitations with datapack advancements, so this phase exists to de-risk. If anything fails, stop and explain the options.

**Phase 1 – Framework.**
Catalog, generator with validation, `AdvancementIds`, Granter, PlayerDataStore, shared services, `/adv` command, config loading. Use a local test server via the `run-paper` Gradle plugin.

**Phase 2 – All `mode: vanilla` advancements.**

**Phase 3+ – Plugin advancements, one category per phase.**
Order: silly → social → exploration → challenges.

**Testing.**
- Unit-test the pure logic in services (windows, streaks, timers) without a server.
- Write a manual test checklist per advancement in `docs/testing.md`. Use `/adv` debug subcommands to fast-forward where sensible.

---

## 4. Shared services (build these once, reuse everywhere)

- **TickScheduler**: one repeating task (every 20 ticks) that calls registered per-player checks.
- **TimerService**: "condition held continuously for N seconds" per player and id.
  - Used by: It's Fine, Staring Contest, Frozen Human, Free Diver, Untouchable, Iron Will, Caveman, Sky Camper.
  - Supports: reset, pause (e.g. when offline or AFK, if configured), progress query.
- **StreakService**: counted streaks with reset.
  - Used by: Pacifist (nights), One Man's Trash (junk catches).
- **WindowService**: "N distinct events within T seconds".
  - Used by: Food Critic, Totem Chain, Wither Storm, Dimension Hopper, Top to Bottom, Dinner Party.
- **RecentDamageService**: who or what damaged whom, and when.
  - Used by: Bodyguard, Revenge, Creeper Whisperer, Naked and Afraid, Warden Bully.
- **ProximityService**: nearby players within radius, excluding spectators.
  - Used by: Group Project, Roommates, Twinning, Witness, One-Man Army.
- **AfkService**: player counts as AFK if they haven't moved or rotated meaningfully for `afk_seconds` (config).
- **CounterService**: simple persistent counters.
  - Used by: Professional Napper.

---

## 5. Global anti-cheese policy

Each handler declares which of these apply. Defaults for timed and streak advancements:

- Time doesn't count while AFK (AfkService), in spectator or creative mode, or offline.
- Time doesn't count while riding a vehicle or mob, unless the advancement is about riding.
- Creative or spectator mode at the moment of granting → no grant.
- Logging out pauses timers; dying resets them (unless stated otherwise).

---

## 6. `/adv` command

- `/adv progress`: lists every timed or streak advancement for the calling player with current progress, e.g. "Untouchable: 12m 40s / 20m".
- Admin (permission `smpadv.admin`):
  - `grant`, `revoke`, `reset <player> <id>`
  - `debug <id>` to dump tracked state
  - `test` (Phase 0)

---

## 7. Advancement catalog (initial set)

Frame: T = task, G = goal, C = challenge. "Vanilla" = solvable with datapack JSON alone; "Plugin" = handler needed. Thresholds are defaults and belong in config.

### Social
| id | Title | Requirement | Frame | Mode / notes |
|---|---|---|---|---|
| neighborly | Neighborly | Pick up an item another player threw | T | Plugin: `EntityPickupItemEvent`, `Item#getThrower()` ≠ picker. Also records the pair as "traded" for Betrayal |
| roommates | Roommates | Sleep within 5 blocks of another sleeping player | T | Plugin: on bed enter, check sleeping players within 5 |
| betrayal | Betrayal | Kill a player you've traded with | G | Plugin: "traded" = either picked up an item thrown by the other (stored as UUID set in PDC) |
| group_project | Group Project | Be within 16 blocks of 5 other players at once | G | Plugin: ProximityService, spectators excluded, count configurable |
| bodyguard | Bodyguard | Kill a mob that damaged another player in the last 5s | T | Plugin: RecentDamageService |
| medic | Medic | Heal a player below 4 hearts with a splash potion | T | Plugin: `PotionSplashEvent`, Healing or Regeneration, target ≠ thrower, health < 8 before |
| revenge | Revenge | Kill the player who last killed you | G | Plugin: store last killer UUID |
| witness | Witness | Be within 16 blocks when another player earns an advancement | T | Plugin: `PlayerAdvancementDoneEvent`; ignore recipes, roots, and advancements without display |
| twinning | Twinning | Wear the exact same 4 armor pieces as a player within 8 blocks | T | Plugin: compare item types of all 4 slots, all filled |
| dinner_party | Dinner Party | 3 players eat from the same cake within 60s | G | Plugin: track eaters per cake block location; all 3 get it |

### Silly
| id | Title | Requirement | Frame | Mode / notes |
|---|---|---|---|---|
| professional_napper | Professional Napper | Skip 100 nights | G | Plugin: `TimeSkipEvent` NIGHT_SKIP, credit sleeping players, CounterService |
| its_fine | It's Fine | Survive 60s continuously on half a heart or less | G | Plugin: TimerService, health ≤ 1.0; anti-cheese: not AFK |
| hoarder | Hoarder | Fill a double chest entirely with full stacks of dirt | T | Plugin: on inventory close of a double chest, all 54 slots = 64 dirt |
| tax_evasion | Tax Evasion | Kill a Wandering Trader | T | Vanilla |
| fashion_disaster | Fashion Disaster | Wear four different armor materials | T | Plugin (vanilla predicates can't express "all different" cleanly) |
| creeper_whisperer | Creeper Whisperer | A creeper explodes within 3 blocks of you and you take no damage | G | Plugin: record candidates on explode, remove anyone with final damage > 0 from that creeper, grant next tick. Blocking with a shield counts (final damage 0) |
| self_own | Self-Own | Get killed by your own arrow | T | Plugin: death by arrow whose shooter is the victim |
| gravity_check | Gravity Check | Die of fall damage with a water bucket in your hotbar | T | Plugin |
| staring_contest | Staring Contest | Look at an Enderman for 10s while wearing a carved pumpkin | T | Plugin: ray-trace every check, TimerService |
| one_mans_trash | One Man's Trash | Fish up junk 10 times in a row | G | Plugin: `PlayerFishEvent`, junk item list in config, StreakService (fish or treasure resets) |

### Challenges
| id | Title | Requirement | Frame | Mode / notes |
|---|---|---|---|---|
| naked_and_afraid | Naked and Afraid | Kill the Wither wearing no armor | C | Plugin: disqualify a player for a given Wither if they ever damaged it while wearing armor |
| whittled_down | Whittled Down | Final hit on the Ender Dragon with a wooden sword | C | Plugin (dragon is respawnable → no soft lock) |
| untouchable | Untouchable | Take no damage for 20 minutes (one in-game day) | C | Plugin: TimerService; anti-cheese: not AFK, not in vehicle |
| pacifist | Pacifist | Play through 3 consecutive nights without killing any mob | C | Plugin: StreakService. A night counts if the player was online and not AFK for the whole night. Any kill or a skipped or partial night resets |
| food_critic | Food Critic | Eat pufferfish, poisonous potato, rotten flesh and spider eye within 60s, then survive 30s | G | Plugin: WindowService + survival timer |
| totem_chain | Totem Chain | Pop 3 Totems of Undying within 60s | C | Plugin: `EntityResurrectEvent`, WindowService |
| actual_sniper | Actual Sniper | Kill a mob with an arrow from 100+ blocks | C | Plugin: store shooter location on the projectile (PDC) at launch, measure to victim |
| one_man_army | One-Man Army | Win a raid at the highest omen level with no other player within 64 blocks | C | Plugin: track the raid from start; any other player within 64 blocks of raid center at any time disqualifies. Verify the raid omen API for the server version |
| warden_bully | Warden Bully | Kill a Warden with melee only, wearing no armor | C | Plugin: per-Warden disqualification (projectile damage or armor) |
| fresh_start | Fresh Start% | Craft a diamond pickaxe within 10 minutes of respawning | G | Plugin: respawn timestamp + `CraftItemEvent` (crafting required, so handing one over doesn't count) |
| wither_storm | Wither Storm | Kill 3 Withers within 10 minutes | C | Plugin: WindowService |
| iron_will | Iron Will | 5 hours of playtime without dying | C | Plugin: TimerService, death resets, AFK pauses |
| nice_to_mace_you | Nice to Mace You | Craft a mace and hit a mob with it while falling | G | Vanilla (two criteria, both required: `recipe_crafted` mace + `player_hurt_entity` with mace and fall distance > 0). Idea from BlazeandCave's pack |
| frozen_human | Frozen Human | Stay fully frozen in powder snow for 30s and survive | G | Plugin: freeze ticks ≥ max, TimerService |
| free_diver | Free Diver | Stay underwater for 2 minutes without Water Breathing, Conduit Power or Respiration | C | Plugin: eyes in water continuously, TimerService |
| jockey_jamboree | Jockey Jamboree | Kill the rider of a chicken jockey, a spider jockey and a skeleton horseman | C | Plugin, **hidden**. Three sub-criteria via separate criteria names instead of `done` |

### Exploration
| id | Title | Requirement | Frame | Mode / notes |
|---|---|---|---|---|
| commuter | Commuter | Travel 10,000 Overworld-equivalent blocks in one Nether trip | G | Vanilla: `nether_travel` with distance |
| off_the_map | Off the Map | Reach 20,000 blocks from 0,0 on either axis | G | Vanilla: four `location` criteria in OR requirements |
| roads_were_a_mistake | Roads Were a Mistake | Boat 1,000 blocks on ice in one ride | T | Plugin: distance accumulates while in boat on ice, resets on dismount |
| long_way_home | Long Way Home | Die 10,000+ blocks from your respawn point | T | Plugin: Overworld only, respawn point or world spawn |
| top_to_bottom | Top to Bottom | Touch build height and the bedrock floor within 5 minutes | G | Plugin: Overworld, Y thresholds from world min/max height, WindowService |
| dimension_hopper | Dimension Hopper | Visit all three dimensions within 10 minutes | T | Plugin: `PlayerChangedWorldEvent`, WindowService |
| far_end | Far End | Reach 10,000 blocks from 0,0 in the End | G | Vanilla or plugin, whichever is cleaner |
| sky_camper | Sky Camper | Spend a full night above Y 200 | T | Plugin: TimerService over the night window; logout or dropping below Y 200 resets |
| caveman | Caveman | Spend a full in-game day without seeing the sky | G | Plugin: Overworld only (the Nether has no sky = cheese), sky light at head = 0, not AFK |

---

## 8. Adding a new advancement later (keep this section up to date)

**Every advancement also needs** a `difficulty: 1-10` (scale and reasoning in `docs/difficulty.md`) and, except for
smoke tests, a `reward:` (design in `docs/superpowers/specs/2026-10-28-rewards-design.md`):
`reward: [{item: "minecraft:emerald", count: 1, name: "Tax Refund", lore: [...], enchantments: {"minecraft:sharpness": 7}, potion: "minecraft:healing", dye: "#B02E26"}]`.
The generator turns it into a loot table `smp:rewards/<id>` (one pool per item) and adds `rewards.loot` to the advancement.
Two traps found while building it: loot entries use the key `modifier` (an unknown key like `functions` is **silently
ignored**), and `set_count` must come last (components on an oversized stack of unstackable items fail). After changing
any reward, build, deploy, restart the test server and run `scripts/verify-rewards.py` (must say `failures: 0`).

**Vanilla-trigger advancement:**
1. Add an entry to `advancements.yml` with `mode: vanilla` and criteria.
2. Build. The generator validates and emits JSON.

**Plugin-driven advancement:**
1. Add an entry to `advancements.yml` with `mode: plugin` (+ `anti_cheese` if timed or streak).
2. Add thresholds to `config.yml`.
3. Create one handler class in `handlers/<tab>/`, reusing services from §4.
4. Add one line to `Handlers.ALL`.
5. If timed or streak: make sure `/adv progress` shows it.
6. Add a manual test case to `docs/testing.md`.

Before adding anything: check the no-soft-lock rule (§1).

---

## 9. Display (later, not now)

- Frames are assigned deliberately so a future weighted score (e.g. task 1 / goal 2 / challenge 5) is trivial.
- Milestones are out of scope for now.
- Possible later: tab-list score.

---

## Project decisions

- Paper version: 26.3 (test server: `26.3-223-main@e415f5e`, API `26.3.build.223-stable`, Java 25 Temurin, datapack format `data_major` 121 / `data_minor` 0 from the jar's `version.json`)
- Language (Java/Kotlin): Java
- Advancement text language: English
- Namespace: `smp`
- Typical player count: 5 (Group Project default of 5 others is fine)
- Build: `export PATH=~/mc-tools/jdk-25.0.5+8/bin:~/mc-tools/gradle-9.8.2/bin:$PATH`, then `gradle build` (generator + unit tests for the plugin: `gradle :buildSrc:test test`). `gradle deploy` copies the jar into `~/mc-server/plugins`; `scripts/dev-server.sh restart|stop|start|log` controls the test server and refuses to stop it while players are online. Gradle wrapper not generated yet; `run-paper` not used because `~/mc-server` already exists.
- Test server: `~/mc-server` (Claude may use it freely while developing). Start with `start.sh` (console via `console.in` fifo, log in `server.log`). JDK lives in `~/mc-tools/jdk-25*`; `java` and `gradle` are not on the default PATH. Existing plugins there: SBackups, SebStats (the stats/website plugin for Phase 0 check 3 still needs to be identified).
