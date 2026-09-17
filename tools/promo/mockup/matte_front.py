"""Scontorno frontale «per costruzione» (Franz, 17/09 18:05: bordo di alluminio tagliato ai lati, sbavature sull'ansa).
La cassa è un cerchio: lo misuro sul bordo esterno dell'anello di alluminio e lo disegno geometrico, così il filo chiaro
dello smusso resta dentro e il bordo è pulito. Cinturino e anse vengono dalla soglia sul nero, lisciati; la corona da una
soglia sua. Alla fine i pixel di bordo prendono il colore dell'interno, per togliere l'alone beige del tessuto."""
import sys, numpy as np
from PIL import Image, ImageFilter, ImageDraw
S=sys.argv[1]
im=Image.open(f'{S}/f5_clean.png').convert('RGB'); N=im.width
G=np.asarray(im.convert('L')).astype(np.float32); cx0,cy0,R0=np.load(f'{S}/f5_circle.npy')
def sample(x,y):
    x0=np.floor(x).astype(int); y0=np.floor(y).astype(int); fx=x-x0; fy=y-y0
    return G[y0,x0]*(1-fx)*(1-fy)+G[y0,x0+1]*fx*(1-fy)+G[y0+1,x0]*(1-fx)*fy+G[y0+1,x0+1]*fx*fy
pts=[]
sectors=[(125,235),(-52,-14),(30,50)]           # fianchi liberi: niente anse, cinturino, corona
rs=np.arange(R0-26,R0+26,0.25)
for a0,a1 in sectors:
    for deg in np.arange(a0,a1,0.5):
        prof=np.median([sample(cx0+rs*np.cos(np.deg2rad(deg+d)),cy0+rs*np.sin(np.deg2rad(deg+d))) for d in (-0.4,-0.2,0,0.2,0.4)],axis=0)
        ring=np.median(prof[(rs>R0-18)&(rs<R0-8)]); cloth=np.median(prof[(rs>R0+10)&(rs<R0+24)]); mid=(ring+cloth)/2
        idx=np.where(prof<mid)[0]
        if len(idx)==0: continue
        r=rs[idx.max()]                          # ultimo punto ancora «anello»: il bordo esterno
        pts.append((cx0+r*np.cos(np.deg2rad(deg)),cy0+r*np.sin(np.deg2rad(deg))))
P=np.array(pts)
def circle(P):
    A=np.c_[2*P[:,0],2*P[:,1],np.ones(len(P))]; b=(P**2).sum(axis=1); s=np.linalg.lstsq(A,b,rcond=None)[0]; return s[0],s[1],np.sqrt(s[2]+s[0]**2+s[1]**2)
cx,cy,R=circle(P)
for _ in range(4):                               # scarto i punti lontani (pelucchi, riflessi) e riadatto
    d=np.abs(np.hypot(P[:,0]-cx,P[:,1]-cy)-R); P=P[d<max(1.2,2.5*np.median(d))]; cx,cy,R=circle(P)
d=np.hypot(P[:,0]-cx,P[:,1]-cy)-R
print(f'bordo dell\'anello scuro: centro ({cx:.2f},{cy:.2f}) raggio {R:.2f} px · {len(P)} punti · scarto medio {np.abs(d).mean():.2f} px (prima: centro ({cx0:.1f},{cy0:.1f}) raggio {R0:.1f})')
for name,(a0,a1) in zip(('sinistra','destra alto','destra basso'),sectors):
    ang=np.degrees(np.arctan2(P[:,1]-cy,P[:,0]-cx)); ang=np.where(ang>236,ang-360,ang); sel=(ang>=a0)&(ang<a1)
    if sel.any(): print(f'  settore {name}: scarto medio dal cerchio {d[sel].mean():+.2f} px')
