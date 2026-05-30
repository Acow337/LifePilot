import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAdminUsers, updateAdminUserRole, updateAdminUserStatus } from '../../services/modules/admin'

const PAGE_SIZE = 10

export function AdminUsersPage() {
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<number | undefined>(undefined)
  const queryClient = useQueryClient()

  const queryKey = useMemo(() => ['admin-users', page, keyword, status], [keyword, page, status])

  const { data, isLoading, isFetching } = useQuery({
    queryKey,
    queryFn: () => getAdminUsers({ page, size: PAGE_SIZE, keyword: keyword.trim() || undefined, status }),
  })

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['admin-users'] })

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: number; nextStatus: number }) => updateAdminUserStatus(id, nextStatus),
    onSuccess: refresh,
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({ id, nextRole }: { id: number; nextRole: number }) => updateAdminUserRole(id, nextRole),
    onSuccess: refresh,
  })

  const users = data?.data ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  return (
    <div className="section admin-panel">
      <div className="section-head">
        <h2>用户管理</h2>
        <span className="muted">共 {total} 条</span>
      </div>

      <div className="toolbar admin-toolbar">
        <input placeholder="搜索账号/昵称" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
        <select
          value={status === undefined ? 'all' : String(status)}
          onChange={(event) => {
            const value = event.target.value
            setStatus(value === 'all' ? undefined : Number(value))
            setPage(1)
          }}
        >
          <option value="all">全部状态</option>
          <option value="0">正常</option>
          <option value="1">封禁</option>
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
              <th>账号</th>
              <th>昵称</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.phone}</td>
                <td>{user.nickName}</td>
                <td>{user.role === 1 ? '管理员' : '普通用户'}</td>
                <td>{user.status === 1 ? '封禁' : '正常'}</td>
                <td>
                  <div className="op-row">
                    <button
                      className="ghost-btn"
                      disabled={updateStatusMutation.isPending}
                      onClick={() =>
                        updateStatusMutation.mutate({
                          id: user.id,
                          nextStatus: user.status === 1 ? 0 : 1,
                        })
                      }
                    >
                      {user.status === 1 ? '解封' : '封禁'}
                    </button>
                    <button
                      className="ghost-btn"
                      disabled={updateRoleMutation.isPending}
                      onClick={() =>
                        updateRoleMutation.mutate({
                          id: user.id,
                          nextRole: user.role === 1 ? 0 : 1,
                        })
                      }
                    >
                      {user.role === 1 ? '降为用户' : '设为管理员'}
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
