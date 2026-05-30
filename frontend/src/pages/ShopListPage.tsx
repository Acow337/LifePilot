import { useQuery } from '@tanstack/react-query'
import { type FormEvent, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getShopByType, getShopTypes, searchShopByName } from '../services/modules/shop'
import { formatPrice, formatScore, pickFirstImage } from '../utils/format'

export function ShopListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const initialTypeId = Number(searchParams.get('typeId')) || 1
  const initialName = searchParams.get('name') || ''

  const [typeId, setTypeId] = useState(initialTypeId)
  const [name, setName] = useState(initialName)
  const [current, setCurrent] = useState(1)

  const { data: types = [] } = useQuery({
    queryKey: ['shop-types'],
    queryFn: getShopTypes,
  })

  const queryKey = useMemo(() => ['shops', { typeId, name, current }], [typeId, name, current])

  const { data: shops = [], isLoading } = useQuery({
    queryKey,
    queryFn: () => {
      if (name.trim()) {
        return searchShopByName(name.trim(), current)
      }
      return getShopByType(typeId, current)
    },
  })

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCurrent(1)
    const next = new URLSearchParams()
    next.set('typeId', String(typeId))
    if (name.trim()) {
      next.set('name', name.trim())
    }
    setSearchParams(next)
  }

  return (
    <div className="page fade-in">
      <section className="section">
        <div className="section-head">
          <h2>店铺列表</h2>
        </div>

        <form className="toolbar" onSubmit={submitSearch}>
          <select
            value={typeId}
            onChange={(event) => {
              setTypeId(Number(event.target.value))
              setCurrent(1)
            }}
          >
            {types.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
          </select>
          <input
            type="text"
            placeholder="搜索店铺名称"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <button type="submit" className="primary-btn">
            查询
          </button>
        </form>

        {isLoading ? <p className="muted">加载中...</p> : null}

        <div className="grid cards-2">
          {shops.map((shop) => {
            const cover = pickFirstImage(shop.images)
            return (
              <Link key={shop.id} to={`/shop/${shop.id}`} className="card shop-card">
                {cover ? <img src={cover} alt={shop.name} className="card-cover" /> : null}
                <div className="card-body">
                  <h3>{shop.name}</h3>
                  <p className="muted">{shop.area || shop.address || '暂无地址信息'}</p>
                  <div className="meta">
                    <span>评分 {formatScore(shop.score)}</span>
                    <span>人均 {formatPrice(shop.avgPrice)}</span>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>

        <div className="pager">
          <button className="ghost-btn" disabled={current <= 1} onClick={() => setCurrent((value) => value - 1)}>
            上一页
          </button>
          <span>第 {current} 页</span>
          <button className="ghost-btn" disabled={shops.length === 0} onClick={() => setCurrent((value) => value + 1)}>
            下一页
          </button>
        </div>
      </section>
    </div>
  )
}
