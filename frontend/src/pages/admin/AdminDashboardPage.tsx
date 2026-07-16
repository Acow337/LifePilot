import { useQuery } from '@tanstack/react-query'
import { getAdminDashboard } from '../../services/modules/admin'
import { formatCentPrice, formatDateTime } from '../../utils/format'

const percentText = (value?: number) => `${Number(value ?? 0).toFixed(2)}%`

export function AdminDashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: getAdminDashboard,
  })

  const maxStatusCount = Math.max(1, ...(data?.statusDistribution ?? []).map((item) => item.count))

  return (
    <div className="admin-panel fade-in">
      <section className="dashboard-hero">
        <div>
          <p className="kicker">Operations Radar</p>
          <h2>智能运营驾驶舱</h2>
          <p className="muted">汇总营销活动、订单履约、库存账本和 AI 客服表现，支撑活动复盘与异常治理。</p>
        </div>
        <div className="dashboard-hero-badge">
          <span>今日订单</span>
          <strong>{data?.todayOrders ?? 0}</strong>
        </div>
      </section>

      {isLoading ? <p className="muted">加载中...</p> : null}
      {error instanceof Error ? <p className="error-text">{error.message}</p> : null}

      {data ? (
        <>
          <section className="dashboard-grid">
            <div className="metric-card metric-main">
              <span>总 GMV</span>
              <strong>{formatCentPrice(data.gmv)}</strong>
              <small>已支付 / 已核销 / 退款中订单口径</small>
            </div>
            <div className="metric-card">
              <span>订单总量</span>
              <strong>{data.totalOrders}</strong>
              <small>支付转化 {percentText(data.conversionRate)}</small>
            </div>
            <div className="metric-card">
              <span>退款风险</span>
              <strong>{percentText(data.refundRate)}</strong>
              <small>{data.refundingOrders} 笔退款中，{data.refundedOrders} 笔已退款</small>
            </div>
            <div className="metric-card">
              <span>活动中心</span>
              <strong>{data.opsMetrics?.activeCampaigns ?? data.entities.campaigns ?? 0}</strong>
              <small>{data.entities.vouchers} 券 / {data.opsMetrics?.inventoryEvents ?? 0} 条库存流水</small>
            </div>
            <div className="metric-card">
              <span>AI 客服</span>
              <strong>{data.opsMetrics?.agentTurns ?? data.entities.agentTraces ?? 0}</strong>
              <small>失败率 {percentText(data.opsMetrics?.agentFailureRate)} / 异常单 {data.opsMetrics?.abnormalOrders ?? 0}</small>
            </div>
          </section>

          <section className="section dashboard-card">
            <div className="section-head">
              <h3>运营 Copilot 洞察</h3>
              <span className="muted">基于当前订单、活动和 Agent Trace 生成</span>
            </div>
            <div className="insight-list">
              {(data.copilotInsights ?? []).map((insight) => (
                <p key={insight}>✦ {insight}</p>
              ))}
            </div>
          </section>

          <section className="dashboard-split">
            <div className="section dashboard-card">
              <div className="section-head">
                <h3>订单状态分布</h3>
                <span className="muted">状态机视角</span>
              </div>
              <div className="status-bars">
                {data.statusDistribution.map((item) => (
                  <div className="status-bar-row" key={item.status}>
                    <span>{item.label}</span>
                    <div className="status-bar-track">
                      <i style={{ width: `${Math.max(6, (item.count / maxStatusCount) * 100)}%` }} />
                    </div>
                    <strong>{item.count}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="section dashboard-card">
              <div className="section-head">
                <h3>最近订单</h3>
                <span className="muted">实时运营样本</span>
              </div>
              <div className="table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>订单</th>
                      <th>优惠券</th>
                      <th>状态</th>
                      <th>金额</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.recentOrders.map((order) => (
                      <tr key={order.id}>
                        <td>{order.id}</td>
                        <td>{order.voucherTitle || `券 ${order.voucherId}`}</td>
                        <td>
                          <span className="status-pill">{order.statusText || '未知'}</span>
                        </td>
                        <td>{formatCentPrice(order.payValue)}</td>
                        <td>{formatDateTime(order.createTime)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </>
      ) : null}
    </div>
  )
}
