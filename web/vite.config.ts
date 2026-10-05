import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

// Le fixture del contratto stanno fuori dalla cartella della web app (../contract): i dati di prova le leggono da lì.
export default defineConfig({
  plugins: [svelte()],
  server: { fs: { allow: ['..'] } },
})
