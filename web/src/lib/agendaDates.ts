// «ven 10/10» da AAAA-MM-GG, com'è se non si legge (untilLabel di RecapSection.kt).
export function untilLabel(iso: string | null | undefined): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec((iso ?? '').trim())
  if (!m) return iso ?? ''
  const d = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  return `${new Intl.DateTimeFormat('it-IT', { weekday: 'short' }).format(d)} ${m[3]}/${m[2]}`
}
