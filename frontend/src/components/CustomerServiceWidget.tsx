import { useEffect, useMemo, useRef, useState } from 'react'
import { sendChatMessage } from '../services/modules/chatbot'
import type { ChatCard } from '../services/types'
import { useAuthStore } from '../store/auth'

type ChatItem = {
  role: 'user' | 'assistant'
  content: string
  suggestions?: string[]
  cards?: ChatCard[]
}

const QUICK_ACTIONS = [
  { label: '查店铺优惠', prompt: '我想看一下1号店铺有什么优惠券' },
  { label: '秒杀订单状态', prompt: '帮我查一下订单123456789的秒杀状态' },
  { label: '平台规则', prompt: '请介绍一下登录、优惠券和秒杀的常见规则' },
  { label: '转人工', prompt: '我想联系人工客服' },
]

const SESSION_KEY = 'dianping-cs-session-id'

const createSessionId = () => `cs-${Date.now()}-${Math.floor(Math.random() * 10000)}`

const getSessionId = () => {
  if (typeof window === 'undefined') {
    return createSessionId()
  }
  const existed = window.localStorage.getItem(SESSION_KEY)
  if (existed) {
    return existed
  }
  const sid = createSessionId()
  window.localStorage.setItem(SESSION_KEY, sid)
  return sid
}

export function CustomerServiceWidget() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [messages, setMessages] = useState<ChatItem[]>([
    {
      role: 'assistant',
      content: '你好，我是智能客服。可以帮你查店铺、优惠券、秒杀订单状态。',
    },
  ])
  const [error, setError] = useState('')

  const { user, token } = useAuthStore()
  const messagesEndRef = useRef<HTMLDivElement | null>(null)

  const sessionId = useMemo(() => getSessionId(), [])
  const userId = user?.id ? String(user.id) : 'guest'

  useEffect(() => {
    if (open) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
    }
  }, [messages, sending, open])

  const sendText = async (rawText: string) => {
    const text = rawText.trim()
    if (!text || sending) {
      return
    }
    setError('')
    setInput('')
    setMessages((prev) => [...prev, { role: 'user', content: text }])
    setSending(true)
    try {
      const resp = await sendChatMessage({
        session_id: sessionId,
        user_id: userId,
        message: text,
        user_token: token ?? undefined,
      })
      const toolText = resp.used_tools?.length ? `\n\n（已调用：${resp.used_tools.join('、')}）` : ''
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `${resp.answer}${toolText}`, suggestions: resp.suggestions, cards: resp.cards },
      ])
    } catch (e) {
      setError((e as Error).message || '客服服务暂时不可用，请稍后重试')
    } finally {
      setSending(false)
    }
  }

  const onSend = async () => {
    await sendText(input)
  }

  return (
    <div className="cs-widget-wrap">
      {open ? (
        <section className="cs-panel">
          <header className="cs-header">
            <div>
              <h4>智能客服</h4>
              <p className="muted small">会话ID: {sessionId}</p>
            </div>
            <button className="ghost-btn" onClick={() => setOpen(false)}>
              收起
            </button>
          </header>

          <div className="cs-messages">
            <div className="cs-quick-actions">
              {QUICK_ACTIONS.map((action) => (
                <button key={action.label} type="button" className="cs-chip" onClick={() => sendText(action.prompt)}>
                  {action.label}
                </button>
              ))}
            </div>
            {messages.map((item, idx) => (
              <div key={`${item.role}-${idx}`} className="cs-msg-group">
                <div className={`cs-msg ${item.role === 'user' ? 'is-user' : 'is-assistant'}`}>
                  <span>{item.content}</span>
                </div>
                {item.cards?.length ? <ChatCards cards={item.cards} /> : null}
                {item.suggestions?.length ? (
                  <div className="cs-suggestions">
                    {item.suggestions.map((suggestion) => (
                      <button key={suggestion} type="button" className="cs-chip" onClick={() => sendText(suggestion)}>
                        {suggestion}
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            ))}
            {sending ? <p className="muted small">客服思考中...</p> : null}
            {error ? <p className="error-text small">{error}</p> : null}
            <div ref={messagesEndRef} />
          </div>

          <div className="cs-input-row">
            <textarea
              value={input}
              maxLength={1000}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault()
                  onSend()
                }
              }}
              placeholder="请输入你的问题，例如：查订单123的秒杀状态"
            />
            <button className="primary-btn" disabled={sending || !input.trim()} onClick={onSend}>
              发送
            </button>
          </div>
        </section>
      ) : null}

      <button className="cs-float-btn" onClick={() => setOpen((v) => !v)}>
        {open ? '关闭客服' : '智能客服'}
      </button>
    </div>
  )
}

function ChatCards({ cards }: { cards: ChatCard[] }) {
  return (
    <div className="cs-cards">
      {cards.map((card, index) => (
        <article key={`${card.type}-${card.title}-${index}`} className={`cs-card cs-card-${card.type}`}>
          {card.image ? <img src={card.image} alt={card.title} /> : null}
          <div className="cs-card-body">
            <strong>{card.title}</strong>
            {card.subtitle ? <p>{card.subtitle}</p> : null}
            <CardMeta card={card} />
          </div>
        </article>
      ))}
    </div>
  )
}

function CardMeta({ card }: { card: ChatCard }) {
  if (card.type === 'shop') {
    return (
      <div className="cs-card-meta">
        {card.meta?.avgPrice ? <span>人均 ¥{card.meta.avgPrice}</span> : null}
        {card.meta?.score ? <span>评分 {Number(card.meta.score) / 10}</span> : null}
        {card.meta?.openHours ? <span>{card.meta.openHours}</span> : null}
      </div>
    )
  }
  if (card.type === 'voucher') {
    return (
      <div className="cs-card-meta">
        {card.meta?.payValue ? <span>售价 ¥{Number(card.meta.payValue) / 100}</span> : null}
        {card.meta?.actualValue ? <span>面值 ¥{Number(card.meta.actualValue) / 100}</span> : null}
        {card.meta?.type === 1 ? <span>秒杀券</span> : <span>普通券</span>}
      </div>
    )
  }
  return <div className="cs-card-meta">来源 chunk #{card.meta?.chunk}</div>
}
