#!/usr/bin/env python3
"""Mockup del contratto 1.37 (la master al servizio dell'app): una pagina HTML per schermata, 412 px come il telefono,
resa in PNG a 2x con chromium headless. Stessi colori dell'app (web/src/app.css), dati demo delle fixture."""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

CSS = """
:root{--bg:#000;--low:#1B1F26;--surf:#23272E;--high:#292F3A;--line:#2A2E35;--text:#F2F4F7;--text2:#B0B8C4;--icon:#A8C7FA;
--primary:#D3E3FD;--onp:#0A2050;--wait:#FFB020;--good:#65C581;--busy:#7FA1FF;--gone:#FF453A;--warn:#FFC46B;--alert:#E5736B;
--ring:#8BB4F7;--track:#455165;--label:#BCE4C7;--new:#C58AF9}
*{box-sizing:border-box;margin:0}
body{background:#120D1C;color:var(--text);font:15px/1.4 Roboto,system-ui,sans-serif;width:412px}
.screen{position:relative;width:412px;height:915px;overflow:hidden;background:var(--bg)}
.mono{font-family:"Roboto Mono",monospace;letter-spacing:.08em;color:var(--text2);font-size:12px}
.tag{position:absolute;top:0;left:0;right:0;z-index:50;background:#2B1F45;color:#E9DDFF;font:600 11px/1.3 "Roboto Mono",monospace;letter-spacing:.06em;padding:6px 12px}
.new{outline:2px dashed var(--new);outline-offset:3px;border-radius:14px}
svg.i{width:22px;height:22px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;flex:none}
/* testata della sessione */
.hdr{border-bottom:1px solid var(--line);padding:30px 0 8px}
.hrow{display:flex;align-items:center;gap:6px;padding-left:2px;min-height:52px}
.back{width:36px;display:grid;place-items:center}
.pill{position:relative;display:flex;align-items:center;gap:2px;background:var(--surf);border-radius:999px;padding:6px 6px 6px 12px;font-size:14px;font-weight:500;white-space:nowrap}
.dot{position:absolute;top:2px;right:2px;width:9px;height:9px;border-radius:50%;background:var(--new);border:2px solid var(--bg)}
.sp{flex:1}
.meter{display:flex;align-items:center;gap:6px;padding:4px;font-size:14px;font-weight:500;color:var(--text2);white-space:nowrap}
.note{padding:0 16px;font-size:12px;font-weight:500;color:var(--label);letter-spacing:.03em}
/* chat */
.chat{padding:12px 16px}
.me{display:flex;flex-direction:column;align-items:flex-end;margin:8px 0 14px;padding-left:40px}
.bub{background:var(--surf);border-radius:22px 22px 6px 22px;padding:10px 16px;font-size:15.5px}
.meta{display:flex;gap:10px;align-items:center;color:var(--text2);font-size:13px;margin-top:6px}
.cl{font-size:15.5px;line-height:1.5;margin:6px 0}
.acts{display:flex;align-items:center;gap:10px;color:var(--text2);margin:6px 0 14px}
.play{width:36px;height:36px;border-radius:50%;background:var(--high);display:grid;place-items:center}
.comp{position:absolute;left:12px;right:12px;bottom:14px;border:1px solid var(--line);border-radius:28px;height:56px;display:flex;align-items:center;gap:14px;padding:0 8px 0 18px;color:var(--text2)}
.send{margin-left:auto;width:42px;height:42px;border-radius:50%;background:var(--primary);display:grid;place-items:center;color:var(--onp)}
/* home */
.top{padding:34px 20px 6px;display:flex;align-items:baseline;gap:10px}
.top h1{font-size:26px;font-weight:500}.top span{color:var(--text2);font-size:15px}
.ql{display:flex;gap:16px;padding:6px 16px 4px;font-size:12px}
.grp{display:flex;align-items:center;gap:10px;margin:18px 22px 8px;font:500 12px/1 "Roboto Mono",monospace;letter-spacing:.14em}
.grp::after{content:"";flex:1;height:1px;background:currentColor;opacity:.3}
.card{background:var(--low);border-radius:24px;padding:14px 16px;margin:0 14px 10px}
.ch{display:flex;align-items:center;gap:12px}
.ch b{font-size:17px;font-weight:600}
.badge{width:26px;height:26px;border-radius:50%;display:grid;place-items:center;flex:none}
.prog{height:3px;border-radius:2px;background:var(--track);margin-top:10px;overflow:hidden}
.prog i{display:block;height:100%;background:var(--ring)}
.line{color:var(--text2);margin-top:8px;font-size:15px}
.kv{display:grid;grid-template-columns:auto 1fr;gap:4px 10px;margin-top:10px;font-size:14.5px}
.kv span:nth-child(odd){color:var(--text2)}
.chip{display:inline-flex;align-items:center;gap:6px;border-radius:999px;padding:3px 10px;font:600 11px/1.4 "Roboto Mono",monospace;letter-spacing:.08em}
.prod{background:rgba(229,115,107,.16);color:var(--alert)}
.dup{background:rgba(255,196,107,.14);color:var(--warn)}
.btns{display:flex;gap:10px;margin-top:12px;align-items:center}
.filled{background:var(--primary);color:var(--onp);border-radius:999px;padding:11px 20px;font-weight:500;text-align:center}
.tonal{background:var(--high);border-radius:999px;padding:11px 20px;font-weight:500;text-align:center}
.textb{color:var(--icon);padding:11px 8px;font-weight:500}
/* fogli */
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.55);z-index:20}
.sheet{position:absolute;left:0;right:0;bottom:0;z-index:30;background:var(--surf);border-radius:28px 28px 0 0;padding:20px 20px 26px}
.alert{position:absolute;left:22px;right:22px;top:50%;transform:translateY(-50%);z-index:30;background:var(--surf);border-radius:28px;padding:22px}
.sheet h3,.alert h3{font-size:22px;font-weight:600;margin:10px 0 4px}
.sheet h3:first-child,.alert h3:first-child{margin-top:0}
.sub{color:var(--text2);font-size:14px}
.radio{display:flex;align-items:center;gap:14px;padding:7px 4px;font-size:16px}
.rb{width:20px;height:20px;border-radius:50%;border:2px solid var(--text2);flex:none}
.rb.on{border:6px solid var(--primary)}
.rec{margin-left:auto;font:600 11px/1.4 "Roboto Mono",monospace;letter-spacing:.06em;color:var(--new);background:rgba(197,138,249,.14);border-radius:999px;padding:3px 9px}
.why{margin:-4px 0 4px 38px;font-size:13px;color:var(--text2)}
.cost{display:flex;gap:10px;align-items:flex-start;background:rgba(255,196,107,.10);color:var(--warn);border-radius:16px;padding:10px 12px;font-size:13.5px;margin:12px 0 2px}
.field{background:var(--high);border-radius:16px;padding:12px 14px;font-size:15.5px;margin-top:12px;line-height:1.45}
.lab{font-size:12px;color:var(--text2);margin:14px 0 6px}
.col{display:flex;flex-direction:column;gap:10px;margin-top:12px}
.opt{display:flex;flex-direction:column;align-items:flex-start;text-align:left}
.opt small{font-size:12.5px;font-weight:400;opacity:.8;margin-top:2px}
.banner{position:absolute;left:12px;right:12px;bottom:80px;display:flex;align-items:center;gap:12px;background:var(--high);border-radius:20px;padding:10px 10px 10px 14px}
.banner .t{flex:1;font-size:14px;line-height:1.3}
.banner .t b{display:block;font-weight:500;font-size:14.5px}
.banner .tonal{background:#3A4356;color:var(--primary)}
.legend{background:#120D1C;color:#E9DDFF;font-size:12.5px;line-height:1.4;padding:10px 14px 14px;border-top:1px solid #3A2C5C}
"""

