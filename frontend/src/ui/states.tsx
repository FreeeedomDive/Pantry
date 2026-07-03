import { Alert, Button, Center, Loader, Stack, Text } from '@mantine/core'
import { describeApiError } from '../api/http'

export function LoadingState() {
  return (
    <Center py="xl">
      <Loader />
    </Center>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <Alert color="red" title="Ошибка">
      <Stack gap="sm">
        <Text size="sm">{describeApiError(error)}</Text>
        {onRetry && (
          <Button variant="light" color="red" size="xs" onClick={onRetry}>
            Повторить
          </Button>
        )}
      </Stack>
    </Alert>
  )
}

export function EmptyState({ message }: { message: string }) {
  return (
    <Center py="xl">
      <Text c="dimmed" ta="center">
        {message}
      </Text>
    </Center>
  )
}
