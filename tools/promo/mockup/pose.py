"""Posa della cupola dalla sua ellisse: modello di camera a foro stenopeico, così l'interfaccia si appoggia sul piano
del display con la prospettiva vera (convergenza compresa) invece di un parallelogramma stimato a occhio."""
import numpy as np
F=5740.0; PP=np.array([940.0,969.0])            # focale e punto principale nello spazio di q10.png (da EXIF: 24 mm eq. × 2,11)
E=dict(cx=872.0,cy=880.0,A=525.0,B=640.0,rot=7.0)   # ellisse del bordo del vetro misurata sulla foto
def basis(n):
    w=np.array([0,1.0,0]); ey=w-np.dot(w,n)*n; ey/=np.linalg.norm(ey); ex=np.cross(ey,n); ex/=np.linalg.norm(ex)
    if ex[0]<0: ex=-ex
    return ex,ey
def model(p):
    x0,y0,D,th,ps=p; d=np.array([(x0-PP[0])/F,(y0-PP[1])/F,1.0]); d/=np.linalg.norm(d); C=D*d
    n=np.array([np.sin(th)*np.cos(ps),np.sin(th)*np.sin(ps),-np.cos(th)]); return C,n
def proj(P): return PP+F*P[...,:2]/P[...,2:3]
def err(p):
    C,n=model(p); ex,ey=basis(n); t=np.linspace(0,2*np.pi,72,endpoint=False)
    q=proj(C+np.cos(t)[:,None]*ex+np.sin(t)[:,None]*ey)
    c,s=np.cos(np.deg2rad(E['rot'])),np.sin(np.deg2rad(E['rot'])); u=(q[:,0]-E['cx'])*c+(q[:,1]-E['cy'])*s; v=-(q[:,0]-E['cx'])*s+(q[:,1]-E['cy'])*c
    return np.mean(((u/E['A'])**2+(v/E['B'])**2-1)**2)
def nelder(f,x0,step,it=900):
    n=len(x0); sim=[np.array(x0,float)]+[np.array(x0,float)+np.eye(n)[i]*step[i] for i in range(n)]; val=[f(s) for s in sim]
    for _ in range(it):
        o=np.argsort(val); sim=[sim[i] for i in o]; val=[val[i] for i in o]; c=np.mean(sim[:-1],axis=0)
        r=c+(c-sim[-1]); fr=f(r)
        if fr<val[0]:
            e=c+2*(c-sim[-1]); fe=f(e); sim[-1],val[-1]=(e,fe) if fe<fr else (r,fr)
        elif fr<val[-2]: sim[-1],val[-1]=r,fr
        else:
            k=c+0.5*(sim[-1]-c); fk=f(k)
            if fk<val[-1]: sim[-1],val[-1]=k,fk
            else:
                sim=[sim[0]]+[sim[0]+0.5*(s-sim[0]) for s in sim[1:]]; val=[val[0]]+[f(s) for s in sim[1:]]
    return sim[0],val[0]
def fit():
    th0=np.arccos(E['A']/E['B']); ps0=np.deg2rad(180+E['rot'])          # il lato della corona (destra) è il più vicino: la normale pende a sinistra
    p,v=nelder(err,[E['cx'],E['cy'],F/E['B'],th0,ps0],[8,8,0.4,0.05,0.05]); return p,v
def display_quad(p,k=0.86,depth=0.03,axis_deg=0.0):
    """Angoli (TL,TR,BR,BL) del quadrato che contiene il display di raggio k, su un piano parallelo al bordo del vetro e più in fondo di `depth` raggi."""
    C,n=model(p); ex,ey=basis(n); a=np.deg2rad(axis_deg); ex,ey=np.cos(a)*ex+np.sin(a)*ey, -np.sin(a)*ex+np.cos(a)*ey
    Cd=C-depth*n                           # n guarda la camera: «più in fondo» vuol dire allontanarsi da lei
    pts=[Cd+k*(sx*ex+sy*ey) for sx,sy in ((-1,-1),(1,-1),(1,1),(-1,1))]; return [proj(P[None,:])[0] for P in pts]
if __name__=='__main__':
    p,v=fit(); C,n=model(p); print('parametri',np.round(p,3),'errore',f'{v:.2e}'); print('distanza camera/raggio =',round(p[2],2),'· inclinazione =',round(np.degrees(p[3]),1),'° · normale',np.round(n,3))
    for q in display_quad(p): print(np.round(q,1))
