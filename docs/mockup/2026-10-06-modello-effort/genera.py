#!/usr/bin/env python3
"""Bozza del foglio Modello ed effort nello stile del menu principale (Franz, 06/10 22:39): pannello che scende dal
tasto come «Vai a» e il menu ≡, voci con l'icona nel cerchio, titolo e riga che spiega, effort come gruppo di tasti,
e in testa il consiglio sul compito della sessione (contratto 1.37 A, deciso il 05/10 alle 19:51). 412 px come il telefono,
PNG a 2x con chromium headless; colori di web/src/app.css."""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

CSS = """
:root{--bg:#000;--low:#1B1F26;--surf:#23272E;--high:#292F3A;--line:#2A2E35;--text:#F2F4F7;--text2:#B0B8C4;--icon:#A8C7FA;
--primary:#D3E3FD;--onp:#0A2050;--good:#65C581;--warn:#FFC46B;--ring:#8BB4F7;--track:#455165;--advice:#C58AF9}
*{box-sizing:border-box;margin:0}
body{background:#120D1C;color:var(--text);font:15px/1.4 Roboto,system-ui,sans-serif;width:412px}
.screen{position:relative;width:412px;height:915px;overflow:hidden;background:var(--bg)}
.tag{position:absolute;top:0;left:0;right:0;z-index:50;background:#2B1F45;color:#E9DDFF;font:600 11px/1.3 "Roboto Mono",monospace;letter-spacing:.06em;padding:6px 12px}
svg.i{width:24px;height:24px;flex:none}
/* testata della sessione, come oggi */
.top{padding-top:30px;border-bottom:1px solid var(--line)}
.r1{display:flex;align-items:center;gap:10px;height:56px;padding:0 12px 0 8px}
.back{width:40px;display:grid;place-items:center}
.badge{width:26px;height:26px;border-radius:50%;display:grid;place-items:center;background:var(--good);flex:none}
.name{font-size:20px;font-weight:500}
.sp{flex:1}
.r2{display:flex;align-items:center;gap:14px;padding:2px 12px 12px}
.pill{position:relative;display:flex;align-items:center;gap:4px;background:var(--surf);border-radius:999px;padding:8px 8px 8px 16px;font-size:15.5px;font-weight:500;white-space:nowrap}
.dot{position:absolute;top:1px;right:3px;width:10px;height:10px;border-radius:50%;background:var(--advice);border:2px solid var(--bg)}
.meter{display:flex;align-items:center;gap:6px;font-size:15px;font-weight:500;color:var(--text2);white-space:nowrap}
.meter small{display:block;font-size:11.5px}
.chat{padding:14px 16px}
.me{display:flex;justify-content:flex-end;margin:6px 0 16px 40px}
.bub{background:var(--surf);border-radius:22px 22px 6px 22px;padding:10px 16px;font-size:15.5px}
.cl{font-size:15.5px;line-height:1.5}
/* il pannello, come MenuPanel/PanelShell */
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.55);z-index:20}
.panel{position:absolute;left:12px;right:12px;top:86px;z-index:30;background:var(--surf);border-radius:28px;padding:8px 0 14px;box-shadow:0 12px 40px rgba(0,0,0,.5)}
.ph{display:flex;align-items:center;min-height:48px;padding:0 8px 0 20px;color:var(--text2);font-size:14px}
.ph .x{width:48px;height:48px;display:grid;place-items:center}
.grp{font:500 12px/1 "Roboto Mono",monospace;letter-spacing:.14em;color:var(--text2);margin:16px 20px 6px}
.circ{width:40px;height:40px;border-radius:50%;display:grid;place-items:center;background:var(--high);color:var(--icon);font:600 17px/1 Roboto,sans-serif;flex:none}
.mi{display:flex;align-items:center;gap:16px;min-height:64px;padding:10px 20px}
.mi.sel{background:var(--high)}
.mi.sel .circ{background:rgba(168,199,250,.25)}
.tx{flex:1;min-width:0}
.mt{display:flex;align-items:center;gap:10px;font-size:16px;font-weight:500}
.ms{font-size:12.5px;color:var(--text2);margin-top:2px}
.rec{font:600 11px/1.4 "Roboto Mono",monospace;letter-spacing:.06em;color:var(--advice);background:rgba(197,138,249,.14);border-radius:999px;padding:3px 9px}
/* il consiglio */
.adv{margin:2px 12px 4px;background:rgba(197,138,249,.10);border-radius:20px;padding:12px 14px 14px}
.adv .row{display:flex;gap:14px;align-items:flex-start}
.adv .circ{background:rgba(197,138,249,.22);color:var(--advice)}
.ov{font:600 11px/1.4 "Roboto Mono",monospace;letter-spacing:.08em;color:var(--advice)}
.at{font-size:16.5px;font-weight:500;margin-top:2px}
.aw{font-size:13.5px;color:var(--text2);margin-top:2px;line-height:1.35}
.cost{display:flex;gap:8px;align-items:flex-start;color:var(--warn);font-size:13px;line-height:1.35;margin:10px 0 0 54px}
.use{display:inline-flex;align-items:center;gap:8px;margin:12px 0 0 54px;background:rgba(197,138,249,.24);color:#EBD9FF;border-radius:999px;padding:9px 18px;font-weight:500;font-size:14.5px}
/* effort: gruppo di tasti connessi (ButtonGroup di Material 3 Expressive) */
.bgp{display:flex;gap:2px;margin:4px 20px 0}
.bgp span{position:relative;flex:1;height:44px;display:grid;place-items:center;background:var(--high);font-size:14px;font-weight:500;border-radius:8px}
.bgp span:first-child{border-radius:22px 8px 8px 22px}
.bgp span:last-child{border-radius:8px 22px 22px 8px}
.bgp span.on{background:var(--primary);color:var(--onp);border-radius:22px}
.bgp i{position:absolute;top:6px;right:7px;width:7px;height:7px;border-radius:50%;background:var(--advice)}
.ed{font-size:13px;color:var(--text2);margin:10px 20px 0}
.legend{background:#120D1C;color:#E9DDFF;font-size:12.5px;line-height:1.4;padding:10px 14px 14px;border-top:1px solid #3A2C5C}
"""

