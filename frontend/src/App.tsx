import { useEffect } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { CustomerServiceWidget } from './components/CustomerServiceWidget'
import { getMe } from './services/modules/auth'
import { useAuthStore } from './store/auth'

function App() {
  const { token, user, logout, setUser } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!token || user) {
      return
    }
    getMe()
      .then((me) => setUser(me))
      .catch(() => setUser(null))
  }, [setUser, token, user])

  return (
    <div className="layout">
      <header className="topbar">
        <Link to="/" className="brand">
          简约点评
        </Link>
        <nav className="nav">
          <Link to="/">首页</Link>
          <Link to="/shops">店铺</Link>
          {user?.role === 1 ? <Link to="/admin">后台</Link> : null}
        </nav>
        <div className="actions">
          {token ? (
            <>
              <span className="user-pill">{user?.nickName ?? '已登录'}</span>
              <button className="ghost-btn" onClick={logout}>
                退出
              </button>
            </>
          ) : (
            <button
              className="primary-btn"
              onClick={() => navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`)}
            >
              登录
            </button>
          )}
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
      <CustomerServiceWidget />
    </div>
  )
}

export default App
