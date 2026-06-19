export const splitImages = (images?: string) => {
  if (!images) {
    return []
  }
  return images
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => {
      if (item.startsWith('http') || item.startsWith('/')) {
        return item
      }
      return `/${item}`
    })
}

export const pickFirstImage = (images?: string) => splitImages(images)[0]

export const formatPrice = (price?: number) => {
  if (price === undefined || price === null) {
    return '—'
  }
  return `¥${price}`
}

export const formatCentPrice = (price?: number) => {
  if (price === undefined || price === null) {
    return '—'
  }
  const yuan = price / 100
  return `¥${Number.isInteger(yuan) ? yuan.toFixed(0) : yuan.toFixed(2)}`
}

export const formatScore = (score?: number) => {
  if (score === undefined || score === null) {
    return '—'
  }
  return (score / 10).toFixed(1)
}

export const brief = (text?: string, max = 64) => {
  if (!text) {
    return ''
  }
  if (text.length <= max) {
    return text
  }
  return `${text.slice(0, max)}...`
}

export const formatDateTime = (value?: string) => {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
