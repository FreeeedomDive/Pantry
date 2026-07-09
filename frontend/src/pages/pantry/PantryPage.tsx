import { Anchor, Container, Stack, Tabs, Title } from '@mantine/core'
import { Link, useParams } from 'react-router'
import { usePantry } from '../../api/pantries.ts'
import { usePantryBalance } from '../../api/products.ts'
import { ErrorState, LoadingState } from '../../ui/states.tsx'
import { ProductList } from './ProductList.tsx'

export function PantryPage() {
  const { pantryId } = useParams<{ pantryId: string }>()
  const pantry = usePantry(pantryId!)
  const balance = usePantryBalance(pantryId!)

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to="/" size="sm">
          ← Инвентари
        </Anchor>
        <Title order={2} style={{ overflowWrap: 'anywhere' }}>
          {pantry.data?.name ?? 'Инвентарь'}
        </Title>
        {balance.isPending && <LoadingState />}
        {balance.isError && <ErrorState error={balance.error} onRetry={() => balance.refetch()} />}
        {balance.isSuccess && (
          <Tabs defaultValue="all" keepMounted={false}>
            <Tabs.List>
              <Tabs.Tab value="all">Все товары</Tabs.Tab>
              <Tabs.Tab value="shopping">Список покупок</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="all" pt="md">
              <ProductList
                pantryId={pantryId!}
                items={balance.data}
                emptyMessage="Товаров пока нет. Отправьте боту фото чека и подтвердите черновик поступления — товары появятся здесь."
              />
            </Tabs.Panel>
            <Tabs.Panel value="shopping" pt="md">
              <ProductList
                pantryId={pantryId!}
                items={balance.data.filter(({ product, total }) => product.isStaple && total === 0)}
                emptyMessage="Нет постоянных товаров, которые нужно купить."
              />
            </Tabs.Panel>
          </Tabs>
        )}
      </Stack>
    </Container>
  )
}
