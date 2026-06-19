import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { redeemVoucherOrder } from '../../services/modules/voucher'

export function AdminRedeemPage() {
  const [orderId, setOrderId] = useState('')
  const [message, setMessage] = useState('')

  const redeemMutation = useMutation({
    mutationFn: (id: number) => redeemVoucherOrder(id),
  })

  const handleRedeem = async () => {
    const parsed = Number(orderId)
    if (!Number.isFinite(parsed) || parsed <= 0) {
      setMessage('请输入正确的核销码')
      return
    }
    setMessage('')
    try {
      const result = await redeemMutation.mutateAsync(parsed)
      setMessage(`${result.message || '核销成功'}，订单号：${result.orderId}`)
    } catch (e) {
      setMessage((e as Error).message || '核销失败，请稍后重试')
    }
  }

  return (
    <div className="admin-panel fade-in">
      <section className="section">
        <div className="section-head">
          <div>
            <p className="kicker">Redeem</p>
            <h2>优惠券核销</h2>
          </div>
        </div>
        <div className="redeem-panel">
          <label>
            核销码 / 订单号
            <input value={orderId} onChange={(event) => setOrderId(event.target.value)} placeholder="例如 9001001" />
          </label>
          <button className="primary-btn" onClick={handleRedeem} disabled={redeemMutation.isPending}>
            {redeemMutation.isPending ? '核销中...' : '确认核销'}
          </button>
          {message ? <p className={message.includes('成功') ? 'muted' : 'error-text'}>{message}</p> : null}
        </div>
      </section>
    </div>
  )
}
