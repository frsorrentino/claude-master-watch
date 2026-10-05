#!/usr/bin/env python3
"""Tre varianti più sobrie dei Prossimi che sbloccano (Franz, 05/10 21:31: «qualcosa di più professionale ed elegante»):
testo bianco come gli altri tasti, l'ambra solo dove serve. Niente segni da mockup: la differenza deve vedersi da sola."""
import pathlib
import subprocess
from genera import CSS, I, ic

HERE = pathlib.Path(__file__).parent

EXTRA = """
.chip{color:var(--text);font-weight:400}
.row{color:var(--text)}
.chat{padding:0 16px}
.box{position:static;margin:18px 12px 0}
/* V1: solo il lucchetto ambra */
.v1 .chip.u, .v1 .row.u{}
/* V2: un filo ambra */
.v2 .chip.u{box-shadow:inset 0 0 0 1.5px rgba(255,176,32,.65)}
.v2 .row.u{box-shadow:inset 3px 0 0 var(--wait)}
/* V3: fondo ambra tenue */
.v3 .chip.u{background:rgba(255,176,32,.14)}
.v3 .row.u{background:rgba(255,176,32,.09)}
.head{font:500 12px/1.3 "Roboto Mono",monospace;letter-spacing:.12em;color:var(--text2);margin:20px 22px 0}
"""

def screen(v, title, legend):
    lock = ic('lock', 'var(--wait)', 16)
    body = f'''<div style="height:38px"></div>
<div class="head">SCHEDA DELLA SESSIONE, NELLA HOME</div>
<div class="grp" style="color:var(--busy);margin-top:10px">AL LAVORO · 1</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:var(--good)">{ic('bolt', size=16)}</span><b>atlas-shop</b><span class="mono">ctx 18%  5h 11%</span>{ic('chev', '#B0B8C4', 22)}</div>
 <div class="line">migrations 008-011 applied, tests green.</div>
 <div class="chips"><span class="chip u">{lock}ok to deploy on staging</span><span class="chip">review the test seeds</span></div>
 <div class="prog"><i></i></div>
</div>
<div class="head" style="margin-top:26px">CASELLA PROSSIMI NELLA CHAT</div>
<div class="box">
 <div class="bh"><span class="mono">PROSSIMI · 3</span>{ic('chev', '#B0B8C4', 22)}</div>
 <div class="row u">{ic('lock', 'var(--wait)', 18)}<span>ok al push su main</span>{ic('go', '#A8C7FA')}</div>
 <div class="row"><span>aggiorna il changelog</span>{ic('go', '#A8C7FA')}</div>
 <div class="row"><span>tagga la v1.2</span>{ic('go', '#A8C7FA')}</div>
</div>
<div class="head" style="margin-top:26px">ULTIMO ESITO DELLA MASTER</div>
<div class="hero" style="margin-top:10px;padding-top:6px">
 <div class="step u">{ic('lock', 'var(--wait)', 18)}approva il piano Firebase</div>
 <div class="step">{ic('arrow', '#A8C7FA', 18)}codice del contratto 1.37</div>
</div>'''
    return (f'<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=412">'
            f'<style>{CSS}{EXTRA}</style></head><body class="{v}"><div class="screen" style="height:860px"><div class="tag">{title}</div>{body}</div>'
            f'<div class="legend">{legend}</div></body></html>')

variants = {
    'v1-lucchetto': ('VARIANTE 1 · SOLO IL LUCCHETTO', 'v1',
        'Il tasto è uguale agli altri: lo distingue solo il lucchetto ambra davanti, e sta sempre per primo. La più discreta.'),
    'v2-filo': ('VARIANTE 2 · FILO AMBRA', 'v2',
        'Un filo ambra sottile intorno al tasto (nella casella, una riga ambra a sinistra) e il lucchetto. Testo bianco come gli altri. Quella che consiglio: si vede subito senza gridare.'),
    'v3-tonale': ('VARIANTE 3 · FONDO AMBRA TENUE', 'v3',
        'Il fondo del tasto si scalda appena verso l\'ambra, con il lucchetto; testo bianco. Più presente della 2, meno netta come contorno.'),
}
for name, (title, v, legend) in variants.items():
    (HERE / f'{name}.html').write_text(screen(v, title, legend), encoding='utf-8')
    subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', '--force-device-scale-factor=2',
                    '--window-size=412,930', '--virtual-time-budget=3000', f'--screenshot={HERE / (name + ".png")}',
                    (HERE / f'{name}.html').as_uri()], check=True, capture_output=True, timeout=60)
    print(name)
