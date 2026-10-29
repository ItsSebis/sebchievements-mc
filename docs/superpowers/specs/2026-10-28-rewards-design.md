# Advancement rewards: design

## Intent

Earning one of our custom advancements should feel good beyond the toast. Each one hands the player a small,
themed item that connects to what they just did. Serious advancements give something useful and fun; silly
ones give something deliberately useless but clearly related (Tax Evasion gives one emerald named
"Tax Refund", a trophy, not a stack). The hardest challenges give something big, including things that are
impossible in vanilla Minecraft.

Why: the SMP is a private group (about 5 players). The advancements are meant to be fun to chase and fun to talk
about, and a thematic reward is a good punchline.

## Scope

- **In scope:** the custom `smp` advancements only (everything in `src/main/catalog/advancements.yml`).
- **Out of scope:** vanilla advancements (no rewards for them for now), a weighted score or milestones
  (CLAUDE.md section 9), per-player reward history UI.
- **Added 2026-10-29: the reward menu** (`/adv rewards`, see the section at the end): reclaiming a lost reward and an
  admin "take any reward" view. This supersedes the "repeatable rewards" non-goal for reclaims only; the vanilla
  grant on completion is still once.

## Goals, non-goals, anti-goals

- Goals: every custom advancement has a connected reward; adding a new advancement with a reward stays one YAML
  entry (CLAUDE.md design goal: cheap to add); rewards are cheap to retune without touching Java.
- Non-goals: an economy, a shop, random loot, repeatable rewards.
- Anti-goals (a failure even if it works): a reward that makes a player invulnerable; a reward that trivialises
  survival (stacks of totems, unbreakable armor, infinite healing); a reward given twice by accident.

## Tiers (the rule of thumb)

| Frame | Reward |
|---|---|
| task | one named trophy item, often a joke |
| goal | a small themed bundle, mildly useful |
| challenge | something nicer, usually enchanted gear |
| very hard challenge | over-vanilla offense/utility (levels above vanilla maximum, enchantment combinations vanilla forbids) |

**Guardrail (decided):** offense and utility may exceed vanilla limits. Defense stays at the vanilla maximum:
no Protection above IV, no unbreakable armor, no stacks of totems, nothing granting invulnerability. PvP is rare
on this server, so the over-vanilla offense (Sharpness VII sword, Power VII bow) stays as written.

## The rewards

Names in quotes are custom item names. "Lore" lines are optional and left to the builder.

### Silly
| Advancement | Reward |
|---|---|
| Tax Evasion | 1 emerald named "Tax Refund" |
| Hoarder | 1 dirt named "Mint Condition Dirt" |
| Fashion Disaster | 1 armor stand named "Fashion Victim" |
| Self-Own | 1 arrow named "Returned to Sender" |
| Gravity Check | 1 feather named "Should Have Packed This" |
| Staring Contest | 1 eye of ender named "Eye Contact" |
| Creeper Whisperer | 1 creeper banner pattern |
| It's Fine | 1 fire resistance potion named "This Is Fine" |
| One Man's Trash | 1 cauldron named "Trash Can" |
| Professional Napper | 1 clock named "Alarm Clock" |

### Social
| Advancement | Reward |
|---|---|
| Neighborly | 1 cookie named "Much Obliged" |
| Roommates | 1 bed named "Spare Bed" |
| Betrayal | 30 iron nuggets named "Thirty Pieces of Silver" |
| Group Project | 1 bell named "Meeting Bell" |
| Bodyguard | 1 shield |
| Medic | 1 splash potion of Healing named "House Call" |
| Revenge | 1 wither rose named "Served Cold" |
| Witness | 1 spyglass named "Eyewitness" |
| Twinning | 2 identically dyed leather caps named "Matching Hats" |
| Dinner Party | 1 cake named "Leftovers" |

