import {
  ActionIcon,
  Anchor,
  Badge,
  Container,
  Group,
  Menu,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { describeApiError } from '../../api/http.ts'
import {
  useProductBalance,
  useProductStock,
  useStapleProduct,
  useWriteOffStockItem,
} from '../../api/products.ts'
import { formatDate } from '../../ui/format.ts'
import { EmptyState, ErrorState, LoadingState } from '../../ui/states.tsx'
import { SwipeActionCard } from '../../ui/SwipeActionCard.tsx'
import { ConfirmRemoveProductModal } from './ConfirmRemoveProductModal.tsx'
import { RenameProductModal } from './RenameProductModal.tsx'

export function ProductPage() {
  const { pantryId, productId } = useParams<{ pantryId: string; productId: string }>()
  const balance = useProductBalance(pantryId!, productId!)
  const stock = useProductStock(pantryId!, productId!)
  const writeOff = useWriteOffStockItem(pantryId!, productId!)
  const stapleProduct = useStapleProduct(pantryId!, productId!)
  const [renaming, setRenaming] = useState(false)
  const [removing, setRemoving] = useState(false)

  const product = balance.data?.product

  const writeOffOne = (stockItemId: string) =>
    writeOff.mutate(
      { stockItemId, quantity: 1 },
      {
        onError: (error) =>
          notifications.show({
            color: 'red',
            title: 'Не получилось',
            message: describeApiError(error),
          }),
      },
    )

  const toggleStaple = () => {
    if (!product) return
    stapleProduct.mutate(!product.isStaple, {
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })
  }

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to={`/pantries/${pantryId}`} size="sm">
          ← Товары
        </Anchor>
        <Stack gap={4}>
          <Group justify="space-between" wrap="nowrap" align="flex-start">
            <Title order={2} style={{ overflowWrap: 'anywhere' }}>
              {product?.name ?? 'Товар'}
            </Title>
            <Menu position="bottom-end">
              <Menu.Target>
                <ActionIcon
                  variant="subtle"
                  size="lg"
                  style={{ flexShrink: 0 }}
                  aria-label="Действия"
                >
                  ⋯
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Item onClick={() => setRenaming(true)} disabled={!product}>
                  Переименовать
                </Menu.Item>
                <Menu.Item onClick={toggleStaple} disabled={!product || stapleProduct.isPending}>
                  {product?.isStaple ? 'Убрать из постоянных' : 'Сделать постоянным'}
                </Menu.Item>
                <Menu.Item color="red" onClick={() => setRemoving(true)}>
                  Удалить товар
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
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
        {stock.isSuccess && stock.data.length > 0 && (
          <Text size="xs" c="dimmed">
            Свайпните партию влево, чтобы списать 1 шт.
          </Text>
        )}
        {stock.isSuccess &&
          stock.data.map((item) => (
            <SwipeActionCard
              key={item.id}
              actionLabel="Списать 1 шт"
              disabled={writeOff.isPending}
              onAction={() => writeOffOne(item.id)}
            >
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
            </SwipeActionCard>
          ))}
      </Stack>
      {renaming && product && (
        <RenameProductModal
          pantryId={pantryId!}
          product={product}
          onClose={() => setRenaming(false)}
        />
      )}
      {removing && (
        <ConfirmRemoveProductModal
          pantryId={pantryId!}
          productId={productId!}
          productName={product?.name ?? 'Товар'}
          onClose={() => setRemoving(false)}
        />
      )}
    </Container>
  )
}
