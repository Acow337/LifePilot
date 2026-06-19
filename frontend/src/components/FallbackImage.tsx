import { type ImgHTMLAttributes, useEffect, useState } from 'react'

const FALLBACK_IMAGE =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%23eef6f6'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='%23759898' font-size='26' font-family='Arial'>图片暂不可用</text></svg>"

type Props = ImgHTMLAttributes<HTMLImageElement>

export function FallbackImage({ src, alt, ...rest }: Props) {
  const [currentSrc, setCurrentSrc] = useState(src || FALLBACK_IMAGE)

  useEffect(() => {
    setCurrentSrc(src || FALLBACK_IMAGE)
  }, [src])

  return <img {...rest} src={currentSrc} alt={alt} onError={() => setCurrentSrc(FALLBACK_IMAGE)} />
}
