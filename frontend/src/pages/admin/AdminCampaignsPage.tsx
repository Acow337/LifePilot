import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAdminCampaigns, updateAdminCampaignStatus } from '../../services/modules/admin'
import { formatCentPrice, formatDateTime } from '../../utils/format'

const statusOptions = [
  { value: 1, label: '上线' },
  { value: 2, label: '暂停' },
  { value: 3, label: '结束' },
]

export function AdminCampaignsPage() {
  const queryClient = useQueryClient()
  const { data: result, isLoading } = useQuery({
    queryKey: ['admin-campaigns'],
    queryFn: () => getAdminCampaigns({ page: 1, size: 20 }),
  })
  const data = result?.data ?? []
  const total = result?.total ?? 0

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateAdminCampaignStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-campaigns'] }),
  })

  return (
    <div className="admin-panel fade-in">
      <div className="admin-toolbar">
        <div>
          <h2>营销活动中心</h2>
          <p className="muted">用 Campaign 统一承载普通券、秒杀券、会员券和库存预占规则。</p>
        </div>
        <span className="pill">{total} 个活动</span>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>活动</th>
              <th>类型</th>
              <th>店铺</th>
              <th>库存</th>
              <th>预算/券</th>
              <th>时间窗</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {!isLoading && data.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <p className="muted">暂无 Campaign。首次访问后端会尝试从存量优惠券生成活动视图，也可以通过接口创建新活动。</p>
                </td>
              </tr>
            ) : null}
            {data.map((campaign) => (
              <tr key={campaign.id}>
                <td>
                  <div className="admin-title-cell">
                    <strong>{campaign.name}</strong>
                    <span>{campaign.description || campaign.voucherTitle || `Campaign #${campaign.id}`}</span>
                  </div>
                </td>
                <td>{campaign.typeText || campaign.type}</td>
                <td>{campaign.shopName || campaign.shopId}</td>
                <td>
                  {campaign.stockAvailable}/{campaign.stockTotal}
                </td>
                <td>{formatCentPrice(campaign.budgetCent || campaign.payValue || 0)}</td>
                <td>
                  <small>
                    {formatDateTime(campaign.beginTime)}
                    <br />
                    {formatDateTime(campaign.endTime)}
                  </small>
                </td>
                <td><span className="status-badge">{campaign.statusText || campaign.status}</span></td>
                <td>
                  <select
                    value={campaign.status}
                    onChange={(event) => statusMutation.mutate({ id: campaign.id, status: Number(event.target.value) })}
                  >
                    {statusOptions.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
