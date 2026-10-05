import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { execSync } from 'node:child_process'

// La versione mostrata nelle Impostazioni: il commit da cui è costruita la web app.
const version = (() => { try { return execSync('git rev-parse --short HEAD').toString().trim() } catch { return 'sviluppo' } })()

// Le fixture del contratto stanno fuori dalla cartella della web app (../contract): i dati di prova le leggono da lì.
export default defineConfig({
  plugins: [svelte()],
  define: { __APP_VERSION__: JSON.stringify(version) },
  server: { fs: { allow: ['..'] } },
})
