import {
  Anchor,
  Badge,
  Button,
  Card,
  Container,
  CopyButton,
  Group,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { initData, openTelegramLink, useSignal } from '@telegram-apps/sdk-react'
import { Link, useParams } from 'react-router'
import { describeApiError } from '../api/http'
import { useCreateInvite, usePantryMembers } from '../api/pantries'
import type { InviteResponse } from '../api/types'
import { formatDate } from '../ui/format'
import { ErrorState, LoadingState } from '../ui/states'

export function MembersPage() {
  const { pantryId } = useParams<{ pantryId: string }>()
  const members = usePantryMembers(pantryId!)
  const me = useSignal(initData.user)
  const isOwner =
    members.data?.some((member) => member.telegramUserId === me?.id && member.role === 'OWNER') ??
    false

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to={`/pantries/${pantryId}`} size="sm">
          ← К инвентарю
        </Anchor>
        <Title order={2}>Участники</Title>
        {members.isPending && <LoadingState />}
        {members.isError && <ErrorState error={members.error} onRetry={() => members.refetch()} />}
        {members.isSuccess &&
          members.data.map((member) => (
            <Card key={member.telegramUserId} withBorder padding="md">
              <Group justify="space-between" wrap="nowrap">
                <Stack gap={2} style={{ minWidth: 0 }}>
                  <Text fw={500}>
                    Telegram ID {member.telegramUserId}
                    {member.telegramUserId === me?.id && ' (вы)'}
                  </Text>
                  <Text size="sm" c="dimmed">
                    В инвентаре с {formatDate(member.joinedAt)}
                  </Text>
                </Stack>
                <Badge
                  variant="light"
                  color={member.role === 'OWNER' ? 'teal' : 'gray'}
                  style={{ flexShrink: 0 }}
                >
                  {member.role === 'OWNER' ? 'Владелец' : 'Участник'}
                </Badge>
              </Group>
            </Card>
          ))}
        {isOwner && <InviteSection pantryId={pantryId!} />}
      </Stack>
    </Container>
  )
}

function InviteSection({ pantryId }: { pantryId: string }) {
  const createInvite = useCreateInvite(pantryId)

  const create = () =>
    createInvite.mutate(undefined, {
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  return (
    <Stack gap="sm">
      <Button variant="light" onClick={create} loading={createInvite.isPending}>
        Пригласить по ссылке
      </Button>
      {createInvite.data && <InviteCard invite={createInvite.data} />}
    </Stack>
  )
}

function InviteCard({ invite }: { invite: InviteResponse }) {
  const share = () => {
    const url = `https://t.me/share/url?url=${encodeURIComponent(invite.link)}&text=${encodeURIComponent('Присоединяйся к моему инвентарю в Pantry')}`
    if (openTelegramLink.isAvailable()) openTelegramLink(url)
    else window.open(url, '_blank')
  }

  return (
    <Card withBorder padding="md">
      <Stack gap="sm">
        <Text size="sm" style={{ overflowWrap: 'anywhere' }}>
          {invite.link}
        </Text>
        <Text size="xs" c="dimmed">
          Ссылка действует до {formatDate(invite.expiresAt)} — получатель нажмёт Start у бота и
          попадёт в инвентарь.
        </Text>
        <Group grow>
          <CopyButton value={invite.link}>
            {({ copied, copy }) => (
              <Button variant="light" color={copied ? 'teal' : undefined} onClick={copy}>
                {copied ? 'Скопировано' : 'Скопировать'}
              </Button>
            )}
          </CopyButton>
          <Button onClick={share}>Поделиться</Button>
        </Group>
      </Stack>
    </Card>
  )
}
