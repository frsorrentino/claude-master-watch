// Le anteprime già arrivate: «sessione\npercorso» → indirizzo del blob. Restano finché la pagina è aperta, così la chat che
// si ridisegna non le richiede al PC.
export const media: Record<string, string> = $state({})
export const mediaKey = (session: string, path: string) => `${session}\n${path}`