I = {
    'back': '<path d="M15 6l-6 6 6 6"/>',
    'caret': '<path d="M7 10l5 5 5-5z" fill="#B0B8C4" stroke="none"/>',
    'more': '<circle cx="12" cy="5" r="1.4" fill="#B0B8C4"/><circle cx="12" cy="12" r="1.4" fill="#B0B8C4"/><circle cx="12" cy="19" r="1.4" fill="#B0B8C4"/>',
    'copy': '<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"/>',
    'play': '<path d="M8 5v14l11-7z" fill="#A8C7FA" stroke="none"/>',
    'plus': '<path d="M12 5v14M5 12h14"/>',
    'seal': '<path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z"/><path d="m9 12 2 2 4-4"/>',
    'bookmark': '<path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/>',
    'bulb': '<path d="M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5M9 18h6M10 22h4"/>',
    'warn': '<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01"/>',
    'power': '<path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0"/>',
    'chev': '<path d="M6 9l6 6 6-6"/>',
    'pause': '<path d="M9 6v12M15 6v12"/>',
    'bolt': '<path d="M13 2 4 14h6l-1 8 9-12h-6z" fill="#0B2A14" stroke="none"/>',
    'hand': '<path d="M18 11V6a2 2 0 0 0-4 0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.9-6-2.4l-3.6-3.6a2 2 0 0 1 2.8-2.8L7 15"/>',
    'layers': '<path d="M12 2 2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>',
}


