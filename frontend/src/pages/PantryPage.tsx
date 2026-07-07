import { Anchor, Badge, Card, Container, Group, Stack, Text, Title } from '@mantine/core'
import { Link, useParams } from 'react-router'
import { usePantry } from '../api/pantries'
import { usePantryBalance } from '../api/products'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'

export function PantryPage() {
  const { pantryId } = useParams<{ pantryId: string }>()
  const pantry = usePantry(pantryId!)
  const balance = usePantryBalance(pantryId!)

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to="/" size="sm">
          ← Инвентари
        </Anchor>
        <Title order={2} style={{ overflowWrap: 'anywhere' }}>
          {pantry.data?.name ?? 'Инвентарь'}
        </Title>
        {balance.isPending && <LoadingState />}
        {balance.isError && <ErrorState error={balance.error} onRetry={() => balance.refetch()} />}
        {balance.isSuccess && balance.data.length === 0 && (
          <EmptyState message="Товаров пока нет. Отправьте боту фото чека и подтвердите черновик поступления — товары появятся здесь." />
        )}
        {balance.isSuccess &&
          [...balance.data]
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
    </Container>
  )
}
