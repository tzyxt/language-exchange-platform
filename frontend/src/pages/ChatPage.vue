<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { currentUserId, request, uploadChatMedia } from '../lib/api'
import { sessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()

const sessions = ref([])
const active = ref(null)
const detail = ref(null)
const text = ref('')
const keyword = ref('')
const loading = ref(false)
const sending = ref(false)
const favorites = ref([])
const recorderState = ref('idle')
const recorderHint = ref('')
const messageBoard = ref(null)
const imageInput = ref(null)
const previewImage = ref('')

let pollTimer = null
let mediaRecorder = null
let mediaStream = null
let audioChunks = []

const quickPrompts = [
  '聊聊你最近参加的一次校园活动吧',
  '你最想推荐的一道家乡美食是什么？',
  '最近在学的语言表达里，哪句话最有意思？'
]

const currentUserAvatar = computed(() => {
  return sessionStore.currentUser?.avatar || 'https://i.pravatar.cc/120?img=12'
})

const hasSessions = computed(() => sessions.value.length > 0)
const activePeer = computed(() => detail.value?.session?.peer || null)

const filteredSessions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return sessions.value
  return sessions.value.filter((item) => {
    const name = item.peer?.name || ''
    const preview = item.lastMessage || ''
    return `${name} ${preview}`.toLowerCase().includes(query)
  })
})

const conversationSummary = computed(() => {
  const peer = activePeer.value
  if (!peer) return []
  return [
    peer.communicationGoal,
    ...(peer.targetTags || []),
    ...(peer.interestTags || [])
  ].filter(Boolean).slice(0, 6)
})

const profileTags = computed(() => {
  const peer = activePeer.value
  if (!peer) return []
  return [
    peer.nativeLanguage && `母语 ${peer.nativeLanguage}`,
    peer.targetLanguage && `目标 ${peer.targetLanguage}`,
    peer.preferredMode && `偏好 ${peer.preferredMode}`,
    ...(peer.interestTags || []).slice(0, 3)
  ].filter(Boolean)
})

const decoratedMessages = computed(() => {
  return (detail.value?.messages || []).map((msg) => ({
    ...msg,
    isMine: msg.senderId === currentUserId()
  }))
})

const sessionHighlights = computed(() => {
  return sessions.value.slice(0, 3).map((item) => ({
    ...item,
    preview: item.lastMessage || '开始新的交流吧'
  }))
})

const vocabularyCards = computed(() => {
  const messages = detail.value?.messages || []
  const dictionary = []
  const seen = new Set()

  for (const msg of messages) {
    const words = String(msg.content || '')
      .replace(/[^\w\s-]/g, ' ')
      .split(/\s+/)
      .filter((word) => word.length >= 7)

    for (const word of words) {
      const normalized = word.toLowerCase()
      if (!seen.has(normalized)) {
        seen.add(normalized)
        dictionary.push({
          word,
          phonetic: `/${normalized.slice(0, 3)}.../`,
          meaning: '本轮对话中出现的高频词，可以继续围绕它展开跨文化交流。',
          favored: favorites.value.includes(normalized)
        })
      }
      if (dictionary.length >= 4) return dictionary
    }
  }

  return [
    { word: 'Language', phonetic: '/ˈlæŋɡwɪdʒ/', meaning: '语言，是跨文化表达与理解的基础。', favored: favorites.value.includes('language') },
    { word: 'Culture', phonetic: '/ˈkʌltʃər/', meaning: '文化，帮助我们理解对方的生活方式与价值观。', favored: favorites.value.includes('culture') },
    { word: 'Exchange', phonetic: '/ɪksˈtʃeɪndʒ/', meaning: '交流，强调双向分享、倾听与回应。', favored: favorites.value.includes('exchange') }
  ]
})

async function syncRouteSession(sessionId) {
  const nextValue = sessionId ? String(sessionId) : undefined
  const currentValue = route.query.session ? String(route.query.session) : undefined
  if (nextValue === currentValue) return
  await router.replace({
    path: '/chat',
    query: nextValue ? { session: nextValue } : undefined
  })
}

async function scrollToBottom() {
  await nextTick()
  if (messageBoard.value) {
    messageBoard.value.scrollTop = messageBoard.value.scrollHeight
  }
}

function resetPoll() {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(() => {
    refreshSessions({ keepCurrent: true, silent: true })
  }, 10000)
}

