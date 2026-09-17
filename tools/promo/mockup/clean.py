"""Toglie i puntini chiari (polvere) dalle zone scure: cinturino, anse e interno del vetro. Un puntino è un pixel molto
più chiaro della mediana locale; i riflessi veri sono grandi e la mediana li lascia stare. Corona e bordo della cupola
restano intatti: lì i segni chiari sono dettaglio, non polvere."""
import numpy as np
from PIL import Image, ImageFilter
def despeck(img, zone, size=13, thr=12):
    L=img.convert('L'); med=L.filter(ImageFilter.MedianFilter(size))
    a=np.asarray(L).astype(np.int16); m=np.asarray(med).astype(np.int16)
    sp=((a-m)>thr)&(m<120)&(np.asarray(zone)>0)
    mask=Image.fromarray((sp*255).astype(np.uint8)).filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.GaussianBlur(1.2))
    out=img.copy(); out.paste(img.filter(ImageFilter.MedianFilter(size)),(0,0),mask)
    return out,int(sp.sum())
