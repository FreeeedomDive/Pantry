export type PantryRole = 'OWNER' | 'MEMBER'

export interface PantryResponse {
  id: string
  name: string
  role: PantryRole
  isDefault: boolean
}

export interface PantryMemberResponse {
  telegramUserId: number
  role: PantryRole
  joinedAt: string
}

export interface InviteResponse {
  link: string
  expiresAt: string
}

export interface ProductResponse {
  id: string
  name: string
  brand: string | null
  isStaple: boolean
}

export interface ProductBalanceResponse {
  product: ProductResponse
  total: number
}

export interface StockItemResponse {
  id: string
  quantity: number
  purchasedAt: string
  expiresAt: string | null
}

export type DraftStatus = 'EXTRACTED' | 'MATCHING' | 'READY' | 'CONFIRMED' | 'FAILED'
export type DraftLineAction = 'MATCH' | 'CREATE' | 'UNSURE'
export type Confidence = 'HIGH' | 'LOW'

export interface DraftLineResponse {
  id: string
  rawText: string
  action: DraftLineAction
  productId: string | null
  proposedName: string | null
  proposedBrand: string | null
  quantity: number
  confidence: Confidence
  expiresAt: string | null
}

export interface DraftResponse {
  id: string
  pantryId: string
  status: DraftStatus
  lines: DraftLineResponse[]
}

export interface DraftLineInput {
  rawText: string
  action: DraftLineAction
  productId: string | null
  proposedName: string | null
  proposedBrand: string | null
  quantity: number
  expiresAt: string | null
}

export interface UpdateDraftRequest {
  lines: DraftLineInput[]
}

export interface CreatePantryRequest {
  name: string
}

export interface WriteOffStockRequest {
  quantity: number
}

export interface StapleProductRequest {
  isStaple: boolean
}

export interface RenameProductRequest {
  name: string
  brand: string | null
}
