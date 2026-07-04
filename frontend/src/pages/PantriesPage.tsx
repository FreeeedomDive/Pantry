import {
  Badge,
  Button,
  Card,
  Container,
  Group,
  Modal,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { describeApiError } from '../api/http'
import { useCreatePantry, usePantries } from '../api/pantries'
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
          pantries.data.map((pantry) => (
            <Card
              key={pantry.id}
              component={Link}
              to={`/pantries/${pantry.id}`}
              withBorder
              padding="md"
            >
              <Group justify="space-between" wrap="nowrap">
                <Text fw={500} style={{ overflowWrap: 'anywhere' }}>
                  {pantry.name}
                </Text>
                <Badge
                  variant="light"
                  color={pantry.role === 'OWNER' ? 'teal' : 'gray'}
                  style={{ flexShrink: 0 }}
                >
                  {pantry.role === 'OWNER' ? 'Владелец' : 'Участник'}
                </Badge>
              </Group>
            </Card>
          ))}
      </Stack>
      {creating && <CreatePantryModal onClose={() => setCreating(false)} />}
    </Container>
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