I = {
    'back': '<path d="M15 6l-6 6 6 6" fill="none" stroke="#F2F4F7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
    'caret': '<path d="M7 10l5 5 5-5z" fill="#B0B8C4"/>',
    'menu': '<path d="M4 7h16M4 12h16M4 17h16" stroke="#A8C7FA" stroke-width="2" stroke-linecap="round"/>',
    'bolt': '<path d="M13 2 4 14h6l-1 8 9-12h-6z" fill="#0B2A14"/>',
    'close': '<path d="M6 6l12 12M18 6 6 18" stroke="#B0B8C4" stroke-width="2" stroke-linecap="round"/>',
    'check': '<path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#A8C7FA" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>',
    'spark': '<path d="M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z" fill="#C58AF9"/>',
    'warn': '<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01" fill="none" stroke="#FFC46B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
    'reset': '<path d="M3 12a9 9 0 1 0 3-6.7M3 4v5h5" fill="none" stroke="#B0B8C4" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>',
}


def ic(name, size=24):
    return f'<svg class="i" viewBox="0 0 24 24" style="width:{size}px;height:{size}px">{I[name]}</svg>'


def ring(pct, label, sub=''):
    arc = f'{pct * 0.4712:.1f} 100'
    s = f'<small>{ic("reset", 11)} {sub}</small>' if sub else ''
    return (f'<span class="meter"><svg viewBox="0 0 20 20" width="22" height="22"><circle cx="10" cy="10" r="7.5" fill="none" '
            f'stroke="#455165" stroke-width="3"/><circle cx="10" cy="10" r="7.5" fill="none" stroke="#8BB4F7" stroke-width="3" '
            f'pathLength="47.12" stroke-dasharray="{arc}" transform="rotate(-90 10 10)" stroke-linecap="round"/></svg>'
            f'<span>{label}{s}</span></span>')


def screen(tag, pill, dot, chat, panel, legend):
    d = '<span class="dot"></span>' if dot else ''
    return f'''<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=412">
<style>{CSS}</style></head><body><div class="screen"><div class="tag">{tag}</div>
<div class="top"><div class="r1"><span class="back">{ic('back')}</span><span class="badge">{ic('bolt', 16)}</span>
 <span class="name">atlas-shop</span>{ic('caret', 22)}<span class="sp"></span>{ic('menu')}</div>
 <div class="r2"><span class="pill">{pill}{ic('caret', 20)}{d}</span>{ring(11, '5h 11%', 'mer 03:20')}{ring(18, 'ctx 18%')}</div></div>
<div class="chat">{chat}</div>
<div class="scrim"></div><div class="panel">
 <div class="ph"><span class="sp">Modello ed effort · vale per questa sessione</span><span class="x">{ic('close')}</span></div>
{panel}
</div></div><div class="legend">{legend}</div></body></html>'''