def ic(name, color='currentColor', size=22):
    return f'<svg class="i" viewBox="0 0 24 24" style="color:{color};width:{size}px;height:{size}px">{I[name]}</svg>'


def page(tag, body, legend):
    return (f'<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=412">'
            f'<style>{CSS}</style></head><body><div class="screen"><div class="tag">{tag}</div>{body}</div><div class="legend">{legend}</div></body></html>')


def header(pill, dot, ctx, ctx_color='var(--ring)', notes=''):
    d = '<span class="dot"></span>' if dot else ''
    arc = f'{ctx * 0.4712:.1f} 100'
    return f'''<div class="hdr"><div class="hrow">
 <span class="back">{ic('back', '#F2F4F7')}</span>
 <span class="pill {'new' if dot else ''}">{pill}<svg viewBox="0 0 24 24" width="18" height="18">{I['caret']}</svg>{d}</span>
 <span class="sp"></span>
 <span class="meter"><svg viewBox="0 0 20 20" width="20" height="20"><circle cx="10" cy="10" r="7.5" fill="none" stroke="#455165" stroke-width="3"/><circle cx="10" cy="10" r="7.5" fill="none" stroke="var(--ring)" stroke-width="3" pathLength="47.12" stroke-dasharray="5.2 100" transform="rotate(-90 10 10)" stroke-linecap="round"/></svg>5h 11%</span>
 <span class="meter"><svg viewBox="0 0 20 20" width="20" height="20"><circle cx="10" cy="10" r="7.5" fill="none" stroke="#455165" stroke-width="3"/><circle cx="10" cy="10" r="7.5" fill="none" stroke="{ctx_color}" stroke-width="3" pathLength="47.12" stroke-dasharray="{arc}" transform="rotate(-90 10 10)" stroke-linecap="round"/></svg>ctx {ctx}%</span>
 <span style="width:34px;display:grid;place-items:center"><svg class="i" viewBox="0 0 24 24">{I['more']}</svg></span>
</div>{notes}</div>'''


ATLAS_CHAT = f'''<div class="chat">
 <div class="me"><div class="bub">Rifai il checkout con il riepilogo in alto</div><div class="meta">dal PC 12:40 {ic('copy', '#B0B8C4', 20)}</div></div>
 <div class="cl">Ho diviso il checkout in tre passaggi. Restano i test delle spedizioni.</div>
 <div class="acts">{ic('copy', '#B0B8C4')}<span class="play">{ic('play', size=18)}</span><span style="font-size:13px">12:46</span></div>
</div>'''

screens = {}

