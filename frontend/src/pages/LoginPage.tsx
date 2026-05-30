import { useMutation } from '@tanstack/react-query'
import { type FormEvent, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { getMe, login } from '../services/modules/auth'
import { useAuthStore } from '../store/auth'

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirect = useMemo(() => {
    const value = searchParams.get('redirect') || '/'
    return value.startsWith('/') ? value : '/'
  }, [searchParams])

  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const { setToken, setUser } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: () => login({ phone: phone.trim(), password }),
    onSuccess: async (token) => {
      setToken(token)
      try {
        const me = await getMe()
        setUser(me)
      } catch {
        setUser(null)
      }
      navigate(redirect, { replace: true })
    },
    onError: (e: Error) => {
      setError(e.message || '登录失败')
    },
  })

  const submitLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!phone.trim() || !password) {
      setError('请输入邮箱和密码')
      return
    }
    loginMutation.mutate()
  }

  return (
    <div className="login-wrap">
      <div className="login-card fade-in">
        <p className="kicker">React + TypeScript + Query</p>
        <h1>欢迎回来</h1>
        <p className="muted">使用邮箱和密码登录你的点评账号。</p>
        <form onSubmit={submitLogin} className="form-grid">
          <label>
            邮箱
            <input
              type="email"
              placeholder="your@email.com"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              autoComplete="email"
            />
          </label>

          <label>
            密码
            <input
              type="password"
              placeholder="请输入密码"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
          </label>

          {error ? <p className="error-text">{error}</p> : null}

          <button type="submit" className="primary-btn" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? '登录中...' : '立即登录'}
          </button>
        </form>

        <p className="muted small">
          先看看内容？<Link to="/">返回首页</Link>
        </p>
      </div>
    </div>
  )
}
