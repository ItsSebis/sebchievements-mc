#!/usr/bin/env python3
"""Spawns every reward loot table in the running test server and checks the real item data against the catalog.

The server ignores unknown loot-table keys silently, so "it loaded" proves nothing: this reads back the items.
Needs the server started by scripts/dev-server.sh with the current jar deployed and nobody online
(it kills dropped items in the world). Usage: scripts/verify-rewards.py
"""
import re
import sys
import time
from pathlib import Path

import yaml

SERVER = Path.home() / "mc-server"
CATALOG = Path(__file__).resolve().parent.parent / "src/main/catalog/advancements.yml"
LOG, CONSOLE = SERVER / "server.log", SERVER / "console.in"


def send(cmd, wait=0.5):
    with open(CONSOLE, "w") as f:
        f.write(cmd + "\n")
    time.sleep(wait)


def new_log(since):
    lines = LOG.read_text(errors="replace").split("\n")[since:]
    text = "\n".join(lines)
    return re.sub(r"\x1b?\[[0-9;]*m", "", text).replace("\n", "")


def line_count():
    return len(LOG.read_text(errors="replace").split("\n")) - 1


def main():
    mark = line_count()
    send("list", 1)
    if not re.search(r"There are 0 of", new_log(mark)):
        sys.exit("Server not answering, or players are online; not touching the world.")
    entries = {e["id"]: e for e in yaml.safe_load(CATALOG.read_text())["advancements"] if e.get("reward")}
    send("forceload add 0 0", 2)
    send("kill @e[type=item]")
    failures = 0
    for id_, entry in entries.items():
        for n, item in enumerate(entry["reward"], 1):
            mark = line_count()
            send(f"loot spawn 0 100 0 loot smp:rewards/{id_}", 0.5)
            send(f'data get entity @e[type=item,nbt={{Item:{{id:"{item["item"]}"}}}},limit=1] Item', 0.7)
            text = new_log(mark)
            send("kill @e[type=item]", 0.3)
            problems = check(item, text, entry["title"])
            if problems:
                failures += 1
                print(f"FAIL {id_} item #{n} ({item['item']}): {problems}")
    print(f"checked {sum(len(e['reward']) for e in entries.values())} reward items for {len(entries)} advancements; failures: {failures}")
    sys.exit(1 if failures else 0)


def check(item, text, title):
    if "ERROR" in text:
        return ["server logged an error: " + text[text.index("ERROR"):][:200]]
    if "has the following entity data:" not in text:
        return ["item did not drop"]
    data = text.split("has the following entity data:", 1)[1]
    problems = []
    got = re.search(r"count: (\d+)", data)
    # a count above the item's stack size is split into several entities, so only require "not larger than asked"
    want = item.get("count", 1)
    got_n = int(got.group(1)) if got else 1
    if got_n > want:
        problems.append(f"count {got_n} > {want}")
    if "name" in item and item["name"] not in data:
        problems.append("custom name missing")
    if f"Sebchievement: {title}" not in data:
        problems.append("lore missing")
    for ench, level in item.get("enchantments", {}).items():
        if not re.search(rf'"{re.escape(ench)}": {level}\b', data):
            problems.append(f"{ench} {level} missing")
    if "potion" in item and item["potion"] not in data:
        problems.append("potion missing")
    if "dye" in item and "dyed_color" not in data:
        problems.append("dyed_color missing")
    return problems


if __name__ == "__main__":
    main()
