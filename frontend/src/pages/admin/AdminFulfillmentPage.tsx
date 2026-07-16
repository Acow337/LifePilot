import { useQuery } from '@tanstack/react-query'
import { getInventoryLedgers } from '../../services/modules/admin'
import { formatDateTime } from '../../utils/format'

export function AdminFulfillmentPage() {
  const { data: result, isLoading } = useQuery({
    queryKey: ['inventory-ledgers'],
    queryFn: () => getInventoryLedgers({ page: 1, size: 30 }),
  })
  const data = result?.data ?? []
  const total = result?.total ?? 0

  return (
    <div className="admin-panel fade-in">
      <div className="admin-toolbar">
        <div>
          <h2>履约与库存账本</h2>
          <p className="muted">记录库存预占、扣减、释放和回滚，解释“库存为什么变了”。</p>
        </div>
        <span className="pill">{total} 条流水</span>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>活动/券</th>
              <th>订单</th>
              <th>类型</th>
              <th>变化</th>
              <th>来源</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr key={item.id}>
                <td>{formatDateTime(item.createTime)}</td>
                <td>{item.campaignId || '-'} / {item.voucherId || '-'}</td>
                <td>{item.orderId || '-'}</td>
                <td><span className="status-badge">{item.changeType}</span></td>
                <td>{item.changeAmount > 0 ? `+${item.changeAmount}` : item.changeAmount}</td>
                <td>{item.source}</td>
                <td>{item.detail || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
