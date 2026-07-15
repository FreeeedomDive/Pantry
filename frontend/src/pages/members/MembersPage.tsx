import {
  Anchor,
  Alert,
  Badge,
  Button,
  Card,
  Container,
  CopyButton,
  Group,
  Modal,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { initData, openTelegramLink, useSignal } from '@telegram-apps/sdk-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { describeApiError } from '../../api/http.ts'
import {
  useCreateInvite,
  useKickPantryMember,
  usePantry,
  usePantryMembers,
} from '../../api/pantries.ts'
import type { InviteResponse, PantryMemberResponse } from '../../api/types.ts'
import { formatDate } from '../../ui/format.ts'
import { ErrorState, LoadingState } from '../../ui/states.tsx'

export function MembersPage() {
  const { pantryId } = useParams<{ pantryId: string }>()
  const pantry = usePantry(pantryId!)
  const members = usePantryMembers(pantryId!)
  const me = useSignal(initData.user)
  const [memberToKick, setMemberToKick] = useState<PantryMemberResponse | null>(null)
  const isOwner =
    members.data?.some((member) => member.telegramUserId === me?.id && member.role === 'OWNER') ??
    false

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to="/" size="sm">
          ← Инвентари
        </Anchor>
        <Title order={2} style={{ overflowWrap: 'anywhere' }}>
          {pantry.data ? `${pantry.data.name} — участники` : 'Участники'}
        </Title>
        {members.isPending && <LoadingState />}
        {members.isError && <ErrorState error={members.error} onRetry={() => members.refetch()} />}
        {members.isSuccess &&
          members.data.map((member) => {
            const canKick = isOwner && member.telegramUserId !== me?.id
            return (
              <MemberCard
                key={member.userId}
                member={member}
                isCurrentUser={member.telegramUserId === me?.id}
                canKick={canKick}
                onKick={setMemberToKick}
              />
            )
          })}
        {isOwner && <InviteSection pantryId={pantryId!} />}
      </Stack>
      {memberToKick && (
        <ConfirmKickMemberModal
          pantryId={pantryId!}
          member={memberToKick}
          onClose={() => setMemberToKick(null)}
        />
      )}
    </Container>
  )
}

function MemberCard({
  member,
  isCurrentUser,
  canKick,
  onKick,
}: {
  member: PantryMemberResponse
  isCurrentUser: boolean
  canKick: boolean
  onKick: (member: PantryMemberResponse) => void
}) {
  return (
    <Card withBorder padding="md">
      <Stack gap="sm">
        <Group justify="space-between" wrap="nowrap">
          <Stack gap={2} style={{ minWidth: 0 }}>
            <Text fw={500}>
              Telegram ID {member.telegramUserId}
              {isCurrentUser && ' (вы)'}
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
        {canKick && (
          <Button variant="light" color="red" size="xs" onClick={() => onKick(member)}>
            Исключить
          </Button>
        )}
      </Stack>
    </Card>
  )
}

function ConfirmKickMemberModal({
  pantryId,
  member,
  onClose,
}: {
  pantryId: string
  member: PantryMemberResponse
  onClose: () => void
}) {
  const kickMember = useKickPantryMember(pantryId)

  const confirm = () =>
    kickMember.mutate(member.userId, {
      onSuccess: onClose,
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })

  return (
    <Modal
      opened
      onClose={onClose}
      title={`Исключить пользователя ${member.telegramUserId}?`}
      centered
    >
      <Stack gap="md">
        <Alert color="red">
          Пользователь потеряет доступ к товарам и остаткам этого инвентаря. Пригласить его снова
          можно будет по новой ссылке.
        </Alert>
        <Button color="red" onClick={confirm} loading={kickMember.isPending} fullWidth>
          Исключить
        </Button>
      </Stack>
    </Modal>
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