CH=7.8                                           # larghezza dello smusso lucido: misurata (luminosità a destra, ruvidità a sinistra e in basso)
np.save(f'{S}/f5_case.npy',np.array([cx,cy,R+CH]))
SS=4; M=Image.new('L',(N*SS,N*SS),0); dr=ImageDraw.Draw(M); Rc=R+CH
dr.ellipse([(cx-Rc)*SS,(cy-Rc)*SS,(cx+Rc)*SS,(cy+Rc)*SS],fill=255)
case=np.asarray(M.resize((N,N),Image.LANCZOS)).astype(np.float32)/255
yy,xx=np.mgrid[0:N,0:N]; rr=np.hypot(xx-cx,yy-cy)
Gb=np.asarray(im.convert('L').filter(ImageFilter.GaussianBlur(1.2))).astype(np.float32)
dark=((Gb<95)&(rr>R*0.98)).astype(np.uint8)*255                         # cinturino e anse: neri veri
dk=Image.fromarray(dark).filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.MinFilter(5))
seedimg=dk.copy()
for p in ((int(cx),int(cy-R*1.5)),(int(cx),int(cy+R*1.5))):            # tengo solo i pezzi attaccati al cinturino, sopra e sotto
    if seedimg.getpixel(p)==255: ImageDraw.floodfill(seedimg,p,77)
band=((np.asarray(seedimg)==77)*255).astype(np.uint8)
band=Image.fromarray(band).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(2.2)).point(lambda v:255 if v>128 else 0).filter(ImageFilter.GaussianBlur(0.9))
band=np.asarray(band).astype(np.float32)/255
cb=(xx>cx+R*0.97)&(xx<cx+R*1.22)&(np.abs(yy-(cy+R*0.06))<R*0.24)      # riquadro della corona
# La corona è scura con riflessi chiari PICCOLI; sotto di lei c'è l'ombra sul tessuto, a mezza luce: con la soglia a 150
# l'ombra entrava nella maschera come una macchia beige a gradini (prova delle 18:20). Soglia sul nero, poi chiusura
# larga per riprendere i riflessi della zigrinatura, e solo il pezzo attaccato alla cassa.
cr=((Gb<105)&cb).astype(np.uint8)*255
cr=Image.fromarray(cr).filter(ImageFilter.MaxFilter(13)).filter(ImageFilter.MinFilter(13))
cs=cr.copy(); seedp=None
for x in range(int(cx+R*1.0),int(cx+R*1.2)):
    if cs.getpixel((x,int(cy+R*0.06)))==255: seedp=(x,int(cy+R*0.06)); break
if seedp: ImageDraw.floodfill(cs,seedp,77)
cr=Image.fromarray(((np.asarray(cs)==77)*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.0)).point(lambda v:255 if v>118 else 0).filter(ImageFilter.GaussianBlur(0.9))
crown=np.asarray(cr).astype(np.float32)/255
alpha=np.maximum(np.maximum(case,band),crown)
# colore dei bordi dall'interno: media dei pixel pieni vicini, pesata (sfocatura premoltiplicata)
solid=np.asarray(Image.fromarray((alpha*255).astype(np.uint8)).filter(ImageFilter.MinFilter(7))).astype(np.float32)/255; solid=(solid>0.99).astype(np.float32)
rgb=np.asarray(im).astype(np.float32); num=np.stack([np.asarray(Image.fromarray((rgb[...,c]*solid).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4))).astype(np.float32) for c in range(3)],axis=-1)
den=np.asarray(Image.fromarray((solid*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4))).astype(np.float32)/255
inner=num/np.maximum(den[...,None],1e-3); edge=((solid<0.5)&(alpha>0.01))[...,None]
# sul cerchio della cassa NON tocco il colore (lo smusso chiaro è vero); ripulisco solo i bordi di cinturino, anse e corona
edge=edge&((case<0.5)|(rr<R-3))[...,None]
fixed=np.where(edge&(den[...,None]>0.05),inner,rgb)
ang=np.arctan2(yy-cy,xx-cx); key=np.clip(np.cos(ang-np.deg2rad(-140)),0,1)**2          # luce principale in alto a sinistra, come sul vetro
w=np.clip((rr-(R+0.5))/1.5,0,1)*np.clip(((R+CH+1.5)-rr)/1.5,0,1)                          # peso: solo la fascia dello smusso, sfumata
lum=fixed.mean(axis=2,keepdims=True); relit=lum*(0.30+0.55*key[...,None])*np.array([0.86,0.93,1.10])
fixed=fixed*(1-w[...,None])+relit*w[...,None]
Image.fromarray(np.clip(fixed,0,255).astype(np.uint8)).save(f'{S}/f5_body.png'); Image.fromarray((alpha*255).astype(np.uint8)).save(f'{S}/f5_mask.png')
