# Sebchievements

A Paper plugin for a private Minecraft SMP that adds 45 custom advancements across four new tabs, each with a themed
reward. One jar contains its own datapack plus the Java logic for the advancements vanilla triggers can't detect.

> **Status:** built, unit-tested and given a broad first playtest on a real server. Fixes from that session are in
> v0.2.0; not every advancement has been triggered in-game yet. See [docs/playtest.md](docs/playtest.md).

| Tab | Advancements | Flavour |
|---|---|---|
| Social | 10 | Needs other players: trading, sleeping nearby, sharing a cake |
| Silly | 10 | Small absurd feats, and rewards that are deliberately useless but on theme |
| Exploration | 9 | Long trips, dimensions, heights |
| Challenges | 16 | The hard ones, with bigger rewards (some beyond what vanilla allows) |

Full list with descriptions, difficulty grades and rewards: [docs/advancements.md](docs/advancements.md).

## Requirements

- Paper **26.3** (built against `26.3.build.223-stable`)
- Java **25**

## Install

1. Download `sebchievements-<version>.jar` from the [releases](../../releases) and put it in `plugins/`.
2. Start the server. The plugin registers its datapack itself and gives every player the four tab roots on join;
   there is nothing to copy into the world folder.
3. Optional: edit `plugins/Sebchievements/config.yml` (thresholds, AFK time) and restart.

The datapack zip on the releases page is the same pack on its own, for inspection. The plugin jar is all a server needs.

## Commands

| Command | Who | What |
|---|---|---|
| `/adv progress` | everyone | Lists timed, streak and counter advancements with your current progress, e.g. `Untouchable: 12m 40s / 20m` |
| `/adv rewards` | everyone | Menu to get back rewards you earned and lost (gear costs an ordinary item of the same kind, consumables can't be reclaimed; one set at a time, with a cooldown) and to switch effect perks on and off; admins can toggle to every reward |
| `/adv perks` | everyone | Menu to switch your effect perks on and off (also reachable from the rewards menu) |
| `/adv grant <player> <id>` | `smpadv.admin` | Awards every remaining criterion of an advancement |
| `/adv revoke <player> <id>` | `smpadv.admin` | Takes an advancement back |
| `/adv reset <player> <id>` | `smpadv.admin` | Clears the player's tracked state (timers, streaks, counters) for that id |
| `/adv debug <id> [player]` | `smpadv.admin` | Dumps stored state for an advancement |

`smpadv.admin` defaults to operators.

## How it plays

- **Rewards** are items handed over when the advancement is earned (dropped at your feet if your inventory is full).
  Each carries a grey "Sebchievement: <title>" lore line. Task rewards are trophies, goals give a small themed bundle,
  and the hardest challenges give over-vanilla gear. Defensive gear stays at the vanilla maximum.
- **No soft locks.** Every advancement can be earned by any player at any time. Timed and streak ones state what does
  *not* count (AFK, creative/spectator, riding, exploits) and report progress in `/adv progress`.
- **Difficulty** is graded 1-10 per advancement; see [docs/difficulty.md](docs/difficulty.md).

Where the spec was changed to stay achievable: Group Project needs 4 other players (a group of 5), Free Diver counts
cumulative time underwater, and One Man's Trash is a streak of 3. All of these are in `config.yml`.

## Configuration

`config.yml` holds every threshold, keyed by advancement id (distances, durations, counts, junk list, ...) plus
`afk_seconds`. The in-game descriptions come from the catalog and must match the defaults, so if you change a number
that appears in a description, change the catalog entry too.

## Building

You need JDK 25 on your `PATH`; the Gradle wrapper downloads Gradle itself.

```sh
./gradlew build        # generates the datapack, compiles, runs all unit tests -> build/libs/sebchievements.jar
./gradlew :buildSrc:test # catalog validator and loot-table generator tests
```

The generator validates the catalog and **fails the build** on duplicate ids, unknown parents or tabs, bad frames,
vanilla entries without criteria, timed/streak entries without an `anti_cheese` note, and malformed rewards.

## Adding an advancement

Everything an advancement is lives in one place: [`src/main/catalog/advancements.yml`](src/main/catalog/advancements.yml).

- **Vanilla trigger:** add a catalog entry (`mode: vanilla` plus criteria), a `difficulty`, and a `reward`. Done.
- **Plugin-driven:** the same entry with `mode: plugin`, thresholds in `config.yml`, one handler class under
  `handlers/<tab>/`, and one line in `Handlers.ALL`. Timed handlers extend `TimedHandler`, which applies the shared
  anti-cheese policy. Add a test case to `docs/testing.md`.

The full checklist and the traps found so far (the loot table key is `modifier`, unknown keys fail silently, one bad
datapack entry stops the whole server from starting) are in section 8 of [CLAUDE.md](CLAUDE.md).

## Testing

- Unit tests (no server): `./gradlew :buildSrc:test test`
- Rewards on a running test server: `scripts/verify-rewards.py` spawns every reward table and checks the real item
  data against the catalog (it must report `failures: 0`)
- By hand: [docs/playtest.md](docs/playtest.md) (one ordered session) and [docs/testing.md](docs/testing.md)
  (per-advancement cases)
- `scripts/dev-server.sh restart|stop|start|log` manages the local test server and refuses to stop it while players are online

## Project layout

```
src/main/catalog/advancements.yml   single source of truth for every advancement
buildSrc/                           catalog validator + datapack / loot table / AdvancementIds generator
src/main/java/.../core/             Granter, PlayerDataStore, handler plumbing, startup validation
src/main/java/.../services/         shared services: timers, streaks, windows, damage log, proximity, AFK, game clock
src/main/java/.../handlers/         one class per plugin-driven advancement, grouped by tab
src/main/java/.../command/          /adv
docs/                               difficulty, playtest, testing, advancement list, rewards design
scripts/                            dev server control, reward verifier, advancement list generator
```

## Docs

- [docs/advancements.md](docs/advancements.md): the full list (generated; rerun `scripts/gen-advancement-list.py` after catalog changes)
- [docs/difficulty.md](docs/difficulty.md): grading scale and reasoning
- [docs/playtest.md](docs/playtest.md) / [docs/testing.md](docs/testing.md): how to test it
- [docs/superpowers/specs/2026-10-28-rewards-design.md](docs/superpowers/specs/2026-10-28-rewards-design.md): rewards design
- [CLAUDE.md](CLAUDE.md): the original spec, architecture and project decisions

Advancements show up on the SebStats statistics website (a separate plugin by the same author) under the "SMP" category.
