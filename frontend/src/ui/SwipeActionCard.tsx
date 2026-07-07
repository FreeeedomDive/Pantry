import { Box, Card, Text } from '@mantine/core'
import { hapticFeedback } from '@telegram-apps/sdk-react'
import { useRef, useState } from 'react'
import type { PointerEvent, ReactNode } from 'react'

const MAX_PULL = 120
const TRIGGER_OFFSET = 72
const DIRECTION_LOCK = 8

interface SwipeGesture {
  pointerId: number
  startX: number
  startY: number
  captured: boolean
}

interface SwipeActionCardProps {
  actionLabel: string
  disabled?: boolean
  onAction: () => void
  children: ReactNode
}

export function SwipeActionCard({
  actionLabel,
  disabled,
  onAction,
  children,
}: SwipeActionCardProps) {
  const [offset, setOffset] = useState(0)
  const [dragging, setDragging] = useState(false)
  const offsetRef = useRef(0)
  const gesture = useRef<SwipeGesture | null>(null)

  const pull = (value: number) => {
    offsetRef.current = value
    setOffset(value)
  }

  const settle = (triggered: boolean) => {
    gesture.current = null
    setDragging(false)
    pull(0)
    if (!triggered) return
    if (hapticFeedback.impactOccurred.isAvailable()) hapticFeedback.impactOccurred('light')
    onAction()
  }

  const onPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (disabled) return
    gesture.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      captured: false,
    }
  }

  const onPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const current = gesture.current
    if (!current || event.pointerId !== current.pointerId) return
    const dx = event.clientX - current.startX
    const dy = event.clientY - current.startY
    if (!current.captured) {
      if (Math.abs(dx) < DIRECTION_LOCK && Math.abs(dy) < DIRECTION_LOCK) return
      if (Math.abs(dy) > Math.abs(dx)) {
        gesture.current = null
        return
      }
      current.captured = true
      setDragging(true)
      try {
        event.currentTarget.setPointerCapture(event.pointerId)
      } catch {
        // указатель уже отпущен — жест завершится по pointerup/pointercancel
      }
    }
    pull(Math.max(Math.min(dx, 0), -MAX_PULL))
  }

  const onPointerUp = (event: PointerEvent<HTMLDivElement>) => {
    const current = gesture.current
    if (!current || event.pointerId !== current.pointerId) return
    settle(current.captured && offsetRef.current <= -TRIGGER_OFFSET)
  }

  const onPointerCancel = () => settle(false)

  return (
    <Box style={{ position: 'relative', touchAction: 'pan-y' }}>
      <Box
        style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'flex-end',
          paddingRight: 'var(--mantine-spacing-md)',
          borderRadius: 'var(--mantine-radius-default)',
          background: 'var(--mantine-color-red-6)',
          opacity: offset < 0 ? 1 : 0,
          transition: 'opacity 150ms ease',
        }}
      >
        <Text c="white" fw={600} size="sm">
          {actionLabel}
        </Text>
      </Box>
      <Card
        withBorder
        padding="md"
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerCancel}
        style={{
          transform: `translateX(${offset}px)`,
          transition: dragging ? 'none' : 'transform 150ms ease',
        }}
      >
        {children}
      </Card>
    </Box>
  )
}
