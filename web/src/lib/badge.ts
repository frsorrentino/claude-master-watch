import type { SessionState } from './contract'

// Il badge della sessione, porta di Badge.kt (core): forma = account (tondo personale, quadrato lavoro), riempimento = colore
// della sessione (o dall'emoji, o grigio), glifo di stato Lucide nero o bianco scelto dal contrasto WCAG (≥ 4,5:1).
export type Glyph = 'hand' | 'zap' | 'pause' | 'cross'
export type Spec = { square: boolean; fill: string; glyph: Glyph; ink: string }

export const GREY = '#9B9B9B'
const BLACK = '#000000'
const WHITE = '#F2F4F7'
const EMOJI: Record<string, string> = {
  '🟠': '#F5A623', '🟧': '#F5A623', '🧡': '#F5A623', '🟡': '#F4D03F', '🟨': '#F4D03F', '💛': '#F4D03F',
  '🔴': '#E74C3C', '🟥': '#E74C3C', '❤️': '#E74C3C', '🟢': '#2ECC71', '🟩': '#2ECC71', '💚': '#2ECC71',
  '🔵': '#3B82F6', '🟦': '#3B82F6', '💙': '#3B82F6', '🟣': '#9B59B6', '🟪': '#9B59B6', '💜': '#9B59B6',
  '⚪': '#BDC3C7', '⬜': '#BDC3C7', '🤍': '#BDC3C7', '🟤': '#8D6E63', '🟫': '#8D6E63', '🤎': '#8D6E63',
}

export function personal(account: string, kind?: string | null): boolean {
  return kind != null ? kind === 'personal' : account.toLowerCase() === 'personale'
}

function parse(c?: string | null): string | null {
  const h = c?.trim().replace(/^#/, '')
  return h && /^[0-9a-fA-F]{6}$/.test(h) ? `#${h.toUpperCase()}` : null
}

function luminance(hex: string): number {
  const ch = (i: number) => { const c = parseInt(hex.slice(i, i + 2), 16) / 255; return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4 }
  return 0.2126 * ch(1) + 0.7152 * ch(3) + 0.0722 * ch(5)
}
function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

export function badge(account: string, color: string | null | undefined, state: SessionState, icon?: string | null, kind?: string | null): Spec {
  const fill = parse(color) ?? parse(EMOJI[icon?.trim() ?? '']) ?? GREY
  const glyph: Glyph = state === 'busy' || state === 'awaiting' ? 'zap' : state === 'idle' ? 'pause' : state === 'waiting' ? 'hand' : 'cross'
  return { square: !personal(account, kind), fill, glyph, ink: contrast(BLACK, fill) >= 4.5 ? BLACK : WHITE }
}

/** Il badge respira mentre la sessione lavora (come il pallino dell'app Claude). */
export const breathes = (s: SessionState) => s === 'busy' || s === 'awaiting'

/** I tracciati dei glifi, gli stessi di Badge.paths (Lucide, ISC), in un riquadro 24×24. */
export const PATHS: Record<Glyph, string[]> = {
  hand: ['M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2', 'M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2', 'M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8', 'M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15'],
  zap: ['M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z'],
  pause: ['M9 6v12', 'M15 6v12'],
  cross: ['M18 6 6 18', 'M6 6l12 12'],
}
