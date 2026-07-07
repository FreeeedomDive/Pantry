import {
  ActionIcon,
  Alert,
  Anchor,
  Badge,
  Button,
  Container,
  Group,
  Menu,
  Modal,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { describeApiError } from '../api/http'
import {
  useProductBalance,
  useProductStock,
  useRemoveProduct,
  useWriteOffStockItem,
} from '../api/products'
import { formatDate } from '../ui/format'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'
import { SwipeActionCard } from '../ui/SwipeActionCard'

export function ProductPage() {
  const { pantryId, productId } = useParams<{ pantryId: string; productId: string }>()
  const balance = useProductBalance(pantryId!, productId!)
  const stock = useProductStock(pantryId!, productId!)
  const writeOff = useWriteOffStockItem(pantryId!, productId!)
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

function ConfirmRemoveProductModal({
  pantryId,
  productId,
  productName,
  onClose,
}: {
  pantryId: string
  productId: string
  productName: string
  onClose: () => void
}) {
  const navigate = useNavigate()
  const removeProduct = useRemoveProduct(pantryId)

  const confirm = () =>
    removeProduct.mutate(productId, {
      onSuccess: () => navigate(`/pantries/${pantryId}`),
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  return (
    <Modal opened onClose={onClose} title={`Удалить «${productName}»?`} centered>
      <Stack gap="md">
        <Alert color="red">
          Товар и все его партии будут удалены навсегда, а выученные сопоставления чеков по нему —
          забыты. Действие необратимо.
        </Alert>
        <Button color="red" onClick={confirm} loading={removeProduct.isPending} fullWidth>
          Удалить
        </Button>
      </Stack>
    </Modal>
  )
}
