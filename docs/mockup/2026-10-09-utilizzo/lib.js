// Anello doppio come sul polso e nella scheda vecchia: 5 ore fuori (azzurro), settimana dentro (lavanda).
function ring(size, h5, w7, stale, mark){
  const w=size*0.1, r1=size/2-w/2, r2=r1-w-4, c=size/2, C=x=>2*Math.PI*x
  const arc=(r,col,p)=>`<circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="${col}" stroke-width="${w}" stroke-linecap="round" stroke-dasharray="${C(r)*p} ${C(r)}" transform="rotate(-90 ${c} ${c})"/>`
  return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" style="flex:none">`
    +`<circle cx="${c}" cy="${c}" r="${r1}" fill="none" stroke="#3A4150" stroke-width="${w}"/><circle cx="${c}" cy="${c}" r="${r2}" fill="none" stroke="#3A4150" stroke-width="${w}"/>`
    +(stale?'':arc(r1, h5>=90?'#FF6B6B':'#7FA1FF', h5/100))+(w7==null?'':arc(r2,'#C3A6F5',w7/100))
    +(mark==='sq'?`<rect x="${c-6}" y="${c-6}" width="12" height="12" rx="3" fill="none" stroke="#B0B8C4" stroke-width="2"/>`:`<circle cx="${c}" cy="${c}" r="6" fill="none" stroke="#B0B8C4" stroke-width="2"/>`)+`</svg>`
}
// Il grafico delle 5 ore com'è oggi in home: 100/80/50, i campioni pieni, la previsione tratteggiata, «ora».
function chart(W,H,pts,nowX,proj,start,end,nowPct){
  const L=30,T=4,B=16,w=W-L-8,h=H-B-T, X=f=>L+w*f, Y=p=>T+h*(1-p/100)
  let s=`<svg width="${W}" height="${H}" style="display:block">`
  ;[[100,'rgba(255,255,255,.10)'],[50,'rgba(255,255,255,.06)']].forEach(([p,c])=>{s+=`<line x1="${L}" x2="${L+w}" y1="${Y(p)}" y2="${Y(p)}" stroke="${c}"/><text x="0" y="${Y(p)+4}" fill="#B0B8C4" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">${p}</text>`})
  s+=`<line x1="${L}" x2="${L+w}" y1="${Y(80)}" y2="${Y(80)}" stroke="#FFB020" stroke-dasharray="6 6"/><text x="0" y="${Y(80)+4}" fill="#FFB020" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">80</text>`
  s+=`<line x1="${X(nowX)}" x2="${X(nowX)}" y1="${T}" y2="${T+h}" stroke="rgba(176,184,196,.6)" stroke-dasharray="2 4"/>`
  const d=pts.map(([x,p],i)=>(i?'L':'M')+X(x)+' '+Y(p)).join(' ')
  s+=`<path d="${d} L${X(pts.at(-1)[0])} ${T+h} L${X(pts[0][0])} ${T+h} Z" fill="rgba(127,161,255,.12)"/><path d="${d}" fill="none" stroke="#7FA1FF" stroke-width="2" stroke-linecap="round"/>`
  if(proj!=null){s+=`<line x1="${X(nowX)}" y1="${Y(nowPct)}" x2="${X(1)}" y2="${Y(proj)}" stroke="#7FA1FF" stroke-width="1.5" stroke-dasharray="4 4"/><circle cx="${X(1)}" cy="${Y(proj)}" r="4" fill="#1B1F26" stroke="#7FA1FF" stroke-width="1.5"/><text x="${X(1)-26}" y="${Y(proj)+16}" fill="#F2F4F7" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">${proj}%</text>`}
  s+=`<circle cx="${X(nowX)}" cy="${Y(nowPct)}" r="3.5" fill="#7FA1FF"/>`
  s+=`<text x="${L}" y="${H-2}" fill="#B0B8C4" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">${start}</text><text x="${X(nowX)-18}" y="${H-2}" fill="#F2F4F7" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">ora ${nowPct}%</text><text x="${L+w-30}" y="${H-2}" fill="#B0B8C4" font-size="10" font-family="Roboto Mono, DejaVu Sans Mono, monospace">${end}</text></svg>`
  return s
}
// La scheda unita: in testa l'anello (lo stile del polso) col numero, sotto il grafico previsionale e la settimana.
function quotaCard(o, W){
  if(o.stale) return `<div class="q"><div class="qh">${ring(56,0,o.w7,true,o.mark)}<div class="t"><div class="mono">5 ORE · ${o.acc.toUpperCase()}</div><div class="big" style="color:var(--text2)">—<small>si azzera alle ${o.reset}</small></div><span class="chip">settimana ${o.w7}% · ${o.wreset}</span> <span style="color:var(--warn);font-size:12.5px;margin-left:6px">dato vecchio</span></div></div></div>`
  return `<div class="q"><div class="qh">${ring(64,o.h5,o.w7,false,o.mark)}<div class="t"><div class="mono">5 ORE · ${o.acc.toUpperCase()}</div><div class="big">${o.h5}%<small>si azzera alle ${o.reset}</small></div>${o.sched?`<div class="note2" style="color:var(--ring);margin-top:2px">${o.sched}</div>`:''}</div></div>`
   +`<div style="margin-top:10px">${chart(W-30,84,o.pts,o.nowX,o.proj,o.start,o.reset,o.h5)}</div><div class="note" style="margin-top:6px">${o.fnote}</div>`
   +`<div class="hr"></div><div style="display:flex"><span class="mono" style="flex:1">SETTIMANA · ${o.acc.toUpperCase()}</span><span style="font:13px var(--mono)">${o.w7}%</span></div><div class="wk"><s style="width:${o.wproj}%"></s><b style="width:${o.w7}%"></b><u></u></div><div class="note">Allo stesso ritmo: ${o.wproj}% al rinnovo di ${o.wresetLong}</div></div>`
}
function today(W){
  const v=[0,0,0,0,0,0,0,2,5,9,7,4,3,6,8,11,6,null,null,null,null,null,null,null]; const pk=11
  const step=(W-30)/24, bw=step*.6, H=56
  let s=`<svg width="${W-30}" height="${H}" style="display:block">`
  v.forEach((c,i)=>{const x=i*step+(step-bw)/2; if(c==null) s+=`<circle cx="${x+bw/2}" cy="${H-2}" r="2" fill="#3A4150"/>`; else {const t=c?H*c/pk:3; s+=`<rect x="${x}" y="${H-t}" width="${bw}" height="${t}" rx="${bw/2}" fill="${c?'#7FA1FF':'#3A4150'}"/>`}})
  s+=`</svg>`
  return `<div class="q"><div style="display:flex;align-items:baseline;gap:8px"><span class="mono" style="flex:1">OGGI · EVENTI PER ORA</span><span style="font:13px var(--mono)">61</span></div><div style="margin-top:10px">${s}</div><div style="display:flex;font:10px var(--mono);color:var(--text2);margin-top:4px"><span style="flex:1">0</span><span style="flex:1">6</span><span style="flex:1">12</span><span style="flex:1">18</span></div></div>`
}
const P={acc:'personal',mark:'o',h5:34,w7:41,reset:'20:00',start:'15:00',nowX:.24,pts:[[0,8],[.08,14],[.16,25],[.24,34]],proj:62,fnote:'Al ritmo di adesso arrivi al 62% quando riparte, alle 20:00',wproj:70,wreset:'gio 04:00',wresetLong:'giovedì 04:00',sched:'2 invii programmati alla ripartenza, il primo alle 20:01'}
const Wk={acc:'work',mark:'sq',stale:true,w7:75,reset:'15:00',wreset:'mar 04:00'}
