import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAdminSeckillDlq, previewAdminSeckillDlqReplay, replayAdminSeckillDlq } from '../../services/modules/admin'
import type { SeckillDlqReplayPreviewResult } from '../../services/types'

const QUERY_KEY = ['admin-seckill-dlq']

export function AdminSeckillDlqPage() {
  const [limit, setLimit] = useState(20)
  const [voucherIdInput, setVoucherIdInput] = useState('')
  const [replayTip, setReplayTip] = useState('')
  const [previewTip, setPreviewTip] = useState('')
  const [previewResult, setPreviewResult] = useState<SeckillDlqReplayPreviewResult | null>(null)
  const [copyTip, setCopyTip] = useState('')
  const queryClient = useQueryClient()
  const voucherId = Number(voucherIdInput)
  const voucherFilter = Number.isFinite(voucherId) && voucherId > 0 ? voucherId : undefined

  const { data, isLoading, error } = useQuery({
    queryKey: [...QUERY_KEY, limit, voucherFilter],
    queryFn: () => getAdminSeckillDlq(limit, voucherFilter),
    refetchInterval: 5_000,
  })

  const replayMutation = useMutation({
    mutationFn: ({ size, voucher }: { size: number; voucher?: number }) => replayAdminSeckillDlq(size, voucher),
    onSuccess: (result) => {
      setReplayTip(
        `重放完成：扫描 ${result.scanned} 条，成功 ${result.replayed} 条，失败 ${result.failed} 条，剩余 ${result.remaining} 条`,
      )
      queryClient.invalidateQueries({ queryKey: QUERY_KEY })
    },
    onError: (e: Error) => setReplayTip(`重放失败：${e.message || '未知错误'}`),
  })

  const previewMutation = useMutation({
    mutationFn: ({ size, voucher }: { size: number; voucher?: number }) => previewAdminSeckillDlqReplay(size, voucher),
    onSuccess: (result) => {
      setPreviewResult(result)
      setPreviewTip(
        `预演完成：扫描 ${result.scanned} 条，预计可重放 ${result.canReplay} 条（请求 ${result.requested} 条）`,
      )
    },
    onError: (e: Error) => {
      setPreviewResult(null)
      setPreviewTip(`预演失败：${e.message || '未知错误'}`)
    },
  })

  const handleReplay = () => {
    const confirmText = voucherFilter
      ? `将重放 voucherId=${voucherFilter} 的死信消息，最多 ${limit} 条。此操作会重新触发下单流程，是否继续？`
      : `将重放死信消息，最多 ${limit} 条。此操作会重新触发下单流程，是否继续？`
    if (!window.confirm(confirmText)) {
      return
    }
    replayMutation.mutate({ size: limit, voucher: voucherFilter })
  }

  const handlePreview = () => {
    setCopyTip('')
    setPreviewTip('')
    previewMutation.mutate({ size: limit, voucher: voucherFilter })
  }

  const handleCopySampleOrderIds = async () => {
    const ids = previewResult?.sampleOrderIds
    if (!ids || ids.length === 0) {
      setCopyTip('没有可复制的样本订单号')
      return
    }
    try {
      await navigator.clipboard.writeText(ids.join(','))
      setCopyTip('样本订单号已复制')
    } catch {
      setCopyTip('复制失败，请手动复制')
    }
  }

  const snapshot = data
  const rows = snapshot?.messages ?? []

  return (
    <div className="section admin-panel">
      <div className="section-head">
        <h2>秒杀死信队列</h2>
        <span className="muted">队列：{snapshot?.queue ?? 'seckillQueue.dlq'}</span>
      </div>

      <div className="toolbar admin-toolbar">
        <input
          type="number"
          min={1}
          max={100}
          value={limit}
          onChange={(event) => setLimit(Math.max(1, Math.min(100, Number(event.target.value) || 20)))}
          placeholder="每次处理条数"
        />
        <input
          type="number"
          min={1}
          value={voucherIdInput}
          onChange={(event) => setVoucherIdInput(event.target.value)}
          placeholder="按 voucherId 过滤（可选）"
        />
        <button className="ghost-btn" onClick={() => queryClient.invalidateQueries({ queryKey: QUERY_KEY })}>
          刷新
        </button>
        <button className="ghost-btn" disabled={previewMutation.isPending} onClick={handlePreview}>
          {previewMutation.isPending ? '预演中...' : '预演重放'}
        </button>
        <button className="primary-btn" disabled={replayMutation.isPending} onClick={handleReplay}>
          {replayMutation.isPending ? '重放中...' : '重放死信'}
        </button>
      </div>

      <p className="muted">当前积压：{snapshot?.messageCount ?? 0} 条</p>
      <p className="error-text">风险提示：重放会再次触发下单，请优先按 voucherId 精确过滤并小批量执行。</p>
      {previewTip ? <p className="muted">{previewTip}</p> : null}
      {previewResult ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <div className="card-body">
            <h3>预演详情</h3>
            <p className="muted">
              requested={previewResult.requested}，canReplay={previewResult.canReplay}，scanned={previewResult.scanned}，
              remaining={previewResult.remaining}
            </p>
            <p className="muted">
              样本订单号（最多50条）：{previewResult.sampleOrderIds?.length ? previewResult.sampleOrderIds.join(', ') : '无'}
            </p>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button className="ghost-btn" onClick={handleCopySampleOrderIds}>
                复制样本订单号
              </button>
              {copyTip ? <span className="muted">{copyTip}</span> : null}
            </div>
          </div>
        </div>
      ) : null}
      {replayTip ? <p className="muted">{replayTip}</p> : null}
      {isLoading ? <p className="muted">加载中...</p> : null}
      {error instanceof Error ? <p className="error-text">{error.message}</p> : null}

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>deliveryTag</th>
              <th>routingKey</th>
              <th>redeliver</th>
              <th>messageId</th>
              <th>orderId</th>
              <th>voucherId</th>
              <th>payload</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item, idx) => (
              <tr key={`${item.deliveryTag}-${idx}`}>
                <td>{item.deliveryTag ?? '-'}</td>
                <td>{item.routingKey || '-'}</td>
                <td>{item.redeliver ? '是' : '否'}</td>
                <td>{item.messageId || '-'}</td>
                <td>{item.orderId ?? '-'}</td>
                <td>{item.voucherId ?? '-'}</td>
                <td style={{ maxWidth: 420, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{item.payload || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
