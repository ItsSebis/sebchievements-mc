#!/usr/bin/env python3
"""Regenerates docs/advancements.md from src/main/catalog/advancements.yml. Run after changing the catalog."""
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
catalog = yaml.safe_load((ROOT / "src/main/catalog/advancements.yml").read_text())
FRAME = {"task": "Task", "goal": "Goal", "challenge": "Challenge"}


def pretty(item_id):
    return item_id.split(":", 1)[1].replace("_", " ")


ROMAN = ["", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"]


def roman(n):
    return ROMAN[n] if 0 < n < len(ROMAN) else str(n)


def title_case(item_id):
    return pretty(item_id).title()


def enchant_text(enchantments):
    """Sharpness VII, Mending (a level of 1 is left out for single-level enchantments)."""
    out = []
    for name, level in enchantments.items():
        single = name.split(":", 1)[1] in ("mending", "infinity", "aqua_affinity", "multishot", "silk_touch")
        out.append(title_case(name) if single and level == 1 else f"{title_case(name)} {roman(level)}")
    return ", ".join(out)


RECLAIM = {
    "free": "Free",
    "cost": "Costs {count}ordinary {base} (used up)",
    "never": "Not reclaimable",
}


def item_details(item):
    """Everything about one reward item except its name and count."""
    bits = []
    if item.get("enchantments"):
        bits.append(enchant_text(item["enchantments"]))
    if item.get("potion"):
        bits.append(f"{title_case(item['potion'])} potion effect")
    if item.get("dye"):
        bits.append(f"dyed {item['dye']}")
    for line in item.get("lore") or []:
        bits.append(f'lore "{line}"')
    return "; ".join(bits) or "-"


def perk_details(perk):
    if "looting_bonus" in perk:
        return (f"Looting +{perk['looting_bonus']} on top of the Looting on your weapon, for mobs you kill "
                f"(config: `perks.<id>.looting_bonus`)")
    level = roman(perk.get("amplifier", 0) + 1)
    return f"{title_case(perk['effect'])} {level}, kept up while earned and switched on (config: `perks.<id>.amplifier`)"


def reward_rows(entry):
    rows = []
    for item in entry.get("reward") or []:
        count = f"{item['count']}x " if item.get("count", 1) > 1 else ""
        base = title_case(item["item"])
        name = f"{count}{item['name']}" if item.get("name") else f"{count}{base}"
        shown = f"{name} ({base})" if item.get("name") else name
        reclaim = RECLAIM[item.get("reclaim", "free")].format(base=base.lower(), count=f"{item['count']}x " if item.get("count", 1) > 1 else "an ")
        rows.append((shown, item_details(item), reclaim))
    perk = entry.get("perk")
    if perk:
        rows.append((f"Perk: {perk['name']}", perk_details(perk).replace("<id>", entry["id"]), "Switch on or off in `/adv perks`"))
    return rows


def reward_text(entry):
    """The Reward column of the overview: every reward with what it is and what it does."""
    parts = []
    for item in entry.get("reward") or []:
        count = f"{item['count']}x " if item.get("count", 1) > 1 else ""
        base = title_case(item["item"])
        details = item_details(item)
        inner = base
        if details != "-":
            inner = f"{inner}; {details}" if item.get("name") else f"{inner}: {details}"
        parts.append(f"{count}{item['name']} ({inner})" if item.get("name") else f"{count}{inner}")
    perk = entry.get("perk")
    if perk:
        parts.append(f"Perk {perk['name']}: {perk_details(perk).replace('<id>', entry['id'])}")
    return "<br>".join(parts) or "-"


lines = [
    "# Advancement list",
    "",
    "Generated from `src/main/catalog/advancements.yml` by `scripts/gen-advancement-list.py`; do not edit by hand.",
    "Difficulty is 1-10 (see [difficulty.md](difficulty.md)). Hidden advancements are marked and show only a title.",
    "The rewards are explained in detail [below](#rewards-in-detail).",
    "",
]
for key, tab in catalog["tabs"].items():
    entries = [e for e in catalog["advancements"] if e["tab"] == key]
    lines += [f"## {tab['title']} ({len(entries)})", "", "| Advancement | What | Type | Difficulty | Reward |", "|---|---|---|---|---|"]
    for e in sorted(entries, key=lambda e: (e.get("difficulty", 0), e["title"])):
        hidden = e.get("hidden")
        what = "(hidden)" if hidden else e["description"]
        lines.append(f"| {e['title']} | {what} | {FRAME[e.get('frame', 'task')]} | {e.get('difficulty', '-')} | {reward_text(e)} |")
    lines.append("")
lines += [
    "# Rewards in detail",
    "",
    "## How rewards work",
    "",
    "- **Earning:** completing an advancement hands over its reward items straight away (dropped at your feet if your",
    "  inventory is full). Every item carries a grey `Sebchievement: <title>` lore line so a stray one can be traced.",
    "  An admin revoke and re-grant gives the reward again.",
    "- **Tiers:** tasks give a trophy (often a joke), goals a small themed bundle, challenges usually enchanted gear, and",
    "  the hardest challenges go beyond vanilla for offence and utility (levels above the vanilla maximum, enchantment",
    "  combinations vanilla forbids). Defence stays at the vanilla maximum: no Protection above IV, no unbreakable armor,",
    "  no stacks of totems, nothing that makes you invulnerable.",
    "- **Getting a lost reward back (`/adv rewards`):** the menu lists the rewards of advancements you completed. How an",
    "  item can be reclaimed is its *Reclaim* column below:",
    "  - *Free*: trophies and placeable items; a click gives the item again.",
    "  - *Costs ... ordinary ...*: enchanted or durable gear; you need an ordinary item of the same kind in your inventory,",
    "    which is used up, and you get the enchanted version. Nothing is taken unless the new item fits.",
    "  - *Not reclaimable*: consumables (totems, pearls, potions, food, arrows, echo shards, ...); they only come from the",
    "    normal grant when you earn the advancement. A reward made only of these is not listed in the menu.",
    "- **Cooldown:** the same reward can't be reclaimed again for 60 minutes (`rewards.reclaim_cooldown_minutes`), counted",
    "  from the later of earning the advancement and your last reclaim.",
    "- **Copy limit:** without the `smpadv.rewards.multiple` permission you can carry at most the listed count of a",
    "  reward item (inventory, ender chest, shulker boxes and bundles included), and only for advancements you completed.",
    "  Extra copies can't be picked up or taken out of a container, and are removed by a periodic sweep. A reward item",
    "  that was changed into another material (for example a diamond pickaxe upgraded to netherite) still counts as the",
    "  copy it was issued as.",
    "- **Perks:** a few advancements unlock a permanent perk instead of (or besides) an item: a potion effect kept up",
    "  while you have the advancement, or extra Looting levels on mob kills. They are switched on and off in",
    "  `/adv perks` (or the rewards menu) and their strength is set under `perks:` in `config.yml`. Defaults are shown",
    "  below.",
    "- **Admins** (`smpadv.admin`) can toggle the rewards menu to every reward, which gives any reward with no checks and",
    "  no cooldown.",
    "",
]
for key, tab in catalog["tabs"].items():
    entries = [e for e in catalog["advancements"] if e["tab"] == key and (e.get("reward") or e.get("perk"))]
    if not entries:
        continue
    lines += [f"## {tab['title']}", "", "| Advancement | Reward | Details | Reclaim |", "|---|---|---|---|"]
    for e in sorted(entries, key=lambda e: (e.get("difficulty", 0), e["title"])):
        for reward, details, reclaim in reward_rows(e):
            lines.append(f"| {e['title']} | {reward} | {details} | {reclaim} |")
    lines.append("")
(ROOT / "docs/advancements.md").write_text("\n".join(lines))
print("wrote docs/advancements.md")
