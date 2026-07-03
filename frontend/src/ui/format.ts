const dateFormat = new Intl.DateTimeFormat('ru-RU', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

export function formatDate(isoDateOrInstant: string): string {
  return dateFormat.format(new Date(isoDateOrInstant))
}
