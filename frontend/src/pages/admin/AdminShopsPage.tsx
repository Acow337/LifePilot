import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createAdminShop, getAdminShops, updateAdminShop, type AdminShopPayload } from '../../services/modules/admin'
import { getShopTypes } from '../../services/modules/shop'
import { formatDateTime, formatPrice } from '../../utils/format'

const PAGE_SIZE = 10

const defaultForm: AdminShopPayload = {
  name: '',
  typeId: 1,
  images: '/imgs/blogs/blog1.jpg',
  address: '',
  area: '',
  avgPrice: 0,
  x: 0,
  y: 0,
}

export function AdminShopsPage() {
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [typeId, setTypeId] = useState<number | undefined>(undefined)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<AdminShopPayload>(defaultForm)
  const queryClient = useQueryClient()

  const { data: types = [] } = useQuery({ queryKey: ['shop-types'], queryFn: getShopTypes })

  const queryKey = useMemo(() => ['admin-shops', page, keyword, typeId], [keyword, page, typeId])

  const { data, isLoading, isFetching } = useQuery({
    queryKey,
    queryFn: () => getAdminShops({ page, size: PAGE_SIZE, keyword: keyword.trim() || undefined, typeId }),
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-shops'] })

  const createMutation = useMutation({
    mutationFn: () => createAdminShop(form),
    onSuccess: () => {
      setForm(defaultForm)
      refresh()
    },
  })

  const updateMutation = useMutation({
    mutationFn: () => updateAdminShop(editingId as number, form),
    onSuccess: () => {
      setEditingId(null)
      setForm(defaultForm)
      refresh()
    },
  })

  const shops = data?.data ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const saveShop = () => {
    if (!form.name.trim() || !form.address.trim()) {
      window.alert('请填写店铺名称和地址')
      return
    }
    if (editingId) {
      updateMutation.mutate()
      return
    }
    createMutation.mutate()
  }

  return (
    <div className="section admin-panel">
      <div className="section-head">
        <h2>店铺管理</h2>
        <span className="muted">共 {total} 条</span>
      </div>

      <div className="admin-form-grid">
        <input
          placeholder="店铺名称"
          value={form.name}
          onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
        />
        <select
          value={form.typeId}
          onChange={(event) => setForm((prev) => ({ ...prev, typeId: Number(event.target.value) }))}
        >
          {types.map((type) => (
            <option key={type.id} value={type.id}>
              {type.name}
            </option>
          ))}
        </select>
        <input
          placeholder="地址"
          value={form.address}
          onChange={(event) => setForm((prev) => ({ ...prev, address: event.target.value }))}
        />
        <input
          placeholder="商圈"
          value={form.area || ''}
          onChange={(event) => setForm((prev) => ({ ...prev, area: event.target.value }))}
        />
        <input
          type="number"
          placeholder="人均"
          value={form.avgPrice ?? 0}
          onChange={(event) => setForm((prev) => ({ ...prev, avgPrice: Number(event.target.value || 0) }))}
        />
        <div className="op-row">
          <button
            className="primary-btn"
            onClick={saveShop}
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {editingId ? '保存修改' : '创建店铺'}
          </button>
          {editingId ? (
            <button
              className="ghost-btn"
              onClick={() => {
                setEditingId(null)
                setForm(defaultForm)
              }}
            >
              取消编辑
            </button>
          ) : null}
        </div>
      </div>

      <div className="toolbar admin-toolbar">
        <input placeholder="按店铺名搜索" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        <select
          value={typeId ?? 'all'}
          onChange={(event) => {
            const value = event.target.value
            setTypeId(value === 'all' ? undefined : Number(value))
            setPage(1)
          }}
        >
          <option value="all">全部分类</option>
          {types.map((type) => (
            <option key={type.id} value={type.id}>
              {type.name}
            </option>
          ))}
        </select>
        <button className="primary-btn" onClick={() => setPage(1)}>
          查询
        </button>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}
      {isFetching && !isLoading ? <p className="muted">刷新中...</p> : null}

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>店铺名</th>
              <th>分类</th>
              <th>地址</th>
              <th>人均</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {shops.map((shop) => (
              <tr key={shop.id}>
                <td>{shop.id}</td>
                <td>{shop.name}</td>
                <td>{shop.typeId}</td>
                <td>{shop.address || '—'}</td>
                <td>{formatPrice(shop.avgPrice)}</td>
                <td>{formatDateTime(shop.createTime)}</td>
                <td>
                  <button
                    className="ghost-btn"
                    onClick={() => {
                      setEditingId(shop.id)
                      setForm({
                        name: shop.name,
                        typeId: shop.typeId,
                        images: shop.images || '/imgs/blogs/blog1.jpg',
                        address: shop.address || '',
                        area: shop.area || '',
                        avgPrice: shop.avgPrice || 0,
                        x: 0,
                        y: 0,
                      })
                    }}
                  >
                    编辑
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="pager">
        <button className="ghost-btn" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
          上一页
        </button>
        <span>
          第 {page} / {totalPages} 页
        </span>
        <button className="ghost-btn" disabled={page >= totalPages} onClick={() => setPage((value) => value + 1)}>
          下一页
        </button>
      </div>
    </div>
  )
}
