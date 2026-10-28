# Difficulty grades

Every advancement has a `difficulty: 1-10` in `src/main/catalog/advancements.yml`. That field is the source of
truth: the build validates it (whole number, 1 to 10) and generates `AdvancementIds.DIFFICULTY` (id to grade). This
page explains the scale and the reasoning, so a grade can be argued with. If the two ever disagree, the catalog wins.

These are my estimates for a group of about 5 on a private survival SMP, **before anyone has played them**. Retune
them after a few weeks of real data (for example with the rarity numbers SebStats already publishes).

## Scale

| Grade | Meaning |
|---|---|
| 1 | Happens by itself or in a couple of minutes, no risk |
| 2 | A short, easy task; needs a little setup or a bit of grinding |
| 3 | One clear action that takes some thought or a short trip |
| 4 | Needs preparation or another player, or a small amount of risk |
| 5 | A real project: an hour or so, or a genuine risk of dying |
| 6 | Needs rare materials or good coordination; a few hours across a session |
| 7 | Long grind or a hard, skill-heavy feat |
| 8 | Hard fight or run that most players will fail on a first attempt |
| 9 | Needs a specific setup plus skill, or depends on rare random events |
| 10 | Effectively impossible as written (see the flags below) |

## Silly

| Advancement | Grade | Why |
|---|---|---|
| Tax Evasion | 2 | A Wandering Trader has to show up, then it is one kill |
| Hoarder | 2 | 3,456 dirt is tedious, not hard |
| Fashion Disaster | 2 | Four armor materials is a crafting exercise |
| Self-Own | 3 | Needs a trick (shoot straight up, stand still) and a death |
| Gravity Check | 3 | A deliberate fall death with the right item in the hotbar |
| Staring Contest | 3 | Carved pumpkin, an Enderman, ten steady seconds |
| Creeper Whisperer | 3 | Easy with a shield; fiddly without one |
| It's Fine | 5 | Holding half a heart for a minute takes careful control |
| Professional Napper | 7 | 100 skipped nights is a very long grind |
| One Man's Trash | 10 | Flagged: ten junk catches in a row is about a one in ten billion chance |

## Social

| Advancement | Grade | Why |
|---|---|---|
| Neighborly | 1 | Throw something to a friend |
| Witness | 1 | Stand next to someone who earns something |
| Roommates | 2 | Two beds close together |
| Twinning | 2 | Matching armor with a friend |
| Dinner Party | 2 | Three people at one cake |
| Betrayal | 3 | Needs PvP, which is rare here, plus a prior trade |
| Group Project | 3 | Getting the whole group in one place |
| Bodyguard | 4 | Timing: kill the mob within 5 seconds of its hit |
| Medic | 4 | A friend at under 4 hearts and a splash potion ready |
| Revenge | 5 | Needs PvP twice, in the right order |

## Exploration

| Advancement | Grade | Why |
|---|---|---|
| Caveman | 2 | Twenty minutes of strip mining |
| Commuter | 3 | About 1,250 Nether blocks in one trip |
| Dimension Hopper | 3 | Portals already built; just be quick |
| Long Way Home | 4 | Travel far, then die on purpose |
| Top to Bottom | 4 | Build limit to bedrock in five minutes needs a plan |
| Sky Camper | 4 | A safe platform above Y 200 for a whole night |
| Off the Map | 5 | 20,000 blocks takes a long trip or an elytra |
| Roads Were a Mistake | 5 | A 1,000-block ice road or a very long frozen ocean |
| Far End | 6 | 10,000 blocks in the End realistically needs an elytra |

## Challenges

| Advancement | Grade | Why |
|---|---|---|
| Food Critic | 4 | Four foods and a poison to outlast |
| Fresh Start% | 4 | Easy with diamonds stashed near your bed |
| Frozen Human | 4 | Freezing damage over 30 seconds is survivable at full health |
| Untouchable | 5 | Twenty minutes with no damage needs patience |
| Pacifist | 5 | Three full nights awake with no kills, no AFK, no sleeping |
| Free Diver | 5 | Many dives, drowning damage each time (cumulative version) |
| Nice to Mace You | 6 | The heavy core needs an ominous trial vault |
| Totem Chain | 6 | Three totems and three lethal hits inside a minute |
| Iron Will | 6 | Five hours alive, a time commitment more than a skill test |
| Actual Sniper | 7 | A kill from 100+ blocks takes a good bow and aim |
| Whittled Down | 7 | Only the final blow needs the wooden sword, but it is the dragon |
| Naked and Afraid | 8 | The Wither with zero armor; a bow from a safe spot makes it possible |
| Wither Storm | 8 | Nine skulls and three fights in ten minutes |
| One-Man Army | 9 | Bad Omen V raid, solo, nobody within 64 blocks |
| Warden Bully | 9 | One melee hit kills an unarmored player; needs a trap or a trick |
| Jockey Jamboree | 9 | Three rare mob combinations, one needs a thunderstorm trap |

## Flags (read these)

- **One Man's Trash is graded 10 because it is effectively a soft lock as specified.** The catch table is roughly
  85% fish, 10% junk, 5% treasure, and a fish or treasure resets the streak. Ten junk in a row is about 0.1^10.
  CLAUDE.md rule 1 says every advancement must be achievable. Options: lower the streak (3 junk is about one in a
  thousand catches), count 10 junk total, or let fish not reset it. The count is `advancements.one_mans_trash.count`
  in config.yml; the description in the catalog must change with it.
- **Free Diver was changed from the spec** to a cumulative version; the strict one would be a 10 as well.
- **Warden Bully and Naked and Afraid** are achievable only with tactics (a trap, distance, a bow). Fine, but they
  will be the rarest challenges on the website.

## Reading the grades together

Average difficulty per tab: Silly 4.0, Social 2.7, Exploration 4.0, Challenges 6.4.
That shape is intended: Social is easy because it mostly needs friends, and Challenges is where the effort goes.
The reward tiers (task, goal, challenge) track frame, not these grades, so a grade-9 goal such as Jockey Jamboree
and a grade-2 challenge-frame entry, if one ever exists, would be a mismatch worth revisiting.