# 1 — A: il puntino sul tasto e il consiglio dentro il foglio Modello ed effort.
screens['1-consiglio-selettore'] = page(
    'MOCKUP 1.37 · A · CONSIGLIO DENTRO IL SELETTORE',
    header('Sonnet 5 · medium', True, 18, notes='<div class="note">Obiettivo: tutti i test del checkout verdi</div>') + ATLAS_CHAT +
    f'''<div class="scrim"></div><div class="sheet">
 <p class="sub">La scelta vale per questa sessione</p>
 <h3>Modello</h3>
 <div class="radio"><span class="rb"></span>Opus 5</div>
 <div class="new" style="padding-bottom:6px"><div class="radio"><span class="rb"></span>Fable 5.1<span class="rec">CONSIGLIATO</span></div>
 <div class="why">Refactor del checkout su molti file</div></div>
 <div class="radio"><span class="rb on"></span>Sonnet 5</div>
 <div class="radio"><span class="rb"></span>Haiku 4.5</div>
 <h3>Effort</h3>
 <div class="radio"><span class="rb"></span>low</div>
 <div class="radio"><span class="rb on"></span>medium</div>
 <div class="radio new"><span class="rb"></span>high<span class="rec">CONSIGLIATO</span></div>
 <div class="radio"><span class="rb"></span>xhigh</div>
 <div class="radio"><span class="rb"></span>max</div>
 <div class="cost new">{ic('warn', 'var(--warn)', 18)}<span>Sta lavorando: cambiare adesso ricarica circa 36 000 token di cache. Conviene al prossimo compito.</span></div>
</div>''',
    'Puntino lilla sul tasto solo se la scelta attuale è diversa dal consiglio. Nel foglio «consigliato» accanto a modello ed effort, '
    'sotto il modello il motivo. Il riquadro ambra c\'è solo a metà lavoro (when = next_task). Il consiglio non si applica da solo: si sceglie come oggi.')

# 2 — B: «Da approvare» in cima alla home.
screens['2-da-approvare'] = page(
    'MOCKUP 1.37 · B · DA APPROVARE NELLA HOME',
    f'''<div class="top"><h1>Master</h1><span>3 aperte</span></div>
<div class="ql mono" style="align-items:center"><span>○ 5h</span><span style="flex:1;height:3px;border-radius:2px;background:var(--track);position:relative;align-self:center"><i style="position:absolute;left:0;top:0;bottom:0;width:11%;background:var(--ring)"></i></span><span>11% · si azzera alle 16:00</span></div>
<div class="grp" style="color:var(--new)">DA APPROVARE · 1</div>
<div class="card new">
 <div class="ch">{ic('seal', 'var(--new)', 24)}<b style="flex:1">Release 2.4 di atlas-shop</b><span class="chip prod">PRODUZIONE</span></div>
 <div class="kv"><span>Esce</span><span>tag v2.4 e push su origin main</span><span>Dove</span><span>produzione (shop.example.com)</span><span>Da</span><span>atlas-shop · chiesto 6 min fa</span></div>
 <div class="btns"><span class="filled" style="flex:1">Approva</span><span class="textb">Apri la sessione</span></div>
</div>
<div class="grp" style="color:var(--wait)">TI ASPETTA · 1</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:#3B82F6;border-radius:8px">{ic('hand', '#fff', 16)}</span><b style="flex:1">ledger-api</b><span class="mono">ctx 62%</span>{ic('chev', '#B0B8C4')}</div>
 <div class="line" style="color:var(--text)">Deploy ready, waiting for the client's ok. Deploy now?</div>
 <div class="btns"><span class="tonal" style="flex:1">1 · yes</span><span class="tonal" style="flex:1">2 · no</span></div>
</div>
<div class="grp" style="color:var(--busy)">AL LAVORO · 1</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:var(--good)">{ic('bolt', size=16)}</span><b style="flex:1">atlas-shop</b><span class="mono">ctx 18%</span>{ic('chev', '#B0B8C4')}</div>
 <div class="line">migrations 008-011 applied, tests green.</div>
</div>''',
    '«Da approvare» viene prima di «Ti aspetta»: cosa esce, dove, da quale sessione. Il bollino PRODUZIONE quando deploy è vero. '
    "Con un'approvazione in lista, le opzioni della domanda sotto restano tonali: un solo bottone pieno. Anche nel menu ≡ e in «Ti aspettano».")

