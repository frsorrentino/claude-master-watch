#!/usr/bin/env python3
"""Dal dump di `uiautomator` (stdin) il centro del primo elemento il cui testo o descrizione contiene l'argomento.
Stampa «X Y» per `adb shell input tap`; esce con 1 se non lo trova. Uso: find.py "Overview" < ui.xml"""
import re
import sys
import xml.etree.ElementTree as ET

needle = sys.argv[1].lower()
root = ET.fromstring(sys.stdin.read())
for node in root.iter("node"):
    label = (node.get("text") or "") + " " + (node.get("content-desc") or "")
    if needle in label.lower():
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
