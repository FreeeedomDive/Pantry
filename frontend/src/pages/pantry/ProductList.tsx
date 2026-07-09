import { Badge, Card, Group, Stack, Text } from '@mantine/core'
import type { ProductBalanceResponse } from '../../api/types.ts'
import { EmptyState } from '../../ui/states.tsx'
import { Link } from 'react-router'

export function ProductList({
  pantryId,
  items,
  emptyMessage,
}: {
  pantryId: string
  items: ProductBalanceResponse[]
  emptyMessage: string
}) {
  if (items.length === 0) return <EmptyState message={emptyMessage} />

  return (
    <Stack gap="md">
      {[...items]
        .sort((a, b) => Number(b.total > 0) - Number(a.total > 0))
        .map(({ product, total }) => (
          <Card
            key={product.id}
            component={Link}
            to={`/pantries/${pantryId}/products/${product.id}`}
            withBorder
            padding="md"
          >
            <Group justify="space-between" wrap="nowrap">
              <Stack gap={2} style={{ minWidth: 0 }}>
                <Text fw={500} style={{ overflowWrap: 'anywhere' }}>
                  {product.name}
                </Text>
                {product.brand && (
                  <Text size="sm" c="dimmed">
                    {product.brand}
                  </Text>
                )}
              </Stack>
              <Badge
                size="lg"
                variant="light"
                color={total > 0 ? 'teal' : 'gray'}
                style={{ flexShrink: 0 }}
              >
                {total} шт
              </Badge>
            </Group>
          </Card>
        ))}
    </Stack>
  )
}
