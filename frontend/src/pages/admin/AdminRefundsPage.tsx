import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import {
  approveVoucherRefund,
  cancelExpiredVoucherOrders,
  getMyVoucherOrders,
  rejectVoucherRefund,
} from '../../services/modules/voucher'
import { formatCentPrice, formatDateTime } from '../../utils/format'

export function AdminRefundsPage() {
  const [tip, setTip] = useState('')
  const { data: orders = [], isLoading, error, refetch } = useQuery({
    queryKey: ['admin-refund-orders'],
    queryFn: getMyVoucherOrders,
  })

  const approveMutation = useMutation({ mutationFn: (orderId: number) => approveVoucherRefund(orderId) })
  const rejectMutation = useMutation({ mutationFn: (orderId: number) => rejectVoucherRefund(orderId) })
  const timeoutMutation = useMutation({ mutationFn: cancelExpiredVoucherOrders })

  const refundingOrders = orders.filter((order) => order.status === 5)
  const actionPending = approveMutation.isPending || rejectMutation.isPending || timeoutMutation.isPending

  const handleApprove = async (orderId: number) => {
    setTip('')
    try {
      const result = await approveMutation.mutateAsync(orderId)
      setTip(result.message || '退款已通过')
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '退款审核失败')
    }
  }

  const handleReject = async (orderId: number) => {
    setTip('')
    try {
      const result = await rejectMutation.mutateAsync(orderId)
      setTip(result.message || '退款已拒绝')
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '退款审核失败')
    }
  }

  const handleCancelExpired = async () => {
    setTip('')
    try {
      const result = await timeoutMutation.mutateAsync()
      setTip(`已取消 ${result.canceled} 个超过 ${result.timeoutMinutes} 分钟未支付订单`)
      await refetch()
    } catch (e) {
      setTip((e as Error).message || '超时订单处理失败')
    }
  }

  return (
    <div className="admin-panel fade-in">
      <section className="section">
        <div className="section-head">
          <div>
            <p className="kicker">Refund Review</p>
            <h2>退款审核</h2>
          </div>
          <div className="op-row">
            <button className="ghost-btn" onClick={() => refetch()}>
              刷新
            </button>
            <button className="primary-btn" onClick={handleCancelExpired} disabled={actionPending}>
              取消超时未支付
            </button>
          </div>
        </div>

        {isLoading ? <p className="muted">加载中...</p> : null}
        {error ? <p className="error-text">加载失败，请确认管理员登录</p> : null}
        {tip ? <p className="muted">{tip}</p> : null}

        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>订单</th>
                <th>优惠券</th>
                <th>金额</th>
                <th>状态</th>
                <th>时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {refundingOrders.map((order) => (
                <tr key={order.id}>
                  <td>{order.id}</td>
                  <td>
                    <strong>{order.voucherTitle || `优惠券 #${order.voucherId}`}</strong>
                    <p className="muted small">{order.shopName || '未知店铺'}</p>
                  </td>
                  <td>
                    <p>支付 {formatCentPrice(order.payValue)}</p>
                    <p className="muted small">抵扣 {formatCentPrice(order.actualValue)}</p>
                  </td>
                  <td>
                    <span className="status-pill warn">{order.statusText || '退款中'}</span>
                  </td>
                  <td>{formatDateTime(order.createTime)}</td>
                  <td>
                    <div className="op-row">
                      <button className="primary-btn" onClick={() => handleApprove(order.id)} disabled={actionPending}>
                        通过退款
                      </button>
                      <button className="ghost-btn" onClick={() => handleReject(order.id)} disabled={actionPending}>
                        拒绝退款
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {!isLoading && refundingOrders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted">
                    暂无退款中订单
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
