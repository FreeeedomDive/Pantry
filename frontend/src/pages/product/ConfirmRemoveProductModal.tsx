import { useRemoveProduct } from '../../api/products.ts'
import { describeApiError } from '../../api/http.ts'
import { useNavigate } from 'react-router'
import { notifications } from '@mantine/notifications'
import { Alert, Button, Modal, Stack } from '@mantine/core'

export function ConfirmRemoveProductModal({
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
