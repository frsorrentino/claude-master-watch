#!/usr/bin/env python3
"""Mockup del contratto 1.38 (Prossimi che sbloccano): le tre schermate dove l'app mostra i Prossimi, 412 px come il
telefono, rese in PNG a 2x con chromium headless. Stessi colori dell'app (web/src/app.css), dati demo delle fixture."""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

CSS = """
:root{--bg:#000;--low:#1B1F26;--surf:#23272E;--high:#292F3A;--line:#2A2E35;--text:#F2F4F7;--text2:#B0B8C4;--icon:#A8C7FA;
--primary:#D3E3FD;--onp:#0A2050;--wait:#FFB020;--good:#65C581;--busy:#7FA1FF;--ring:#8BB4F7;--track:#455165;--label:#BCE4C7;--new:#C58AF9}
*{box-sizing:border-box;margin:0}
body{background:#120D1C;color:var(--text);font:15px/1.4 Roboto,system-ui,sans-serif;width:412px}
.screen{position:relative;width:412px;height:915px;overflow:hidden;background:var(--bg)}
.tag{position:absolute;top:0;left:0;right:0;z-index:50;background:#2B1F45;color:#E9DDFF;font:600 11px/1.3 "Roboto Mono",monospace;letter-spacing:.06em;padding:6px 12px}
.legend{background:#120D1C;color:#E9DDFF;font-size:12.5px;line-height:1.4;padding:10px 14px 14px;border-top:1px solid #3A2C5C}
.mono{font-family:"Roboto Mono",monospace;letter-spacing:.08em;color:var(--text2);font-size:12px}
svg.i{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;flex:none}
.top{padding:34px 20px 6px;display:flex;align-items:baseline;gap:10px}.top h1{font-size:26px;font-weight:500}.top span{color:var(--text2)}
.grp{display:flex;align-items:center;gap:10px;margin:18px 22px 8px;font:500 12px/1 "Roboto Mono",monospace;letter-spacing:.14em}
.grp::after{content:"";flex:1;height:1px;background:currentColor;opacity:.3}
.card{background:var(--low);border-radius:24px;padding:14px 16px;margin:0 14px 10px}
.ch{display:flex;align-items:center;gap:12px}.ch b{font-size:17px;font-weight:600;flex:1}
.badge{width:26px;height:26px;border-radius:50%;display:grid;place-items:center;flex:none}
.line{color:var(--text2);margin-top:8px}
.goal{color:var(--label);font-size:13px;margin-top:8px;white-space:nowrap}
.chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}
.chip{display:inline-flex;align-items:center;gap:6px;border-radius:999px;padding:8px 14px;font-size:14px;background:rgba(211,227,253,.12)}
.chip.unblock{background:rgba(255,176,32,.16);color:var(--wait);font-weight:500}
.prog{height:3px;border-radius:2px;background:var(--track);margin-top:12px;overflow:hidden}.prog i{display:block;height:100%;width:18%;background:var(--ring)}
.box{position:absolute;left:12px;right:12px;bottom:84px;background:var(--low);border:1px solid var(--line);border-radius:22px;overflow:hidden}
.bh{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;border-bottom:1px solid var(--line)}
.row{display:flex;align-items:center;gap:10px;padding:13px 16px;border-top:1px solid var(--line);font-size:16px}
.row:first-of-type{border-top:0}
.row span{flex:1}
.row.unblock{background:rgba(255,176,32,.10);color:var(--wait);font-weight:500;box-shadow:inset 3px 0 0 var(--wait)}
.comp{position:absolute;left:12px;right:12px;bottom:14px;border:1px solid var(--line);border-radius:28px;height:56px;display:flex;align-items:center;gap:14px;padding:0 8px 0 18px;color:var(--text2)}
.send{margin-left:auto;width:42px;height:42px;border-radius:50%;background:var(--high);display:grid;place-items:center}
.chat{padding:40px 16px 0}.cl{font-size:15.5px;line-height:1.5;margin:6px 0}
.hero{background:var(--surf);border-radius:24px;padding:16px 16px 10px;margin:0 14px}
.hero h3{font-size:18px;font-weight:600;margin:8px 0 6px}.hero p{color:var(--text2)}
.step{display:flex;align-items:center;gap:8px;color:var(--icon);font-size:16px;padding:8px 0}
.step.unblock{color:var(--wait);font-weight:500}
.new{outline:2px dashed var(--new);outline-offset:3px;border-radius:14px}
"""

I = {
    'lock': '<rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/>',
    'go': '<path d="M7 17 17 7M8 7h9v9"/>',
    'chev': '<path d="M6 9l6 6 6-6"/>',
    'bolt': '<path d="M13 2 4 14h6l-1 8 9-12h-6z" fill="#0B2A14" stroke="none"/>',
    'pause': '<path d="M9 6v12M15 6v12"/>',
    'plus': '<path d="M12 5v14M5 12h14"/>',
    'send': '<path d="m22 2-7 20-4-9-9-4z"/>',
    'arrow': '<path d="M5 4v7a3 3 0 0 0 3 3h11M15 10l4 4-4 4"/>',
}