async function openSession(id, options = {}) {
  if (!id) return
  const { syncRoute = true, force = false } = options
  if (!force && active.value === id && detail.value) return

  active.value = id
  detail.value = await request(`/chats/sessions/${id}?userId=${currentUserId()}`)

  if (syncRoute) {
    await syncRouteSession(id)
  }

  await scrollToBottom()
}

async function refreshSessions(options = {}) {
  const { preferredId = null, keepCurrent = false, silent = false } = options
  if (!silent) loading.value = true

  try {
    const sessionData = await request(`/chats/${currentUserId()}/sessions`)
    sessions.value = sessionData

    if (!sessionData.length) {
      active.value = null
      detail.value = null
      await syncRouteSession(null)
      return
    }

    const queryId = Number(route.query.session)
    const requestedId = Number(preferredId)
    const targetId =
      (Number.isFinite(requestedId) && requestedId) ||
      (keepCurrent ? active.value : null) ||
      active.value ||
      (Number.isFinite(queryId) && queryId) ||
      sessionData[0].id

    const existing = sessionData.find((item) => item.id === targetId) || sessionData[0]
    await openSession(existing.id, { force: true })
  } finally {
    if (!silent) loading.value = false
  }
}

async function sendMessage(messageType, payload) {
  const sessionId = active.value
  if (!sessionId || sending.value) return

  sending.value = true
  try {
    await request(`/chats/sessions/${sessionId}/messages`, {
      method: 'POST',
      body: JSON.stringify({
        senderId: currentUserId(),
        messageType,
        content: payload.content,
        fileUrl: payload.fileUrl || null
      })
    })

    await refreshSessions({ preferredId: sessionId, keepCurrent: true })
  } finally {
    sending.value = false
  }
}

async function sendText() {
  const content = text.value.trim()
  if (!content) return
  text.value = ''
  await sendMessage('TEXT', { content })
}

function openImagePicker() {
  if (!active.value) return
  imageInput.value?.click()
}

async function handleImageChange(event) {
  const file = event.target.files?.[0]
  if (!file || !active.value) return

  const uploaded = await uploadChatMedia(file)
  await sendMessage('IMAGE', {
    content: `分享了一张图片：${uploaded.filename}`,
    fileUrl: uploaded.url
  })

  event.target.value = ''
}

async function toggleRecording() {
  if (!active.value) return

  if (recorderState.value === 'recording') {
    mediaRecorder?.stop()
    recorderState.value = 'processing'
    recorderHint.value = '正在处理录音...'
    return
  }

  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    recorderHint.value = '当前浏览器不支持录音'
    return
  }

  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(mediaStream)
    audioChunks = []

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) audioChunks.push(event.data)
    }

    mediaRecorder.onstop = async () => {
      try {
        const mimeType = mediaRecorder.mimeType || 'audio/webm'
        const extension = mimeType.includes('ogg') ? 'ogg' : 'webm'
        const audioBlob = new Blob(audioChunks, { type: mimeType })
        const audioFile = new File([audioBlob], `chat-audio-${Date.now()}.${extension}`, { type: mimeType })
        const uploaded = await uploadChatMedia(audioFile)

        await sendMessage('AUDIO', {
          content: '发送了一段语音，继续练习口语表达吧。',
          fileUrl: uploaded.url
        })

        recorderState.value = 'idle'
        recorderHint.value = ''
      } catch {
        recorderState.value = 'idle'
        recorderHint.value = '录音发送失败，请重试'
      } finally {
        mediaStream?.getTracks().forEach((track) => track.stop())
        mediaStream = null
        mediaRecorder = null
        audioChunks = []
      }
    }

    mediaRecorder.start()
    recorderState.value = 'recording'
    recorderHint.value = '录音中，再点一次发送'
  } catch {
    recorderHint.value = '无法访问麦克风，请检查浏览器权限'
    recorderState.value = 'idle'
  }
}

function usePrompt(prompt) {
  text.value = prompt
}

function translateDraft() {
  const draft = text.value.trim()
  if (!draft) {
    text.value = '可以用更自然的中文介绍一下你的校园生活吗？'
    return
  }
  text.value = `${draft}\n\n[翻译辅助] 请帮我把上面这句话换成更自然的双语表达。`
}

function toggleFavorite(word) {
  const normalized = word.toLowerCase()
  if (favorites.value.includes(normalized)) {
    favorites.value = favorites.value.filter((item) => item !== normalized)
  } else {
    favorites.value = [...favorites.value, normalized]
  }
}

function openImagePreview(url) {
  previewImage.value = url
}

function closeImagePreview() {
  previewImage.value = ''
}

function handleEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  sendText()
}

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function handlePreviewEscape(event) {
  if (event.key === 'Escape' && previewImage.value) {
    closeImagePreview()
  }
}

watch(
  () => route.query.session,
  async (sessionId) => {
    const nextId = Number(sessionId)
    if (Number.isFinite(nextId) && nextId && nextId !== active.value) {
      const exists = sessions.value.find((item) => item.id === nextId)
      if (exists) {
        await openSession(nextId, { syncRoute: false, force: true })
      } else {
        await refreshSessions({ preferredId: nextId })
      }
    }
  }
)

watch(
  () => detail.value?.messages?.length,
  async () => {
    await scrollToBottom()
  }
)

onMounted(async () => {
  await refreshSessions()
  resetPoll()
  window.addEventListener('keydown', handlePreviewEscape)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  mediaStream?.getTracks().forEach((track) => track.stop())
  window.removeEventListener('keydown', handlePreviewEscape)
})
</script>

<template>
  <div class="chat-layout">
    <input
      ref="imageInput"
      type="file"
      accept="image/*"
      class="chat-hidden-input"
      @change="handleImageChange"
    />

    <aside class="chat-sidebar">
      <div class="chat-sidebar-header">
        <span class="chat-kicker">聊天交流</span>
        <h2>活跃对话</h2>
        <p>继续你最想聊下去的那场交流，把语言练习放进真实语境里。</p>
      </div>

      <div class="chat-search">
        <input v-model="keyword" placeholder="搜索伙伴或消息内容..." />
      </div>

      <div class="chat-session-list">
        <template v-if="filteredSessions.length">
          <button
            v-for="item in filteredSessions"
            :key="item.id"
            class="chat-session-card"
            :class="{ active: item.id === active }"
            @click="openSession(item.id)"
          >
            <img :src="item.peer.avatar" :alt="item.peer.name" class="chat-session-avatar" />
            <div class="chat-session-copy">
              <div class="chat-session-title">
                <strong>{{ item.peer.name }}</strong>
                <span>{{ formatTime(item.lastMessageTime) }}</span>
              </div>
              <p>{{ item.lastMessage || '开始新的交流吧' }}</p>
            </div>
            <span v-if="item.unreadCount" class="chat-session-badge">{{ item.unreadCount }}</span>
          </button>
        </template>

        <div v-else class="chat-empty-state">
          你还没有任何聊天会话，先去智能匹配里发起联系吧。
        </div>
      </div>
    </aside>

    <section class="chat-conversation">
      <template v-if="detail && activePeer">
        <header class="chat-conversation-header">
          <div class="chat-conversation-heading">
            <span class="chat-presence-dot"></span>
            <div>
              <h2>与 {{ activePeer.name }} 对话中</h2>
              <p>{{ activePeer.nativeLanguage }} · {{ activePeer.targetLanguage }}</p>
            </div>
          </div>
          <div class="chat-conversation-actions">
            <button class="chat-icon-button" type="button">视频</button>
            <button class="chat-icon-button" type="button">语音</button>
            <button class="chat-icon-button" type="button">更多</button>
          </div>
        </header>

        <div class="chat-topic-strip">
          <span class="chat-topic-label">对话灵感</span>
          <span v-for="tag in conversationSummary" :key="tag" class="chat-topic-pill">{{ tag }}</span>
        </div>

        <div class="chat-message-board" ref="messageBoard">
          <div v-if="loading" class="chat-board-state">正在加载会话内容...</div>
          <div v-else-if="!decoratedMessages.length" class="chat-board-state">这段对话还没有消息，先打个招呼吧。</div>

          <template v-else>
            <div
              v-for="msg in decoratedMessages"
              :key="msg.id"
              class="chat-message-row"
              :class="{ mine: msg.isMine }"
            >
              <img
                :src="msg.isMine ? currentUserAvatar : activePeer.avatar"
                :alt="msg.senderName"
                class="chat-bubble-avatar"
              />
              <div class="chat-bubble-stack">
                <div class="chat-bubble" :class="{ mine: msg.isMine }">
                  <template v-if="msg.messageType === 'IMAGE'">
                    <div class="chat-media-card">
                      <img :src="msg.fileUrl" alt="图片消息" class="chat-media-image" @click="openImagePreview(msg.fileUrl)" />
                      <p>{{ msg.content }}</p>
                    </div>
                  </template>

                  <template v-else-if="msg.messageType === 'AUDIO'">
                    <div class="chat-audio-card">
                      <span class="chat-audio-pill">语音</span>
                      <audio v-if="msg.fileUrl" controls :src="msg.fileUrl" class="chat-audio-player"></audio>
                      <p>{{ msg.content }}</p>
                    </div>
                  </template>

                  <template v-else>
                    <p>{{ msg.content }}</p>
                  </template>
                </div>
                <span class="chat-bubble-time">{{ formatTime(msg.sendTime) }}</span>
              </div>
            </div>
          </template>
        </div>

        <div class="chat-composer">
          <div class="chat-quick-prompts">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              type="button"
              class="chat-prompt-button"
              @click="usePrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>

          <textarea
            v-model="text"
            placeholder="输入消息，按 Enter 发送，Shift + Enter 换行"
            @keydown.enter="handleEnter"
          ></textarea>

          <div class="chat-composer-bar">
            <div class="chat-composer-tools">
              <button class="chat-tool-button" type="button" @click="openImagePicker">图片</button>
              <button class="chat-tool-button" type="button" @click="toggleRecording">
                {{ recorderState === 'recording' ? '结束录音' : '语音' }}
              </button>
              <button class="chat-tool-button" type="button" @click="translateDraft">翻译</button>
            </div>
            <button class="chat-send-button" type="button" :disabled="sending || !text.trim()" @click="sendText">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
          <p v-if="recorderHint" class="chat-recorder-hint">{{ recorderHint }}</p>
        </div>
      </template>

      <template v-else>
        <div class="chat-empty-panel">
          <span class="chat-kicker">等待发起联系</span>
          <h2>聊天界面已准备好</h2>
          <p>现在还没有建立会话。去智能匹配页点“发起联系”，系统会自动为你打开对应聊天窗口。</p>
          <button class="primary-button" type="button" @click="router.push('/matches')">前往智能匹配</button>
        </div>
      </template>
    </section>

    <aside class="chat-insight-panel">
      <template v-if="detail && activePeer">
        <div class="chat-profile-card">
          <img :src="activePeer.avatar" :alt="activePeer.name" class="chat-profile-avatar" />
          <span class="chat-profile-badge">{{ activePeer.languageLevel || '交流中' }}</span>
          <h3>{{ activePeer.name }}</h3>
          <p>{{ activePeer.introduction || '喜欢分享真实生活里的语言细节，也愿意倾听不同文化背景下的故事。' }}</p>
          <div class="chat-profile-tags">
            <span v-for="tag in profileTags" :key="tag" class="chat-topic-pill">{{ tag }}</span>
          </div>
        </div>

        <div class="chat-session-summary">
          <h3>会话概览</h3>
          <div class="chat-summary-list">
            <div v-for="item in sessionHighlights" :key="item.id" class="chat-summary-item">
              <strong>{{ item.peer.name }}</strong>
              <span>{{ formatTime(item.lastMessageTime) }}</span>
              <p>{{ item.preview }}</p>
            </div>
          </div>
        </div>

        <div class="chat-insight-header">
          <div>
            <span class="chat-kicker">词汇助手</span>
            <h3>本次对话关键词库</h3>
          </div>
          <button class="chat-icon-button" type="button">收藏</button>
        </div>

        <div class="chat-word-list">
          <article v-for="item in vocabularyCards" :key="item.word" class="chat-word-card">
            <div class="chat-word-title">
              <strong>{{ item.word }}</strong>
              <div class="chat-word-actions">
                <button class="chat-audio-button" type="button">发音</button>
                <button
                  class="chat-audio-button"
                  type="button"
                  :class="{ active: item.favored }"
                  @click="toggleFavorite(item.word)"
                >
                  {{ item.favored ? '已藏' : '收藏' }}
                </button>
              </div>
            </div>
            <span>{{ item.phonetic }}</span>
            <p>{{ item.meaning }}</p>
          </article>
        </div>
      </template>

      <template v-else>
        <div class="chat-empty-side-card">
          <span class="chat-kicker">交流准备</span>
          <h3>从匹配开始</h3>
          <p>建立联系后，这里会显示对方资料、会话概览和词汇助手，不会再整页空白。</p>
        </div>
      </template>
    </aside>

    <div v-if="previewImage" class="chat-image-lightbox" @click="closeImagePreview">
      <button type="button" class="chat-lightbox-close" @click.stop="closeImagePreview">关闭</button>
      <img :src="previewImage" alt="放大图片" class="chat-lightbox-image" @click.stop />
    </div>
  </div>
</template>
