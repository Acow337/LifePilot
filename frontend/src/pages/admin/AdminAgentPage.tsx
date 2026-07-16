import { useQuery } from '@tanstack/react-query'
import { getAgentTraces } from '../../services/modules/admin'
import { formatDateTime } from '../../utils/format'

export function AdminAgentPage() {
  const { data: result, isLoading } = useQuery({
    queryKey: ['agent-traces'],
    queryFn: () => getAgentTraces({ page: 1, size: 30 }),
  })
  const data = result?.data ?? []
  const total = result?.total ?? 0

  return (
    <div className="admin-panel fade-in">
      <div className="admin-toolbar">
        <div>
          <h2>AI Agent 工作台</h2>
          <p className="muted">查看客服意图、工具调用、错误码和回复摘要，让 AI 处理链路可解释。</p>
        </div>
        <span className="pill">{total} 次对话</span>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}

      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Trace</th>
              <th>会话/用户</th>
              <th>意图</th>
              <th>问题</th>
              <th>工具</th>
              <th>耗时</th>
              <th>错误</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            {data.map((trace) => (
              <tr key={trace.id}>
                <td><code>{trace.traceId?.slice(0, 8)}</code></td>
                <td>{trace.sessionId}<br /><small>{trace.userId}</small></td>
                <td><span className="status-badge">{trace.intent || 'unknown'}</span></td>
                <td>{trace.message}</td>
                <td>{trace.usedTools || '[]'}</td>
                <td>{trace.latencyMs || 0}ms</td>
                <td>{trace.errorCode || '-'}</td>
                <td>{formatDateTime(trace.createTime)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
