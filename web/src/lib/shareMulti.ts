// Contratto 1.50 (Franz, 10/10 16:52: «due allegati insieme vengono mostrati ancora su 2 post»): più allegati in un
// `report` solo, come ShareLimits.together sul telefono.
type Share = { multi?: number } | null | undefined

/** `n` allegati vanno insieme quando il relay lo dice (`share.multi`) e sono almeno due. */
export const together = (share: Share, n: number) => n >= 2 && n <= (share?.multi ?? 0)

/** I gruppi da mandare: fino a `share.multi` per report, uno solo per report senza. */
export function groups<T>(xs: T[], share: Share): T[][] {
  const size = Math.max(1, share?.multi ?? 1)
  const out: T[][] = []
  for (let i = 0; i < xs.length; i += size) out.push(xs.slice(i, i + size))
  return out
}
