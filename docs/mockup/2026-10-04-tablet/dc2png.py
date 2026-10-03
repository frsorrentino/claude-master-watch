#!/usr/bin/env python3
"""Rende una tavola .dc.html del canvas come PNG alla sua misura vera, senza il motore del canvas.

Espande {{buchi}}, <sc-for> e <sc-if> con i valori di renderVals() (vals.js), poi fotografa con Chromium headless.
Uso: dc2png.py tavola.dc.html uscita.png LARGHEZZA ALTEZZA [stato-json] [scala]
"""
import json, re, subprocess, sys, html, os, tempfile

here = os.path.dirname(os.path.abspath(__file__))
src_path, out_png, w, h = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
state = sys.argv[5] if len(sys.argv) > 5 else ''
scale = sys.argv[6] if len(sys.argv) > 6 else '2'
src = open(src_path, encoding='utf-8').read()
vals = json.loads(subprocess.run(['node', os.path.join(here, 'vals.js'), src_path] + ([state] if state else []),
                                 capture_output=True, text=True, check=True).stdout)

body = re.search(r'<x-dc>([\s\S]*)</x-dc>', src).group(1)
helmet = re.search(r'<helmet>([\s\S]*?)</helmet>', body)
head_extra = helmet.group(1) if helmet else ''
body = re.sub(r'<helmet>[\s\S]*?</helmet>', '', body)

def lookup(scope, path):
    path = path.strip()
    if path in ('true', 'false'):
        return path == 'true'
    cur = None
    for i, part in enumerate(path.split('.')):
        if i == 0:
            for s in reversed(scope):
                if isinstance(s, dict) and part in s:
                    cur = s[part]; break
            else:
                return None
        else:
            cur = cur.get(part) if isinstance(cur, dict) else None
    return cur

def holes(text, scope):
    def rep(m):
        v = lookup(scope, m.group(1))
        if v is None: return ''
        if isinstance(v, bool): return 'true' if v else 'false'
        return html.escape(str(v), quote=True)
    return re.sub(r'\{\{\s*([\w.$]+)\s*\}\}', rep, text)

TAG = re.compile(r'<(/?)(sc-for|sc-if)\b([^>]*)>')

def render(text, scope):
    out, pos = [], 0
    while True:
        m = TAG.search(text, pos)
        if not m:
            out.append(holes(text[pos:], scope)); break
        out.append(holes(text[pos:m.start()], scope))
        name, attrs = m.group(2), m.group(3)
        depth, j = 1, m.end()
        while depth:
            n = TAG.search(text, j)
            if n.group(2) == name: depth += -1 if n.group(1) else 1
            j = n.end()
        inner = text[m.end():n.start()]
        if name == 'sc-for':
            lst = lookup(scope, re.search(r'list="\{\{\s*([\w.$]+)\s*\}\}"', attrs).group(1)) or []
            var = re.search(r'as="(\w+)"', attrs).group(1)
            for i, item in enumerate(lst):
                out.append(render(inner, scope + [{var: item, '$index': i}]))
        else:
            v = lookup(scope, re.search(r'value="\{\{\s*([\w.$]+)\s*\}\}"', attrs).group(1))
            if v: out.append(render(inner, scope))
        pos = j
    return ''.join(out)

page = f'''<!doctype html><html lang="it"><head><meta charset="utf-8">{head_extra}
<style>html,body{{margin:0;background:#000}}</style></head><body>{render(body, [vals])}</body></html>'''
with tempfile.NamedTemporaryFile('w', suffix='.html', delete=False, encoding='utf-8') as f:
    f.write(page); tmp = f.name
subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', f'--force-device-scale-factor={scale}',
                f'--window-size={w},{h}', '--virtual-time-budget=4000', f'--screenshot={os.path.abspath(out_png)}', 'file://' + tmp],
               check=True, capture_output=True)
os.unlink(tmp)
print(out_png)
