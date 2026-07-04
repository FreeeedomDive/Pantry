import {
  ActionIcon,
  Alert,
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
import { Link, useNavigate } from 'react-router'
import {
  useCreatePantry,
  useLeaveOrDeletePantry,
  usePantries,
  useRenamePantry,
  useSetDefaultPantry,
} from '../api/pantries'
import { ApiError, describeApiError } from '../api/http'
import type { PantryResponse } from '../api/types'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'

export function PantriesPage() {
  const pantries = usePantries()
  const [creating, setCreating] = useState(false)

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Group justify="space-between" wrap="nowrap">
          <Title order={2}>Мои инвентари</Title>
          <Button
            variant="light"
            size="xs"
            style={{ flexShrink: 0 }}
            onClick={() => setCreating(true)}
          >
            Создать
          </Button>
        </Group>
        {pantries.isPending && <LoadingState />}
        {pantries.isError && (
          <ErrorState error={pantries.error} onRetry={() => pantries.refetch()} />
        )}
        {pantries.isSuccess && pantries.data.length === 0 && (
          <EmptyState message="У вас пока нет инвентарей. Создайте первый или отправьте боту фото чека." />
        )}
        {pantries.isSuccess &&
          pantries.data.map((pantry) => <PantryCard key={pantry.id} pantry={pantry} />)}
      </Stack>
      {creating && <CreatePantryModal onClose={() => setCreating(false)} />}
    </Container>
  )
}

function PantryCard({ pantry }: { pantry: PantryResponse }) {
  const navigate = useNavigate()
  const setDefaultPantry = useSetDefaultPantry()
  const [renaming, setRenaming] = useState(false)
  const [confirming, setConfirming] = useState(false)

  const setDefault = () =>
    setDefaultPantry.mutate(pantry.id, {
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  const stop = (event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
  }

  const stopAnd = (action: () => void) => (event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    action()
  }

  return (
    <>
      <Card component={Link} to={`/pantries/${pantry.id}`} withBorder padding="md">
        <Stack gap="xs">
          <Group justify="space-between" wrap="nowrap" align="flex-start">
            <Text fw={500} style={{ overflowWrap: 'anywhere' }}>
              {pantry.name}
            </Text>
            <Menu position="bottom-end">
              <Menu.Target>
                <ActionIcon
                  variant="subtle"
                  size="lg"
                  style={{ flexShrink: 0 }}
                  aria-label="Действия"
                  onClick={stop}
                >
                  ⋯
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                {pantry.role === 'OWNER' && (
                  <Menu.Item onClick={stopAnd(() => setRenaming(true))}>Переименовать</Menu.Item>
                )}
                {!pantry.isDefault && (
                  <Menu.Item onClick={stopAnd(setDefault)} disabled={setDefaultPantry.isPending}>
                    Сделать инвентарём по умолчанию
                  </Menu.Item>
                )}
                <Menu.Item onClick={stopAnd(() => navigate(`/pantries/${pantry.id}/members`))}>
                  Владельцы
                </Menu.Item>
                <Menu.Item color="red" onClick={stopAnd(() => setConfirming(true))}>
                  {pantry.role === 'OWNER' ? 'Удалить' : 'Покинуть'}
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
          <Group gap="xs">
            <Badge variant="light" color={pantry.role === 'OWNER' ? 'teal' : 'gray'}>
              {pantry.role === 'OWNER' ? 'Владелец' : 'Совладелец'}
            </Badge>
            {pantry.isDefault && (
              <Badge variant="light" color="blue">
                По умолчанию
              </Badge>
            )}
          </Group>
        </Stack>
      </Card>
      {renaming && <RenamePantryModal pantry={pantry} onClose={() => setRenaming(false)} />}
      {confirming && (
        <ConfirmLeaveOrDeleteModal pantry={pantry} onClose={() => setConfirming(false)} />
      )}
    </>
  )
}

function CreatePantryModal({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate()
  const createPantry = useCreatePantry()
  const [name, setName] = useState('')

  const create = () =>
    createPantry.mutate(name.trim(), {
      onSuccess: (pantry) => navigate(`/pantries/${pantry.id}`),
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  return (
    <Modal opened onClose={onClose} title="Новый инвентарь" centered>
      <Stack gap="md">
        <TextInput
          label="Название"
          placeholder="Например, Дача"
          value={name}
          onChange={(event) => setName(event.currentTarget.value)}
          data-autofocus
        />
        <Button onClick={create} disabled={!name.trim()} loading={createPantry.isPending} fullWidth>
          Создать
        </Button>
      </Stack>
    </Modal>
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

function ConfirmLeaveOrDeleteModal({
  pantry,
  onClose,
}: {
  pantry: PantryResponse
  onClose: () => void
}) {
  const leaveOrDeletePantry = useLeaveOrDeletePantry()
  const isOwner = pantry.role === 'OWNER'

  const confirm = () =>
    leaveOrDeletePantry.mutate(pantry.id, {
      onSuccess: onClose,
      onError: (error) => {
        const message =
          error instanceof ApiError && error.status === 409
            ? 'Нельзя удалить последний инвентарь, владельцем которого вы являетесь — сначала создайте другой.'
            : describeApiError(error)
        notifications.show({ color: 'red', title: 'Не получилось', message })
      },
    })

  return (
    <Modal
      opened
      onClose={onClose}
      title={isOwner ? `Удалить «${pantry.name}»?` : `Покинуть «${pantry.name}»?`}
      centered
    >
      <Stack gap="md">
        <Alert color={isOwner ? 'red' : 'orange'}>
          {isOwner
            ? 'Это навсегда удалит все товары, партии, историю чеков и приглашения этого инвентаря. Другие владельцы потеряют к нему доступ. Действие необратимо.'
            : 'Вы потеряете доступ к товарам и остаткам этого инвентаря. Владелец сможет пригласить вас снова.'}
        </Alert>
        <Button
          color={isOwner ? 'red' : 'orange'}
          onClick={confirm}
          loading={leaveOrDeletePantry.isPending}
          fullWidth
        >
          {isOwner ? 'Удалить' : 'Покинуть'}
        </Button>
      </Stack>
    </Modal>
  )
}
