#!/usr/bin/env python3
"""Misure del relay dal suo log (piano prestazioni del 07/10/2026, Task 0): cadenza delle push e comandi all'ora per
dispositivo. Legge le righe di oggi:
  2026-10-07T16:19:27 push: 7 sessioni, 0 eventi
  2026-10-07T16:20:28 cmd <id>: transcript <sessione> da <dispositivo> → ok …
Uso: relay-measures.py LOG --since HH:MM --until HH:MM [--day AAAA-MM-GG]"""
import json
import re
import statistics
import sys
from collections import Counter
from datetime import datetime

PUSH = re.compile(r"^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d) push: ")
CMD = re.compile(r"^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d) cmd \S+: (\w+) (?:(\S+) )?da (.+?) → ")


def minutes(hhmm):
    h, m = hhmm.split(":")
    return int(h) * 60 + int(m)


def measure(lines, since, until, day=None):
    pushes, ops = [], Counter()
    for line in lines:
        if day and not line.startswith(day):
            continue
        t = line[11:16]
        if not (since <= t < until):
            continue
        if m := PUSH.match(line):
            pushes.append(datetime.fromisoformat(m.group(1)))
        elif m := CMD.match(line):
            ops[(m.group(2), m.group(4))] += 1
    hours = max(minutes(until) - minutes(since), 1) / 60
    gaps = [(b - a).total_seconds() for a, b in zip(pushes, pushes[1:])]
    return {
        "window": f"{day + ' ' if day else ''}{since}-{until}",
        "push_per_hour": round(len(pushes) / hours, 1),
        "push_interval_s": {
            "median": statistics.median(gaps) if gaps else None,
            "p90": sorted(gaps)[min(len(gaps) - 1, int(len(gaps) * 0.9))] if gaps else None,
            "max": max(gaps) if gaps else None,
        },
        "per_hour": {f"{op} da {dev}": round(n / hours, 1) for (op, dev), n in sorted(ops.items(), key=lambda x: -x[1])},
    }


def main(argv):
    path = argv[1]
    since, until = argv[argv.index("--since") + 1], argv[argv.index("--until") + 1]
    day = argv[argv.index("--day") + 1] if "--day" in argv else None
    with open(path, encoding="utf-8", errors="replace") as f:
        print(json.dumps(measure(f, since, until, day), ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main(sys.argv)
