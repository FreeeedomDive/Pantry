import { hapticFeedback } from '@telegram-apps/sdk-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'

const EDGE_ZONE = 24
const DIRECTION_LOCK = 8
const TRIGGER_DISTANCE = 80
const MAX_VISUAL_PULL = 64

interface EdgeGesture {
  pointerId: number
  startX: number
  startY: number
  captured: boolean
}

interface EdgeBackGestureProps {
  enabled: boolean
  onBack: () => void
  children: ReactNode
}

export function EdgeBackGesture({ enabled, onBack, children }: EdgeBackGestureProps) {
  const [dx, setDx] = useState(0)
  const [dragging, setDragging] = useState(false)
  const dxRef = useRef(0)
  const gesture = useRef<EdgeGesture | null>(null)
  const onBackRef = useRef(onBack)

  useEffect(() => {
    onBackRef.current = onBack
  }, [onBack])

  const pull = useCallback((value: number) => {
    dxRef.current = value
    setDx(value)
  }, [])

  const settle = useCallback(
    (triggered: boolean) => {
      gesture.current = null
      setDragging(false)
      pull(0)
      if (!triggered) return
      if (hapticFeedback.impactOccurred.isAvailable()) hapticFeedback.impactOccurred('light')
      onBackRef.current()
    },
    [pull],
  )

  useEffect(() => {
    if (!enabled) return

    const onPointerMove = (event: PointerEvent) => {
      const current = gesture.current
      if (!current || event.pointerId !== current.pointerId) return
      const moveDx = event.clientX - current.startX
      const moveDy = event.clientY - current.startY
      if (!current.captured) {
        if (Math.abs(moveDx) < DIRECTION_LOCK && Math.abs(moveDy) < DIRECTION_LOCK) return
        if (Math.abs(moveDy) > Math.abs(moveDx) || moveDx <= 0) {
          window.removeEventListener('pointermove', onPointerMove)
          window.removeEventListener('pointerup', onPointerUp)
          window.removeEventListener('pointercancel', onPointerUp)
          gesture.current = null
          return
        }
        current.captured = true
        setDragging(true)
        try {
          ;(event.target as Element).setPointerCapture(event.pointerId)
        } catch {
          // указатель уже отпущен — жест завершится по pointerup/pointercancel
        }
      }
      event.preventDefault()
      pull(Math.max(moveDx, 0))
    }

    const onPointerUp = (event: PointerEvent) => {
      const current = gesture.current
      if (!current || event.pointerId !== current.pointerId) return
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('pointercancel', onPointerUp)
      settle(current.captured && dxRef.current >= TRIGGER_DISTANCE)
    }

    const onPointerDown = (event: PointerEvent) => {
      if (gesture.current) return
      if (event.clientX > EDGE_ZONE) return
      if (document.body.hasAttribute('data-scroll-locked')) return
      gesture.current = {
        pointerId: event.pointerId,
        startX: event.clientX,
        startY: event.clientY,
        captured: false,
      }
      window.addEventListener('pointermove', onPointerMove, { passive: false })
      window.addEventListener('pointerup', onPointerUp)
      window.addEventListener('pointercancel', onPointerUp)
    }

    window.addEventListener('pointerdown', onPointerDown)
    return () => {
      window.removeEventListener('pointerdown', onPointerDown)
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('pointercancel', onPointerUp)
    }
  }, [enabled, pull, settle])

  const pullPx = Math.min(dx, MAX_VISUAL_PULL)
  const progress = Math.min(dx / TRIGGER_DISTANCE, 1)

  return (
    <div style={{ position: 'relative', overflow: 'hidden', minHeight: '100dvh' }}>
      <div
        style={{
          transform: `translateX(${pullPx}px)`,
          transition: dragging ? 'none' : 'transform 200ms ease',
        }}
      >
        {children}
      </div>
      <div
        style={{
          position: 'fixed',
          left: 8,
          top: '50%',
          width: 28,
          height: 44,
          borderRadius: 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          pointerEvents: 'none',
          background: progress >= 1 ? 'var(--mantine-color-teal-6)' : 'var(--mantine-color-gray-6)',
          opacity: progress,
          transform: `translateY(-50%) scale(${0.7 + 0.3 * progress})`,
          transition: dragging
            ? 'none'
            : 'opacity 200ms ease, transform 200ms ease, background 150ms ease',
        }}
      >
        <span style={{ color: 'white', fontSize: 20, fontWeight: 700, lineHeight: 1 }}>‹</span>
      </div>
    </div>
  )
}
