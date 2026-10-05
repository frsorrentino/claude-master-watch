import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'

const app = mount(App, {
  target: document.getElementById('app')!,
})

// Installabile come app (PWA): il service worker solo nella build, in sviluppo ricaricherebbe file vecchi.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => { navigator.serviceWorker.register('/sw.js').catch(() => { /* senza, l'app va lo stesso */ }) })
}

export default app
