import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelVoucherOrder, getMyVoucherOrders, payVoucherOrder, requestVoucherRefund } from '../services/modules/voucher'
import { formatCentPrice, formatDateTime } from '../utils/format'

const statusClass = (status?: number) => {
  if (status === 2 || status === 3) {
    return 'ok'
  }
  if (status === 5) {
    return 'warn'
  }
  if (status === 4 || status === 6) {
    return 'muted-pill'
  }
  return ''
}

export function OrdersPage() {
  const [tip, setTip] = useState('')
  const { data: orders = [], isLoading, error, refetch } = useQuery({
    queryKey: ['my-voucher-orders'],
    queryFn: getMyVoucherOrders,
  })
  const payMutation = useMutation({
    mutationFn: (orderId: number) => payVoucherOrder(orderId),
  })
  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => cancelVoucherOrder(orderId),
  })
  const refundMutation = useMutation({
    mutationFn: (orderId: number) => requestVoucherRefund(orderId),
  })

  const handlePay = async (orderId: number) => {
    setTip('')
    try {
      const result = await payMutation.mutateAsync(orderId)
      setTip(result.message || '支付成功')
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '支付失败，请稍后重试')
    }
  }

  const handleCancel = async (orderId: number) => {
    setTip('')
    try {
      const result = await cancelMutation.mutateAsync(orderId)
      setTip(result.message || '订单已取消')
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '取消失败，请稍后重试')
    }
  }

  const handleRefund = async (orderId: number) => {
    setTip('')
    try {
      const result = await refundMutation.mutateAsync(orderId)
      setTip(result.message || '退款申请已提交')
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '退款申请失败，请稍后重试')
    }
  }

  const actionPending = payMutation.isPending || cancelMutation.isPending || refundMutation.isPending

  return (
    <div className="page fade-in">
      <section className="section">
        <div className="section-head">
          <div>
            <p className="kicker">Voucher Orders</p>
            <h2>我的订单</h2>
          </div>
          <button className="ghost-btn" onClick={() => refetch()}>
            刷新
          </button>
        </div>

        {isLoading ? <p className="muted">订单加载中...</p> : null}
        {error ? <p className="error-text">订单加载失败，请确认已登录</p> : null}
        {tip ? <p className="muted">{tip}</p> : null}
        {!isLoading && !error && orders.length === 0 ? (
          <div className="empty-state">
            <h3>还没有订单</h3>
            <p className="muted">去店铺详情页购买优惠券或参与秒杀后，这里会展示订单状态。</p>
            <Link className="primary-btn" to="/shops">
              去逛店铺
            </Link>
          </div>
        ) : null}

        <div className="order-list">
          {orders.map((order) => (
            <article key={order.id} className="order-card">
              <div className="order-main">
                <div>
                  <h3>{order.voucherTitle || `优惠券 #${order.voucherId}`}</h3>
                  <p className="muted">{order.shopName || '未知店铺'}</p>
                </div>
                <span className={`status-pill ${statusClass(order.status)}`}>{order.statusText || '未知'}</span>
              </div>
              <div className="meta">
                <span>订单号 {order.id}</span>
                <span>类型 {order.voucherType === 1 ? '秒杀券' : '普通券'}</span>
                <span>支付 {formatCentPrice(order.payValue)}</span>
                <span>抵扣 {formatCentPrice(order.actualValue)}</span>
              </div>
              <div className="meta">
                <span>下单 {formatDateTime(order.createTime)}</span>
                {order.payTime ? <span>支付 {formatDateTime(order.payTime)}</span> : null}
                {order.useTime ? <span>核销 {formatDateTime(order.useTime)}</span> : null}
                {order.refundTime ? <span>退款 {formatDateTime(order.refundTime)}</span> : null}
              </div>
              <div className="order-actions">
                {order.status === 1 ? (
                  <button className="primary-btn" onClick={() => handlePay(order.id)} disabled={actionPending}>
                    {payMutation.isPending ? '支付中...' : '模拟支付'}
                  </button>
                ) : null}
                {order.status === 1 ? (
                  <button className="ghost-btn" onClick={() => handleCancel(order.id)} disabled={actionPending}>
                    取消订单
                  </button>
                ) : null}
                {order.status === 2 ? <span className="redeem-code">核销码：{order.id}</span> : null}
                {order.status === 2 ? (
                  <button className="ghost-btn" onClick={() => handleRefund(order.id)} disabled={actionPending}>
                    申请退款
                  </button>
                ) : null}
                {order.status === 3 ? <span className="status-pill ok">已完成核销</span> : null}
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
