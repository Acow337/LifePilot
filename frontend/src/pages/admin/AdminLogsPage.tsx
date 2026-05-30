import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAdminLogs } from '../../services/modules/admin'
import { formatDateTime } from '../../utils/format'

const PAGE_SIZE = 20

export function AdminLogsPage() {
  const [page, setPage] = useState(1)
  const [module, setModule] = useState('')
  const [action, setAction] = useState('')

  const queryKey = useMemo(() => ['admin-logs', page, module, action], [action, module, page])

  const { data, isLoading, error } = useQuery({
    queryKey,
    queryFn: () =>
      getAdminLogs({
        page,
        size: PAGE_SIZE,
        module: module.trim() || undefined,
        action: action.trim() || undefined,
      }),
  })

  const logs = data?.data ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <div className="section admin-panel">
      <div className="section-head">
        <h2>操作日志</h2>
        <span className="muted">共 {total} 条</span>
      </div>

      <div className="toolbar admin-toolbar">
        <input placeholder="模块，如 user/blog" value={module} onChange={(event) => setModule(event.target.value)} />
        <input placeholder="动作，如 update_status" value={action} onChange={(event) => setAction(event.target.value)} />
        <button className="primary-btn" onClick={() => setPage(1)}>
          查询
        </button>
      </div>

      {isLoading ? <p className="muted">加载中...</p> : null}
      {error instanceof Error ? <p className="error-text">{error.message}</p> : null}

      <div className="table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>管理员</th>
              <th>模块</th>
              <th>动作</th>
              <th>对象</th>
              <th>对象ID</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{log.id}</td>
                <td>{log.operatorId}</td>
                <td>{log.module}</td>
                <td>{log.action}</td>
                <td>{log.targetType}</td>
                <td>{log.targetId}</td>
                <td>{formatDateTime(log.createTime)}</td>
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
