import { useEffect, useMemo } from 'react'
import { Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { getMe } from '../../services/modules/auth'
import { useAuthStore } from '../../store/auth'

const menus = [
  { to: '/admin/dashboard', label: '数据看板' },
  { to: '/admin/users', label: '用户管理' },
  { to: '/admin/blogs', label: '笔记审核' },
  { to: '/admin/shops', label: '店铺管理' },
  { to: '/admin/vouchers', label: '优惠券管理' },
  { to: '/admin/redeem', label: '优惠券核销' },
  { to: '/admin/refunds', label: '退款审核' },
  { to: '/admin/seckill-dlq', label: '秒杀死信' },
  { to: '/admin/logs', label: '操作日志' },
]

export function AdminLayout() {
  const { token, user, setUser } = useAuthStore()
  const location = useLocation()
  const navigate = useNavigate()

  const redirectTo = useMemo(
    () => encodeURIComponent(location.pathname + location.search),
    [location.pathname, location.search],
  )

  useEffect(() => {
    if (!token || user) {
      return
    }
    getMe()
      .then((me) => setUser(me))
      .catch(() => setUser(null))
  }, [setUser, token, user])

  if (!token) {
    return <Navigate to={`/login?redirect=${redirectTo}`} replace />
  }

  if (!user) {
    return <div className="section">正在校验身份...</div>
  }

  if (user.role !== 1) {
    return (
      <section className="section">
        <h2>无权限访问后台</h2>
        <p className="muted">当前账号不是管理员，请使用管理员账号登录。</p>
        <button className="primary-btn" onClick={() => navigate('/')}>
          返回首页
        </button>
      </section>
    )
  }

  return (
    <div className="admin-layout fade-in">
      <aside className="admin-side">
        <h3>后台管理</h3>
        <nav className="admin-nav">
          {menus.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <section className="admin-content">
        <Outlet />
      </section>
    </div>
  )
}
