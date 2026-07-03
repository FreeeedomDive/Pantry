import { Card, Container, Group, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'
import { usePantries } from '../api/pantries'
import { EmptyState, ErrorState, LoadingState } from '../ui/states'

export function PantriesPage() {
  const pantries = usePantries()

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Title order={2}>Мои инвентари</Title>
        {pantries.isPending && <LoadingState />}
        {pantries.isError && (
          <ErrorState error={pantries.error} onRetry={() => pantries.refetch()} />
        )}
        {pantries.isSuccess && pantries.data.length === 0 && (
          <EmptyState message="У вас пока нет инвентарей. Отправьте боту фото чека — он создаст инвентарь и черновик поступления." />
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
              <Group justify="space-between">
                <Text fw={500}>{pantry.name}</Text>
                <Text c="dimmed">→</Text>
              </Group>
            </Card>
          ))}
      </Stack>
    </Container>
  )
}
