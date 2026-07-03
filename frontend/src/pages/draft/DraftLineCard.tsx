import { Badge, Card, Group, Stack, Text } from '@mantine/core'
import type { DraftLineResponse, ProductResponse } from '../../api/types'
import { formatDate } from '../../ui/format'
import { resolveLine } from './lineStatus'

export function DraftLineCard({
  line,
  products,
  onEdit,
}: {
  line: DraftLineResponse
  products: ProductResponse[]
  onEdit: () => void
}) {
  const resolution = resolveLine(line, products)

  return (
    <Card withBorder padding="md" onClick={onEdit} style={{ cursor: 'pointer' }}>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Stack gap={2} style={{ minWidth: 0 }}>
            {resolution.kind === 'match' && (
              <>
                <Text fw={500}>{resolution.product.name}</Text>
                {resolution.product.brand && (
                  <Text size="sm" c="dimmed">
                    {resolution.product.brand}
                  </Text>
                )}
              </>
            )}
            {resolution.kind === 'create' && (
              <>
                <Text fw={500}>{resolution.name}</Text>
                {resolution.brand && (
                  <Text size="sm" c="dimmed">
                    {resolution.brand}
                  </Text>
                )}
              </>
            )}
            {resolution.kind === 'skipped' && <Text fw={500}>{line.rawText}</Text>}
          </Stack>
          <Badge size="lg" variant="light" style={{ flexShrink: 0 }}>
            {line.quantity} шт
          </Badge>
        </Group>
        <Text size="xs" c="dimmed" lineClamp={1}>
          В чеке: {line.rawText}
        </Text>
        {line.expiresAt && resolution.kind !== 'skipped' && (
          <Text size="xs" c="orange">
            Годен до {formatDate(line.expiresAt)}
          </Text>
        )}
        <Group gap="xs">
          {resolution.kind === 'match' && (
            <Badge size="sm" variant="light" color="teal">
              Совпадение
            </Badge>
          )}
          {resolution.kind === 'create' && (
            <Badge size="sm" variant="light" color="blue">
              Новый товар
            </Badge>
          )}
          {resolution.kind === 'skipped' && (
            <Badge size="sm" variant="light" color="yellow">
              {resolution.reason} — будет пропущена
            </Badge>
          )}
          {line.confidence === 'LOW' && resolution.kind !== 'skipped' && (
            <Badge size="sm" variant="light" color="orange">
              Проверьте
            </Badge>
          )}
        </Group>
      </Stack>
    </Card>
  )
}
