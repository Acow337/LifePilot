import axios from 'axios'
import { useAuthStore } from '../../store/auth'
import type { ChatBotRequest, ChatBotResponse } from '../types'

const botHttp = axios.create({
  baseURL: import.meta.env.VITE_BOT_BASE_URL || '/bot',
  timeout: 20_000,
})

export const sendChatMessage = async (payload: ChatBotRequest): Promise<ChatBotResponse> => {
  const token = useAuthStore.getState().token
  const body: ChatBotRequest = {
    ...payload,
    user_token: payload.user_token ?? token ?? undefined,
  }
  const resp = await botHttp.post<ChatBotResponse>('/chat', body)
  return resp.data
}
