import type { APIRequestContext, APIResponse } from '@playwright/test'
import type {
  PantryMemberResponse,
  PantryResponse,
  ProductBalanceResponse,
  ProductResponse,
  StockItemResponse,
} from '../src/api/types.js'

export interface CreateProductRequest {
  name: string
  brand: string | null
}

export interface CreateStockBatchRequest {
  quantity: number
  expiresAt: string | null
}

export interface AddPantryMemberRequest {
  telegramUserId: number
}

async function responseBody(
  operation: string,
  response: APIResponse,
  expectedStatus = 200,
): Promise<string> {
  const body = await response.text()
  if (response.status() !== expectedStatus) {
    throw new Error(
      `${operation} failed: expected ${expectedStatus}, received ${response.status()} ${response.statusText()} ${response.url()}\n${body || '<empty response>'}`,
    )
  }

  return body
}

async function responseJson<T>(operation: string, response: APIResponse): Promise<T> {
  const body = await responseBody(operation, response)

  try {
    return JSON.parse(body) as T
  } catch (error) {
    throw new Error(
      `${operation} returned invalid JSON: ${response.status()} ${response.url()}\n${body || '<empty response>'}`,
      { cause: error },
    )
  }
}

export async function listPantries(api: APIRequestContext): Promise<PantryResponse[]> {
  return responseJson('List pantries', await api.get('/api/pantries'))
}

export async function createPantry(
  api: APIRequestContext,
  name: string,
): Promise<PantryResponse> {
  return responseJson('Create pantry', await api.post('/api/pantries', { data: { name } }))
}

export async function createProduct(
  api: APIRequestContext,
  pantryId: string,
  request: CreateProductRequest,
): Promise<ProductResponse> {
  return responseJson(
    'Create product',
    await api.post(`/api/pantries/${pantryId}/products`, { data: request }),
  )
}

export async function createStockBatch(
  api: APIRequestContext,
  pantryId: string,
  productId: string,
  request: CreateStockBatchRequest,
): Promise<StockItemResponse> {
  return responseJson(
    'Create stock batch',
    await api.post(`/api/pantries/${pantryId}/products/${productId}/stock`, { data: request }),
  )
}

export async function setStaple(
  api: APIRequestContext,
  pantryId: string,
  productId: string,
  isStaple: boolean,
): Promise<ProductResponse> {
  return responseJson(
    'Set product staple status',
    await api.patch(`/api/pantries/${pantryId}/products/${productId}/staple`, {
      data: { isStaple },
    }),
  )
}

export async function getBalance(
  api: APIRequestContext,
  pantryId: string,
): Promise<ProductBalanceResponse[]> {
  return responseJson('Get pantry balance', await api.get(`/api/pantries/${pantryId}/balance`))
}

export async function getMembers(
  api: APIRequestContext,
  pantryId: string,
): Promise<PantryMemberResponse[]> {
  return responseJson('Get pantry members', await api.get(`/api/pantries/${pantryId}/members`))
}

export async function addPantryMember(
  api: APIRequestContext,
  pantryId: string,
  request: AddPantryMemberRequest,
): Promise<void> {
  await responseBody(
    'Add pantry member',
    await api.post(`/api/e2e/pantries/${pantryId}/members`, { data: request }),
    204,
  )
}
