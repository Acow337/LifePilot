import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAdminVoucher,
  getAdminVouchers,
  updateAdminVoucher,
  updateAdminVoucherStatus,
  type AdminVoucherPayload,
} from '../../services/modules/admin'
import { formatDateTime } from '../../utils/format'

const PAGE_SIZE = 10

const defaultVoucher: AdminVoucherPayload = {
  shopId: 1,
  title: '',
  subTitle: '',
  rules: '',
  payValue: 100,
  actualValue: 120,
  type: 0,
  status: 1,
}

export function AdminVouchersPage() {
  const [page, setPage] = useState(1)
  const [title, setTitle] = useState('')
  const [status, setStatus] = useState<number | undefined>(undefined)
  const [type, setType] = useState<number | undefined>(undefined)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<AdminVoucherPayload>(defaultVoucher)
  const queryClient = useQueryClient()

  const queryKey = useMemo(() => ['admin-vouchers', page, title, status, type], [page, status, title, type])

  const { data, isLoading, isFetching } = useQuery({
    queryKey,
    queryFn: () =>
      getAdminVouchers({
        page,
        size: PAGE_SIZE,
        title: title.trim() || undefined,
        status,
        type,
      }),
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-vouchers'] })

  const createMutation = useMutation({
    mutationFn: () => createAdminVoucher(form),
    onSuccess: () => {
      setForm(defaultVoucher)
      refresh()
    },
  })

  const updateMutation = useMutation({
    mutationFn: () => updateAdminVoucher(editingId as number, form),
    onSuccess: () => {
      setEditingId(null)
      setForm(defaultVoucher)
      refresh()
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: number; nextStatus: number }) => updateAdminVoucherStatus(id, nextStatus),
    onSuccess: refresh,
  })

  const vouchers = data?.data ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const saveVoucher = () => {
    if (!form.title.trim()) {
      window.alert('请输入优惠券标题')
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
        <h2>优惠券管理</h2>
        <span className="muted">共 {total} 条</span>
      </div>

      <div className="admin-form-grid voucher-grid">
        <input
          type="number"
          placeholder="店铺ID"
          value={form.shopId}
          onChange={(event) => setForm((prev) => ({ ...prev, shopId: Number(event.target.value || 1) }))}
        />
        <input
          placeholder="标题"
          value={form.title}
          onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))}
        />
        <input
          placeholder="副标题"
          value={form.subTitle || ''}
          onChange={(event) => setForm((prev) => ({ ...prev, subTitle: event.target.value }))}
        />
        <input
          type="number"
          placeholder="支付金额"
          value={form.payValue}
          onChange={(event) => setForm((prev) => ({ ...prev, payValue: Number(event.target.value || 0) }))}
        />
        <input
          type="number"
          placeholder="抵扣金额"
          value={form.actualValue}
          onChange={(event) => setForm((prev) => ({ ...prev, actualValue: Number(event.target.value || 0) }))}
        />
        <select
          value={form.type}
          onChange={(event) => setForm((prev) => ({ ...prev, type: Number(event.target.value) }))}
        >
          <option value={0}>普通券</option>
          <option value={1}>秒杀券</option>
        </select>
        <select
          value={form.status}
          onChange={(event) => setForm((prev) => ({ ...prev, status: Number(event.target.value) }))}
        >
          <option value={1}>生效</option>
          <option value={0}>下架</option>
        </select>

        {form.type === 1 ? (
          <>
            <input
              type="number"
              placeholder="库存"
              value={form.stock ?? 0}
              onChange={(event) => setForm((prev) => ({ ...prev, stock: Number(event.target.value || 0) }))}
            />
            <input
              type="datetime-local"
              value={toDateTimeLocal(form.beginTime)}
              onChange={(event) => setForm((prev) => ({ ...prev, beginTime: event.target.value }))}
            />
            <input
              type="datetime-local"
              value={toDateTimeLocal(form.endTime)}
              onChange={(event) => setForm((prev) => ({ ...prev, endTime: event.target.value }))}
            />
          </>
        ) : null}

        <div className="op-row">
          <button
            className="primary-btn"
            onClick={saveVoucher}
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {editingId ? '保存修改' : '创建优惠券'}
          </button>
          {editingId ? (
            <button
              className="ghost-btn"
              onClick={() => {
                setEditingId(null)
                setForm(defaultVoucher)
              }}
            >
              取消编辑
            </button>
          ) : null}
        </div>
      </div>

      <div className="toolbar admin-toolbar">
        <input placeholder="按标题搜索" value={title} onChange={(event) => setTitle(event.target.value)} />
        <select
          value={status === undefined ? 'all' : String(status)}
          onChange={(event) => {
            const value = event.target.value
            setStatus(value === 'all' ? undefined : Number(value))
            setPage(1)
          }}
        >
          <option value="all">全部状态</option>
          <option value="1">生效</option>
          <option value="0">下架</option>
        </select>
        <select
          value={type === undefined ? 'all' : String(type)}
          onChange={(event) => {
            const value = event.target.value
            setType(value === 'all' ? undefined : Number(value))
            setPage(1)
          }}
        >
          <option value="all">全部类型</option>
          <option value="0">普通券</option>
          <option value="1">秒杀券</option>
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
              <th>标题</th>
              <th>店铺</th>
              <th>类型</th>
              <th>状态</th>
              <th>库存</th>
              <th>有效期</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {vouchers.map((voucher) => (
              <tr key={voucher.id}>
                <td>{voucher.id}</td>
                <td>{voucher.title}</td>
                <td>{voucher.shopId}</td>
                <td>{voucher.type === 1 ? '秒杀券' : '普通券'}</td>
                <td>{voucher.status === 1 ? '生效' : '下架'}</td>
                <td>{voucher.stock ?? '—'}</td>
                <td>
                  {formatDateTime(voucher.beginTime)} ~ {formatDateTime(voucher.endTime)}
                </td>
                <td>
                  <div className="op-row">
                    <button
                      className="ghost-btn"
                      onClick={() => {
                        setEditingId(voucher.id)
                        setForm({
                          shopId: voucher.shopId,
                          title: voucher.title,
                          subTitle: voucher.subTitle,
                          rules: voucher.rules,
                          payValue: voucher.payValue || 0,
                          actualValue: voucher.actualValue || 0,
                          type: voucher.type ?? 0,
                          status: voucher.status ?? 1,
                          stock: voucher.stock,
                          beginTime: voucher.beginTime,
                          endTime: voucher.endTime,
                        })
                      }}
                    >
                      编辑
                    </button>
                    <button
                      className="ghost-btn"
                      disabled={statusMutation.isPending}
                      onClick={() =>
                        statusMutation.mutate({
                          id: voucher.id,
                          nextStatus: voucher.status === 1 ? 0 : 1,
                        })
                      }
                    >
                      {voucher.status === 1 ? '下架' : '上架'}
                    </button>
                  </div>
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

function toDateTimeLocal(value?: string) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}`
}
