import { Anchor, Badge, Card, Container, Group, Stack, Text, Title } from '@mantine/core'
import { Link, useParams } from 'react-router'
import { useProductBalance, useProductStock } from '../api/products'
import { formatDate } from '../ui/format'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'

export function ProductPage() {
  const { pantryId, productId } = useParams<{ pantryId: string; productId: string }>()
  const balance = useProductBalance(pantryId!, productId!)
  const stock = useProductStock(pantryId!, productId!)

  const product = balance.data?.product

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to={`/pantries/${pantryId}`} size="sm">
          ← Товары
        </Anchor>
        <Stack gap={4}>
          <Title order={2} style={{ overflowWrap: 'anywhere' }}>
            {product?.name ?? 'Товар'}
          </Title>
          <Group wrap="nowrap">
            {product?.brand && <Text c="dimmed">{product.brand}</Text>}
            {balance.data && (
              <Badge
                size="xl"
                variant="light"
                color={balance.data.total > 0 ? 'teal' : 'gray'}
                ml="auto"
                style={{ flexShrink: 0 }}
              >
                {balance.data.total} шт
              </Badge>
            )}
          </Group>
        </Stack>
        {stock.isPending && <LoadingState />}
        {stock.isError && <ErrorState error={stock.error} onRetry={() => stock.refetch()} />}
        {stock.isSuccess && stock.data.length === 0 && (
          <EmptyState message="Партий нет — весь запас израсходован." />
        )}
        {stock.isSuccess &&
          stock.data.map((item) => (
            <Card key={item.id} withBorder padding="md">
              <Group justify="space-between" wrap="nowrap">
                <Stack gap={2}>
                  <Text size="sm">Куплено {formatDate(item.purchasedAt)}</Text>
                  <Text size="sm" c={item.expiresAt ? 'orange' : 'dimmed'}>
                    {item.expiresAt
                      ? `Годен до ${formatDate(item.expiresAt)}`
                      : 'Срок годности не указан'}
                  </Text>
                </Stack>
                <Badge size="lg" variant="light">
                  {item.quantity} шт
                </Badge>
              </Group>
            </Card>
          ))}
      </Stack>
    </Container>
  )
}
