import { describe, expect, it } from 'vitest'
import { groups, together } from './shareMulti'

describe('più allegati in un report solo (contratto 1.50)', () => {
  it('insieme solo se il relay lo dice e sono almeno due', () => {
    expect(together({ multi: 5 }, 2)).toBe(true)
    expect(together({ multi: 5 }, 1)).toBe(false)
    expect(together({}, 2)).toBe(false)
    expect(together(null, 3)).toBe(false)
  })
  it('a gruppi di multi, uno per uno senza', () => {
    expect(groups([1, 2, 3, 4, 5, 6, 7], { multi: 5 })).toEqual([[1, 2, 3, 4, 5], [6, 7]])
    expect(groups([1, 2], null)).toEqual([[1], [2]])
  })
})
