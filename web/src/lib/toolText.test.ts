import { describe, expect, it } from 'vitest'
import { kind, phrase, row, short, type Labels } from './toolText'

// Gli stessi casi di ToolTextTest in Kotlin.
const l: Labels = {
  run: 'esegue %1$s', read: 'legge %1$s', edit: 'modifica %1$s', write: 'scrive %1$s',
  search: 'cerca %1$s', web: 'cerca sul web', message: "scrive a un'altra sessione",
  delegate: 'delega a un agente', plan: 'aggiorna il piano', other: 'usa %1$s',
}

describe('ToolText', () => {
  it('il nome nudo diventa una frase', () => {
    expect(phrase('SendMessage', l)).toBe("scrive a un'altra sessione")
    expect(phrase('Task', l)).toBe('delega a un agente')
    expect(phrase('TodoWrite', l)).toBe('aggiorna il piano')
    expect(phrase('WebFetch https://esempio.it/pagina', l)).toBe('cerca sul web')
  })
  it('il comando resta per esteso', () => {
    expect(phrase('Bash pytest -q tests', l)).toBe('esegue pytest -q tests')
  })
  it('dei percorsi resta solo il file', () => {
    expect(phrase('Read core/src/main/kotlin/it/pixelbox/cmwatch/rules/TileTexts.kt', l)).toBe('legge TileTexts.kt')
    expect(phrase('Edit(wear/src/main/kotlin/it/pixelbox/cmwatch/wear/push/Notifier.kt)', l)).toBe('modifica Notifier.kt')
  })
  it('uno strumento sconosciuto si dice comunque', () => {
    expect(phrase('Paparazzi', l)).toBe('usa Paparazzi')
  })
  it('niente da dire resta niente', () => {
    expect(phrase(null, l)).toBeNull()
    expect(phrase('   ', l)).toBeNull()
  })
  it('kind of tools', () => {
    expect(kind('Bash')).toBe('run')
    expect(kind('Read')).toBe('read')
    expect(kind('MultiEdit')).toBe('edit')
    expect(kind('Write')).toBe('write')
    expect(kind('Grep')).toBe('search')
    expect(kind(null)).toBe('other')
  })
  it('bash row shows the description then the command', () => {
    expect(row('Bash', 'grep -n source cm-config.py', 'Trova la cartella')).toEqual(['Trova la cartella', 'grep -n source cm-config.py'])
    expect(row('Bash', 'ls -la', null)).toEqual(['ls -la', null])
  })
  it('file row shows the name then the folder', () => {
    expect(row('Read', 'claude-master/scripts/cm-quota.py', null)).toEqual(['cm-quota.py', 'claude-master/scripts'])
    expect(row('Edit', 'notes.py', null)).toEqual(['notes.py', null])
  })
  it('mcp tools go by their short name', () => {
    expect(short('mcp__chrome-bridge__execute_js')).toBe('execute_js')
    expect(short('Bash')).toBe('Bash')
    expect(row('mcp__chrome-bridge__execute_js', '', null)).toEqual(['execute_js', null])
    expect(row(null, null, null)).toEqual(['?', null])
  })
  it('browser mcp tools are web', () => {
    expect(kind('mcp__chrome-bridge__click')).toBe('web')
    expect(kind('mcp__claude-in-chrome__navigate')).toBe('web')
    expect(kind('mcp__firebase__auth_get_users')).toBe('other')
  })
})