# 3 — B: la conferma, sempre per la produzione.
screens['3-approva-conferma'] = page(
    'MOCKUP 1.37 · B · CONFERMA PRIMA DI APPROVARE',
    f'''<div class="top"><h1>Master</h1><span>3 aperte</span></div>
<div class="grp" style="color:var(--new)">DA APPROVARE · 1</div>
<div class="card"><div class="ch">{ic('seal', 'var(--new)', 24)}<b style="flex:1">Release 2.4 di atlas-shop</b><span class="chip prod">PRODUZIONE</span></div></div>
<div class="scrim"></div><div class="alert new">
 <h3>Approvi l'uscita in produzione?</h3>
 <p class="sub" style="margin-top:6px">Release 2.4 di atlas-shop: tag v2.4 e push su origin main, verso shop.example.com.</p>
 <div class="lab">Nota per la sessione (facoltativa)</div>
 <div class="field" style="color:var(--text2)">ok</div>
 <div class="btns" style="justify-content:flex-end;margin-top:18px"><span class="textb" style="color:var(--text2)">Annulla</span><span class="filled">Approva</span></div>
</div>''',
    'Il tocco su Approva apre questa conferma; la nota va nel registro come «… (dal telefono)» o «(dalla web app)». '
    'Senza nota vale «ok». Dopo l\'ok la card sparisce al prossimo stato; un rifiuto del PC si dice in basso con il suo motivo.')

# 4 — C: la proposta nella chat oltre il 60%.
screens['4-contesto-proposta'] = page(
    'MOCKUP 1.37 · C · CONTESTO OLTRE IL 60%',
    header('Sonnet 5 · medium', False, 64, 'var(--warn)') + ATLAS_CHAT +
    f'''<div class="banner new">{ic('layers', 'var(--warn)')}<span class="t"><b>Contesto al 64%</b>Handoff e poi /clear: riparte leggera</span><span class="tonal" style="padding:9px 16px">Fallo</span></div>
<div class="comp">{ic('plus', '#A8C7FA')}<span>Scrivi ad atlas-shop</span><span class="send">{ic('play', '#0A2050', 18)}</span></div>''',
    'Sopra il campo, solo con la sessione ferma e il contesto oltre il 60%; si chiude con uno scorrimento e torna al 70% e all\'80%. '
    '«Fallo» manda il prompt di handoff (alla master la ricorrente master-handoff) e, a turno finito, /clear.')

# 5 — C: il foglio del contesto, con l'handoff in cima.
screens['5-contesto-foglio'] = page(
    'MOCKUP 1.37 · C · IL FOGLIO DEL CONTESTO',
    header('Sonnet 5 · medium', False, 64, 'var(--warn)') + ATLAS_CHAT +
    f'''<div class="scrim"></div><div class="sheet">
 <h3>Contesto 64%</h3>
 <div class="col">
  <span class="filled opt new">Handoff, poi /clear<small>Scrive il punto in docs, poi svuota il contesto a turno finito</small></span>
  <span class="tonal opt">Compatta la conversazione<small>/compact: riassume e tiene il filo</small></span>
  <span class="tonal opt">Passa alla finestra da 1M<small>Stesso modello, più spazio</small></span>
 </div>
 <p class="sub" style="margin-top:14px">Il riavvio della sessione compare solo quando c'è un plugin o un Claude Code più nuovo.</p>
</div>''',
    'Il tasto ctx apre il foglio di oggi; cambia la prima voce: non più «riparti pulita» con il riavvio, ma handoff e /clear (la slash già permessa). '
    'Il riavvio torna solo con una versione nuova, che il relay riconosce dall\'asterisco.')

