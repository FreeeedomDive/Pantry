import {
  ActionIcon,
  Anchor,
  Badge,
  Button,
  Card,
  Container,
  Group,
  Menu,
  Modal,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { describeApiError } from '../api/http'
import { usePantry, useRenamePantry } from '../api/pantries'
import { usePantryBalance } from '../api/products'
import type { PantryResponse } from '../api/types'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'

export function PantryPage() {
  const { pantryId } = useParams<{ pantryId: string }>()
  const navigate = useNavigate()
  const pantry = usePantry(pantryId!)
  const balance = usePantryBalance(pantryId!)
  const [renaming, setRenaming] = useState(false)

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to="/" size="sm">
          ← Инвентари
        </Anchor>
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Title order={2} style={{ overflowWrap: 'anywhere', minWidth: 0 }}>
            {pantry.data?.name ?? 'Инвентарь'}
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
              {pantry.data?.role === 'OWNER' && (
                <Menu.Item onClick={() => setRenaming(true)}>Переименовать</Menu.Item>
              )}
              <Menu.Item onClick={() => navigate(`/pantries/${pantryId}/members`)}>
                Участники
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
        {balance.isPending && <LoadingState />}
        {balance.isError && <ErrorState error={balance.error} onRetry={() => balance.refetch()} />}
        {balance.isSuccess && balance.data.length === 0 && (
          <EmptyState message="Товаров пока нет. Отправьте боту фото чека и подтвердите черновик поступления — товары появятся здесь." />
        )}
        {balance.isSuccess &&
          balance.data.map(({ product, total }) => (
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
      {renaming && pantry.data && (
        <RenamePantryModal pantry={pantry.data} onClose={() => setRenaming(false)} />
      )}
    </Container>
  )
}

function RenamePantryModal({ pantry, onClose }: { pantry: PantryResponse; onClose: () => void }) {
  const renamePantry = useRenamePantry(pantry.id)
  const [name, setName] = useState(pantry.name)

  const rename = () =>
    renamePantry.mutate(name.trim(), {
      onSuccess: onClose,
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  return (
    <Modal opened onClose={onClose} title="Переименовать инвентарь" centered>
      <Stack gap="md">
        <TextInput
          label="Название"
          value={name}
          onChange={(event) => setName(event.currentTarget.value)}
          data-autofocus
        />
        <Button
          onClick={rename}
          disabled={!name.trim() || name.trim() === pantry.name}
          loading={renamePantry.isPending}
          fullWidth
        >
          Сохранить
        </Button>
      </Stack>
    </Modal>
  )
}