def ic(name, color='currentColor', size=20):
    return f'<svg class="i" viewBox="0 0 24 24" style="color:{color};width:{size}px;height:{size}px">{I[name]}</svg>'


def page(tag, body, legend):
    return (f'<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=412">'
            f'<style>{CSS}</style></head><body><div class="screen"><div class="tag">{tag}</div>{body}</div>'
            f'<div class="legend">{legend}</div></body></html>')


screens = {
    '1-home': page(
        'MOCKUP 1.38 · PROSSIMI CHE SBLOCCANO · HOME',
        f'''<div class="top"><h1>Master</h1><span>3 aperte</span></div>
<div class="grp" style="color:var(--busy)">AL LAVORO · 1</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:var(--good)">{ic('bolt', size=16)}</span><b>atlas-shop</b><span class="mono">ctx 18%  5h 11%</span>{ic('chev', '#B0B8C4', 22)}</div>
 <div class="goal">Obiettivo: All checkout tests green and the release tagged</div>
 <div class="line">migrations 008-011 applied, tests green.</div>
 <div class="chips"><span class="chip unblock new">{ic('lock', 'var(--wait)', 16)}ok to deploy on staging</span><span class="chip">review the test seeds</span></div>
 <div class="prog"><i></i></div>
</div>
<div class="grp" style="color:var(--good)">HA FINITO · 1</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:#F4D03F">{ic('pause', '#3A2E00', 14)}</span><b>field-notes</b><span class="mono">ctx 4%  5h 11%</span>{ic('chev', '#B0B8C4', 22)}</div>
 <div class="line">README rewritten with the three sections asked for.</div>
 <div class="chips"><span class="chip">aggiorna il changelog</span><span class="chip">tagga la v1.2</span></div>
 <div class="prog"><i style="width:4%"></i></div>
</div>''',
        'Nella scheda della sessione il tasto che sblocca ha il fondo ambra tenue, il testo ambra e un lucchetto aperto: lo stesso '
        'ambra di «Ti aspetta». Viene sempre per primo; gli altri tasti restano come oggi. Il tocco manda il testo senza «!».'),
    '2-chat': page(
        'MOCKUP 1.38 · PROSSIMI CHE SBLOCCANO · CHAT',
        f'''<div class="chat">
 <div class="cl">README riscritto con le tre sezioni. Per il rilascio serve il tuo ok al push su main.</div>
 <div class="cl" style="color:var(--text2);font-size:13px;margin-top:10px">55 s · 288 token scritti</div>
</div>
<div class="box">
 <div class="bh"><span class="mono">PROSSIMI · 3 · 1 SBLOCCA</span>{ic('chev', '#B0B8C4', 22)}</div>
 <div class="row unblock new">{ic('lock', 'var(--wait)', 18)}<span>ok al push su main</span>{ic('go', 'var(--wait)')}</div>
 <div class="row"><span>aggiorna il changelog</span>{ic('go', '#A8C7FA')}</div>
 <div class="row"><span>tagga la v1.2</span>{ic('go', '#A8C7FA')}</div>
</div>
<div class="comp">{ic('plus', '#A8C7FA', 22)}<span style="font-style:italic">apri la PR</span><span class="send">{ic('send', '#A8C7FA', 20)}</span></div>''',
        'Nella casella «Prossimi» sopra il campo la voce che sblocca è la prima, con la riga ambra a sinistra, il lucchetto e la '
        'freccia ambra; il titolo dice quante sbloccano. Tocco e invio diretto come oggi.'),
    '3-master': page(
        'MOCKUP 1.38 · PROSSIMI CHE SBLOCCANO · ULTIMO ESITO',
        f'''<div style="height:60px"></div>
<div class="hero">
 <div class="mono" style="color:var(--good);font-weight:600;letter-spacing:.14em">ULTIMO ESITO · 21:12</div>
 <h3>Piano Firebase pronto</h3>
 <p>Il piano della strada remota è scritto; per partire serve il tuo ok.</p>
 <div style="height:1px;background:var(--line);margin:14px 0 8px"></div>
 <div class="mono" style="margin-bottom:4px">Prossimi</div>
 <div class="step unblock new">{ic('lock', 'var(--wait)', 18)}approva il piano Firebase</div>
 <div class="step">{ic('arrow', '#A8C7FA', 18)}mockup dei Prossimi che sbloccano</div>
 <div class="step">{ic('arrow', '#A8C7FA', 18)}codice del contratto 1.37</div>
</div>''',
        'Nell\'ultimo esito della master la riga che sblocca prende il lucchetto ambra al posto di «↳». Orologio e widget della master non mostrano i '
        'Prossimi: lì non cambia niente.'),
}

if __name__ == "__main__":
  for name, html in screens.items():
      (HERE / f'{name}.html').write_text(html, encoding='utf-8')
      subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', '--force-device-scale-factor=2',
                      '--window-size=412,1000', '--virtual-time-budget=3000', f'--screenshot={HERE / (name + ".png")}',
                      (HERE / f'{name}.html').as_uri()], check=True, capture_output=True, timeout=60)
      print(name)