# 6 — D: «Salva come decisione» da una risposta di Claude.
screens['6-salva-decisione'] = page(
    'MOCKUP 1.37 · D · SALVA COME DECISIONE',
    header('Sonnet 5 · medium', False, 18) + f'''<div class="chat">
 <div class="cl">Ho deciso di mostrare i prezzi sempre con l'IVA nel checkout, anche per le aziende: la tolgo solo in fattura.</div>
 <div class="acts">{ic('copy', '#B0B8C4')}<span class="play">{ic('play', size=18)}</span><span class="new" style="display:flex;padding:4px">{ic('bookmark', '#A8C7FA')}</span><span style="font-size:13px">12:58</span></div>
</div>
<div class="scrim"></div><div class="sheet new">
 <h3>Salva come decisione</h3>
 <p class="sub">La master la scrive nella sua memoria: la leggono il giro novità e le sessioni.</p>
 <div class="field">I prezzi del checkout includono sempre l'IVA, anche per le aziende; si toglie solo in fattura.</div>
 <div class="lab">Vale per</div>
 <div style="display:flex;gap:8px"><span class="tonal" style="padding:7px 14px;background:#2E3A55;color:var(--primary)">atlas-shop</span><span class="tonal" style="padding:7px 14px">Tutti i progetti</span></div>
 <div class="btns" style="justify-content:flex-end;margin-top:18px"><span class="textb" style="color:var(--text2)">Annulla</span><span class="filled">Salva</span></div>
</div>''',
    'Il segnalibro accanto a Copia e ▶ sotto ogni risposta di Claude; il testo si può correggere prima di salvare. '
    'Il progetto parte da quello della sessione. Anche dal + della master, «Salva una decisione», con il campo vuoto.')

# 7 — E: pulizia delle sessioni finite e dei doppioni.
screens['7-pulizia'] = page(
    'MOCKUP 1.37 · E · PULIZIA',
    f'''<div class="top"><h1>Master</h1><span>4 aperte</span></div>
<div class="grp" style="color:var(--good)">HA FINITO · 3</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:#F4D03F">{ic('pause', '#3A2E00', 14)}</span><b style="flex:1">field-notes</b><span class="mono">ctx 4%</span>{ic('chev', '#B0B8C4')}</div>
 <div class="line">README rewritten with the three sections asked for.</div>
 <div class="btns new" style="padding:2px"><span class="sub" style="flex:1">Compito chiuso, senza finestra</span><span class="tonal" style="display:flex;gap:8px;align-items:center;padding:9px 16px">{ic('power', '#E5736B', 18)}Chiudi</span></div>
</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:#F4D03F">{ic('pause', '#3A2E00', 14)}</span><b style="flex:1">field-notes-2</b><span class="chip dup new">DOPPIONE</span>{ic('chev', '#B0B8C4')}</div>
 <div class="line">Doppione di field-notes: stessa conversazione</div>
 <div class="btns new" style="padding:2px"><span class="sub" style="flex:1">Aperta alle 12:41, dopo l'altra</span><span class="tonal" style="display:flex;gap:8px;align-items:center;padding:9px 16px">{ic('power', '#E5736B', 18)}Chiudi</span></div>
</div>
<div class="card">
 <div class="ch"><span class="badge" style="background:var(--good)">{ic('bolt', size=16)}</span><b style="flex:1">orbit-docs</b><span class="mono">ctx 31%</span>{ic('chev', '#B0B8C4')}</div>
 <div class="line">Docs published.</div>
 <div class="btns new" style="padding:2px"><span class="sub" style="flex:1">Finestra aperta sul PC: si chiude da lì</span></div>
</div>''',
    '«Chiudi» solo per una sessione ferma col compito chiuso, o per un doppione, e solo senza finestra aperta (attached falso): '
    'manda /exit dopo la conferma di oggi. Con la finestra aperta solo la riga che lo dice; mai detach-client.')

for name, html in screens.items():
    (HERE / f'{name}.html').write_text(html, encoding='utf-8')
    subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', '--force-device-scale-factor=2',
                    '--window-size=412,1010', '--virtual-time-budget=3000', f'--screenshot={HERE / (name + ".png")}',
                    (HERE / f'{name}.html').as_uri()], check=True, capture_output=True, timeout=60)
    print(name)