### Challenges
| Advancement | Reward |
|---|---|
| Naked and Afraid | leather tunic "Birthday Suit": Protection IV, Thorns III |
| Whittled Down | wooden sword: Sharpness VII, Unbreaking V |
| Untouchable | shield: Unbreaking V, Mending |
| Pacifist | 8 golden carrots and a flowering azalea named "Olive Branch" |
| Food Critic | 2 golden apples named "Michelin Star" |
| Totem Chain | 2 totems of undying |
| Actual Sniper | bow "Long Shot": Power VII, Infinity and Mending together |
| One-Man Army | crossbow: Multishot and Piercing IV together, Quick Charge IV |
| Warden Bully | netherite sword "Warden's Bane": Sharpness VII, Unbreaking V, Mending; plus 4 echo shards |
| Fresh Start% | 1 compass named "Speedrunner's Compass" |
| Wither Storm | netherite axe: Sharpness VII, Efficiency VII, Unbreaking V |
| Iron Will | iron chestplate: Protection IV, Unbreaking V, Mending |
| Nice to Mace You | 4 wind charges |
| Frozen Human | leather boots named "Cold Feet" |
| Free Diver | turtle helmet: Respiration V, Aqua Affinity |
| Jockey Jamboree | 1 saddle and 1 carrot on a stick |

### Exploration
| Advancement | Reward |
|---|---|
| Commuter | 1 warped fungus on a stick named "Commuter Pass" |
| Off the Map | 1 recovery compass |
| Far End | 8 ender pearls |
| Roads Were a Mistake | 1 blue ice |
| Long Way Home | 1 ender pearl named "Return Ticket" |
| Top to Bottom | Feather Falling IV book |
| Dimension Hopper | 1 flint and steel named "Frequent Flyer" |
| Sky Camper | 1 white wool named "Cloud Nine" |
| Caveman | 2 night vision potions |

## How it works (decided and builder's call)

- **Decided (by Sebi):** items only; given when the advancement is earned; the full-inventory case drops the item
  at the player's feet.
- **My call (proposal, not yet agreed): where rewards live.** A `reward:` field on each catalog entry, turned by
  the existing generator into vanilla advancement `rewards` (a generated loot table per advancement). Reasons:
  works identically for `mode: vanilla` and `mode: plugin`, needs no Java, and the vanilla reward system already
  handles "drop at feet if full". The catalog validator should check reward item ids and enchantment names.
- **My call:** over-vanilla enchantment levels and forbidden combinations are expressed in the loot table
  (`set_enchantments` allows both), so no plugin code is needed for them.
- **Constraint (CLAUDE.md section 1):** thresholds and numbers belong in config, not code. Reward contents are
  catalog data, not thresholds, so they live in the catalog. In-game descriptions of rewards, if shown, must come
  from the same catalog entry.
- **Constraint:** one malformed datapack entry stops the whole server from loading (seen in Phase 1). Reward
  generation must be validated at build time and checked on the test server before any real deploy.

## Left to the builder

- Exact YAML shape of `reward:` and how enchantments are written.
- Whether item lore is added, and its wording.
- Whether `/adv` shows a reward preview (not requested).
- How to verify an over-vanilla loot table loads on 26.3 (verify against vanilla loot table JSON in the server
  jar before relying on it, as with advancement criteria).

## Calls made after the builder check (mine, marked for review)

- **Rollout order:** the tables above cover the full planned catalog (CLAUDE.md section 7). Most of those
  advancements do not exist yet; they arrive in Phases 3+. Build the reward mechanism first and give the five
  existing entries (Tax Evasion, Nice to Mace You, Commuter, Off the Map, Far End) their rewards; every later
  entry gets its `reward:` when it is added.
- **Frame vs tier:** the tier table is guidance for choosing rewards, not enforced by the generator. The reward
  table wins for any specific advancement (Nice to Mace You is a `goal` frame in the Challenges tab).
- **Mechanism risk:** spike the awkward items on the test server before committing to the loot-table route:
  creeper banner pattern, named potions, an enchanted book with Feather Falling IV, levels above vanilla maximum,
  forbidden enchantment combinations, two identically dyed caps. Design `reward:` as catalog data with a backend
  that can be a loot table or a small plugin `RewardGiver`, chosen per entry if needed.
- **Matching Hats:** both caps are red.
- **Totems:** Totem Chain's 2 totems are two separate items (totems do not stack); that is fine under the guardrail.

## Decided by Sebi after the builder check