MODELS = [('O', 'Opus 5.5', 'Di punta: pianifica, scrive e verifica'),
          ('F', 'Fable 5.1', 'Il più forte nei giudizi, costa di più'),
          ('S', 'Sonnet 5', 'Esegue bene compiti chiari, costa meno'),
          ('H', 'Haiku 4.5', 'Compiti semplici e veloci')]
EFFORTS = ['low', 'medium', 'high', 'xhigh', 'max']
EFFORT_SUB = {'low': 'Risposte rapide, ragiona poco', 'medium': 'Equilibrio fra velocità e ragionamento',
              'high': 'Ragiona di più: debug e piani', 'xhigh': 'Lavori lunghi e difficili',
              'max': 'Nessun limite al ragionamento: consuma più quota'}


def lists(model, effort, rec_model, rec_effort):
    rows = ''.join(
        f'''<div class="mi{' sel' if label == model else ''}"><span class="circ">{mono}</span>
 <span class="tx"><div class="mt">{label}{'<span class="rec">CONSIGLIATO</span>' if label == rec_model and label != model else ''}</div><div class="ms">{sub}</div></span>
 {ic('check') if label == model else ''}</div>'''
        for mono, label, sub in MODELS)
    keys = ''.join(f'<span class="{"on" if e == effort else ""}">{e}{"<i></i>" if e == rec_effort and e != effort else ""}</span>' for e in EFFORTS)
    return (f'<div class="grp">MODELLO</div>{rows}<div class="grp">EFFORT</div><div class="bgp">{keys}</div>'
            f'<div class="ed">{effort} · {EFFORT_SUB[effort]}</div>')


CHAT1 = '''<div class="me"><div class="bub">Rifai il checkout con il riepilogo in alto</div></div>
<div class="cl">Ho diviso il checkout in tre passaggi. Restano i test delle spedizioni.</div>'''

screens = {
    '1-consiglio-diverso': screen(
        'BOZZA · CONSIGLIO DIVERSO, A METÀ LAVORO', 'Sonnet 5 · medium', True, CHAT1,
        f'''<div class="adv"><div class="row"><span class="circ">{ic('spark', 22)}</span><span class="tx">
 <div class="ov">CONSIGLIO PER QUESTO COMPITO</div><div class="at">Fable 5.1 · high</div>
 <div class="aw">Refactor del checkout su molti file: servono giudizi su tanto codice</div></span></div>
 <div class="cost">{ic('warn', 16)}<span>Sta lavorando: cambiare adesso ricarica circa 36 000 token di cache. Conviene al prossimo compito.</span></div>
 <span class="use">Usa il consiglio</span></div>
''' + lists('Sonnet 5', 'medium', 'Fable 5.1', 'high'),
        'Pannello come «Vai a» e il menu ≡: scende dal tasto, voci con l\'icona nel cerchio e la riga che spiega. In testa il '
        'consiglio di fable-director sul compito della sessione, col motivo; «Usa il consiglio» cambia modello ed effort '
        'insieme (nuovo, da approvare). Riquadro ambra solo se il consiglio è diverso e la sessione è a metà lavoro. '
        'Effort come gruppo di tasti; il puntino lilla segna quello consigliato.'),
    '2-consiglio-uguale': screen(
        'BOZZA · SCELTA GIÀ CONSIGLIATA', 'Opus 5.5 · medium', False,
        '''<div class="me"><div class="bub">Rivediamo insieme i colori della home</div></div>
<div class="cl">Ecco tre varianti della card, la seconda è più vicina al resto dell'app.</div>''',
        f'''<div class="adv"><div class="row"><span class="circ">{ic('spark', 22)}</span><span class="tx">
 <div class="ov">CONSIGLIO PER QUESTO COMPITO</div><div class="at">Va bene così: Opus 5.5 · medium</div>
 <div class="aw">Revisione grafica con te: serve il modello di punta, medium basta</div></span></div></div>
''' + lists('Opus 5.5', 'medium', 'Opus 5.5', 'medium'),
        'Consiglio uguale alla scelta: una conferma col motivo, senza tasto, senza costo e senza puntino sul tasto. '
        'Senza consiglio (assente o più vecchio di 6 ore) il riquadro non c\'è. I motivi sono esempi: oggi fable-director '
        'scrive solo la quota («quota ok: Opus 5.5 medium va bene»), il compito non lo guarda.'),
}

for name, html in screens.items():
    (HERE / f'{name}.html').write_text(html, encoding='utf-8')
    subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', '--force-device-scale-factor=2',
                    '--window-size=412,1100', '--virtual-time-budget=3000', f'--screenshot={HERE / (name + ".png")}',
                    (HERE / f'{name}.html').as_uri()], check=True, capture_output=True, timeout=90)
    print(name)
