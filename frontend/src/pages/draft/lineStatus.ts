import type { DraftLineResponse, ProductResponse } from '../../api/types'

export type LineResolution =
  | { kind: 'match'; product: ProductResponse }
  | { kind: 'create'; name: string; brand: string | null }
  | { kind: 'skipped'; reason: string }

export function resolveLine(line: DraftLineResponse, products: ProductResponse[]): LineResolution {
  switch (line.action) {
    case 'MATCH': {
      const product = products.find((candidate) => candidate.id === line.productId)
      return product ? { kind: 'match', product } : { kind: 'skipped', reason: 'Товар не выбран' }
    }
    case 'CREATE':
      return line.proposedName
        ? { kind: 'create', name: line.proposedName, brand: line.proposedBrand }
        : { kind: 'skipped', reason: 'Не указано название нового товара' }
    case 'UNSURE':
      return { kind: 'skipped', reason: 'Строка не распознана' }
  }
}

export function willBeApplied(resolution: LineResolution): boolean {
  return resolution.kind !== 'skipped'
}