1. An admin revoke and re-grant **may** give the reward again. No per-player claimed-state is needed; vanilla
   advancement rewards (generated loot table) are enough. This also settles the anti-goal "a reward given twice by
   accident": it means accidental double-grants from the game itself, not deliberate admin re-grants.
2. **No retroactive rewards.** The SMP has not been live, so nobody has progress to backfill.

## Open: nothing blocking

All design questions from the conversation are settled. If the loot-table route turns out not to support
something above (for example a named potion or a banner pattern), the fallback is a small `RewardGiver` in the
plugin driven by the same catalog field.

## Status: built (2026-10-28)

Implemented as proposed: a `reward:` list per catalog entry (item, count, name, lore, enchantments, potion, dye),
validated at build time and generated into one loot table per advancement. Every item also gets a grey lore line
"Sebchievement: <title>" so a stray reward can be traced. All 49 reward items (45 advancements) verified on the 26.3
test server with `scripts/verify-rewards.py`; the loot-table route handled every awkward item (over-vanilla levels,
Infinity with Mending, Multishot with Piercing, named potions, banner pattern, dyed caps, enchanted book), so no plugin
`RewardGiver` was needed. Red for Matching Hats is `#B02E26`. Not yet verified: the actual grant from an advancement
in-game (needs a player; see "Rewards" in `docs/testing.md`).

## Reward menu (added 2026-10-29)

`/adv rewards` opens a 54-slot inventory screen.

- **Player view:** the rewards of advancements the player has completed; clicking one gives it back.
  Admins (`smpadv.admin`) get a lever in the bottom row that toggles to **All rewards (admin)**, where a click gives any
  reward with no checks and no cooldown. The menu always opens in the player view.
- **Tagging:** every reward item carries the PDC tag `smp:reward_id` (the generated loot table writes it into
  `custom_data.PublicBukkitValues`, the menu writes it through the Bukkit API; `scripts/verify-rewards.py` checks it).
  Item specs are generated into `RewardSpecs` from the same catalog field, so there is still one source of truth.
- **Copy cap:** a player without `smpadv.rewards.multiple` may hold, per reward item, at most the catalog count, and only
  for advancements they completed. Held = cursor + inventory + ender chest + contents of shulker boxes and bundles they
  carry. It is enforced when picking items up, when taking them out of a container, when reclaiming, and by a sweep every
  second (`TickScheduler`) that **deletes** excess copies. Taking items out of a container into the inventory is blocked;
  putting them into a chest is allowed.
- **Cooldown:** after a reclaim, the same reward can't be reclaimed again for `rewards.reclaim_cooldown_minutes` (default
  60), stored per player in `reward_reclaim/<id>`. Bypass: `smpadv.rewards.nocooldown`. The first reclaim after earning
  an advancement has no wait; the vanilla grant does not start the cooldown.
- **Known limits:** consumable rewards (golden apples, pearls, arrows) can be reclaimed again once used and the cooldown
  has passed; players who haven't earned a reward can't hold or be gifted a copy; an admin `revoke` of an advancement
  makes the sweep remove that player's copies; a bypass-permission holder is never checked.

## Effect perks (added 2026-10-29)

A reward can be a permanent potion effect instead of an item: `perk: {effect, amplifier, name, icon}` on a plugin-mode
catalog entry (generated into `PerkSpecs`; `amplifier` 0 is level I). It is shown in `/adv rewards` as a toggle (on by
default, state in the player's PDC key `perk_off/<id>`).

- **How it works:** `PerkService` re-applies the effect every second for 2 seconds (ambient, no particles) to every
  online player who has the advancement and has not switched the perk off. Milk, death, relog and a plain potion that
  overwrites it are fixed by the next sweep. Revoking the advancement or switching the perk off just stops the
  reapplication; the effect it leaves behind (ambient, no particles, 2 seconds or less, same level) is also removed at
  once. A real potion of the same level is never removed, and a stronger or longer effect is not replaced.
- **Guardrail:** perks follow the same rule as items. Offence and utility (Speed, Haste) are fine; nothing that adds
  defence beyond the vanilla maximum or invulnerability (no Resistance V, no permanent Regeneration or Absorption).
- **Current perks:** Marathon -> Speed II, Moving Mountains -> Haste II.

