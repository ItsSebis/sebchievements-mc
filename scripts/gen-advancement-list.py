#!/usr/bin/env python3
"""Regenerates docs/advancements.md from src/main/catalog/advancements.yml. Run after changing the catalog."""
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
catalog = yaml.safe_load((ROOT / "src/main/catalog/advancements.yml").read_text())
FRAME = {"task": "Task", "goal": "Goal", "challenge": "Challenge"}


def pretty(item_id):
    return item_id.split(":", 1)[1].replace("_", " ")


def reward_text(entry):
    parts = []
    for item in entry.get("reward") or []:
        label = item.get("name") or pretty(item["item"])
        count = f"{item['count']}x " if item.get("count", 1) > 1 else ""
        extra = " (enchanted)" if item.get("enchantments") else ""
        parts.append(f"{count}{label}{extra}")
    return ", ".join(parts) or "-"


lines = [
    "# Advancement list",
    "",
    "Generated from `src/main/catalog/advancements.yml` by `scripts/gen-advancement-list.py`; do not edit by hand.",
    "Difficulty is 1-10 (see [difficulty.md](difficulty.md)). Hidden advancements are marked and show only a title.",
    "",
]
for key, tab in catalog["tabs"].items():
    entries = [e for e in catalog["advancements"] if e["tab"] == key and e["id"] != "test"]
    lines += [f"## {tab['title']} ({len(entries)})", "", "| Advancement | What | Type | Difficulty | Reward |", "|---|---|---|---|---|"]
    for e in sorted(entries, key=lambda e: (e.get("difficulty", 0), e["title"])):
        hidden = e.get("hidden")
        what = "(hidden)" if hidden else e["description"]
        lines.append(f"| {e['title']} | {what} | {FRAME[e.get('frame', 'task')]} | {e.get('difficulty', '-')} | {reward_text(e)} |")
    lines.append("")
(ROOT / "docs/advancements.md").write_text("\n".join(lines))
print("wrote docs/advancements.md")
