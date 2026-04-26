<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { currentUserId, request, uploadChatMedia, WS_ORIGIN } from '../lib/api'
import { getFavoriteVocabulary, removeFavoriteVocabulary, saveFavoriteVocabulary } from '../lib/vocabulary'
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
const favorites = ref(getFavoriteVocabulary())
const recorderState = ref('idle')
const recorderHint = ref('')
const messageBoard = ref(null)
const imageInput = ref(null)
const previewImage = ref('')
const saveNotice = ref('')

const socketReady = ref(false)
const incomingCall = ref(null)
const callState = ref('idle')
const callMode = ref('')
const callStatus = ref('')
const callMuted = ref(false)
const callCameraOff = ref(false)
const localVideoRef = ref(null)
const remoteVideoRef = ref(null)
const localCallStreamRef = ref(null)
const remoteCallStreamRef = ref(null)

let pollTimer = null
let mediaRecorder = null
let mediaStream = null
let audioChunks = []
let signalSocket = null
let peerConnection = null
let callTargetUserId = null
let saveNoticeTimer = null

const quickPrompts = [
  '聊聊你最近参加的一次校园活动吧',
  '你最想推荐的一道家乡美食是什么？',
  '最近在学的语言表达里，哪句话最有意思？'
]

const vocabularyThemePool = {
  language: [
    { word: 'Practice', phonetic: '/ˈpræk.tɪs/', meaning: '练习，适合表达持续提升语言能力。', topic: '口语练习' },
    { word: 'Fluent', phonetic: '/ˈfluː.ənt/', meaning: '流利的，适合聊表达自然度和进步感。', topic: '口语练习' },
    { word: 'Express', phonetic: '/ɪkˈspres/', meaning: '表达，用来介绍观点、感受和故事。', topic: '语言交流' }
  ],
  culture: [
    { word: 'Tradition', phonetic: '/trəˈdɪʃ.ən/', meaning: '传统，可以延伸到节日、习俗和家庭文化。', topic: '文化交流' },
    { word: 'Perspective', phonetic: '/pəˈspek.tɪv/', meaning: '视角，适合讨论不同文化下的理解差异。', topic: '文化交流' },
    { word: 'Community', phonetic: '/kəˈmjuː.nə.ti/', meaning: '群体、社区，适合聊校园和城市氛围。', topic: '文化交流' }
  ],
  campus: [
    { word: 'Semester', phonetic: '/səˈmes.tər/', meaning: '学期，适合聊课程安排和校园节奏。', topic: '校园生活' },
    { word: 'Workshop', phonetic: '/ˈwɜːk.ʃɒp/', meaning: '工作坊，可用于活动和社团交流。', topic: '校园生活' },
    { word: 'Schedule', phonetic: '/ˈskedʒ.uːl/', meaning: '日程安排，适合约时间或讨论学习计划。', topic: '校园生活' }
  ],
  movie: [
    { word: 'Character', phonetic: '/ˈkær.ək.tər/', meaning: '角色，适合聊电影人物和剧情印象。', topic: '电影' },
    { word: 'Scene', phonetic: '/siːn/', meaning: '场景，可以描述电影片段或城市画面。', topic: '电影' },
    { word: 'Subtitle', phonetic: '/ˈsʌbˌtaɪ.təl/', meaning: '字幕，特别适合双语电影和语言学习场景。', topic: '电影' }
  ],
  city: [
    { word: 'Neighborhood', phonetic: '/ˈneɪ.bə.hʊd/', meaning: '街区，适合聊城市探索和生活氛围。', topic: '城市探索' },
    { word: 'Landmark', phonetic: '/ˈlænd.mɑːk/', meaning: '地标，适合推荐值得打卡的地方。', topic: '城市探索' },
    { word: 'Explore', phonetic: '/ɪkˈsplɔːr/', meaning: '探索，适合邀请对方一起体验城市。', topic: '城市探索' }
  ],
  design: [
    { word: 'Creative', phonetic: '/kriˈeɪ.tɪv/', meaning: '有创意的，适合聊设计作品和灵感。', topic: '设计' },
    { word: 'Visual', phonetic: '/ˈvɪʒ.u.əl/', meaning: '视觉的，可以评价画面和风格。', topic: '设计' },
    { word: 'Inspiration', phonetic: '/ˌɪn.spəˈreɪ.ʃən/', meaning: '灵感，适合继续追问创作来源。', topic: '设计' }
  ],
  sports: [
    { word: 'Training', phonetic: '/ˈtreɪ.nɪŋ/', meaning: '训练，适合聊运动习惯和身体状态。', topic: '运动' },
    { word: 'Teamwork', phonetic: '/ˈtiːm.wɜːk/', meaning: '团队协作，适合聊球类活动和合作体验。', topic: '运动' },
    { word: 'Match', phonetic: '/mætʃ/', meaning: '比赛，也适合自然过渡到平台匹配话题。', topic: '运动' }
  ],
  food: [
    { word: 'Flavor', phonetic: '/ˈfleɪ.vər/', meaning: '风味，适合聊家乡美食和口味偏好。', topic: '美食' },
    { word: 'Recipe', phonetic: '/ˈres.ə.pi/', meaning: '食谱，可以延伸到饮食文化和家庭记忆。', topic: '美食' },
    { word: 'Recommend', phonetic: '/ˌrek.əˈmend/', meaning: '推荐，适合邀请对方分享想安利的食物。', topic: '美食' }
  ]
}

const currentUserAvatar = computed(() => sessionStore.currentUser?.avatar || 'https://i.pravatar.cc/120?img=12')
const activePeer = computed(() => detail.value?.session?.peer || null)
const peerById = computed(() => new Map(sessions.value.map((item) => [item.peer.id, item.peer])))
const callPeer = computed(() => {
  if (incomingCall.value) return peerById.value.get(incomingCall.value.fromUserId) || null
  if (callTargetUserId) return peerById.value.get(callTargetUserId) || activePeer.value || null
  return activePeer.value || null
})
const showCallModal = computed(() => callState !== 'idle' || Boolean(incomingCall.value))

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
  return [peer.communicationGoal, ...(peer.targetTags || []), ...(peer.interestTags || [])].filter(Boolean).slice(0, 6)
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

const decoratedMessages = computed(() =>
  (detail.value?.messages || []).map((msg) => ({
    ...msg,
    isMine: msg.senderId === currentUserId()
  }))
)

const sessionHighlights = computed(() =>
  sessions.value.slice(0, 3).map((item) => ({
    ...item,
    preview: item.lastMessage || '开始新的交流吧'
  }))
)

const favoriteLookup = computed(() => new Set(favorites.value.map((item) => item.normalized)))

const vocabularyCards = computed(() => {
  const peer = activePeer.value
  const messages = detail.value?.messages || []
  const topicText = [
    peer?.communicationGoal,
    ...(peer?.targetTags || []),
    ...(peer?.interestTags || []),
    ...messages.slice(-8).map((item) => item.content || '')
  ]
    .join(' ')
    .toLowerCase()

  const themes = []
  if (/(口语|语言|中文|english|speak|practice)/.test(topicText)) themes.push('language')
  if (/(文化|tradition|culture)/.test(topicText)) themes.push('culture')
  if (/(校园|学院|课程|semester|campus)/.test(topicText)) themes.push('campus')
  if (/(电影|movie|film|subtitle)/.test(topicText)) themes.push('movie')
  if (/(城市|漫步|city|travel|explore)/.test(topicText)) themes.push('city')
  if (/(设计|design|creative|visual)/.test(topicText)) themes.push('design')
  if (/(足球|运动|sport|basketball|training)/.test(topicText)) themes.push('sports')
  if (/(美食|家乡|food|flavor|recipe)/.test(topicText)) themes.push('food')
  if (!themes.length) themes.push('language', 'culture', 'campus')

  const messageWords = messages
    .flatMap((msg) => String(msg.content || '').match(/[A-Za-z][A-Za-z-]{3,15}/g) || [])
    .filter((word, index, list) => list.findIndex((item) => item.toLowerCase() === word.toLowerCase()) === index)
    .slice(0, 3)
    .map((word) => ({
      word,
      phonetic: `/${word.toLowerCase()}/`,
      meaning: '这轮对话里真实出现过的英文词，可以顺着它继续追问和扩展。',
      topic: '会话实词'
    }))

  const thematicWords = themes
    .flatMap((theme) => vocabularyThemePool[theme] || [])
    .filter((item, index, list) => list.findIndex((entry) => entry.word.toLowerCase() === item.word.toLowerCase()) === index)

  return [...messageWords, ...thematicWords]
    .filter((item, index, list) => list.findIndex((entry) => entry.word.toLowerCase() === item.word.toLowerCase()) === index)
    .slice(0, 5)
    .map((item) => ({
      ...item,
      favored: favoriteLookup.value.has(item.word.toLowerCase())
    }))
})

async function syncRouteSession(sessionId) {
  const nextValue = sessionId ? String(sessionId) : undefined
  const currentValue = route.query.session ? String(route.query.session) : undefined
  if (nextValue === currentValue) return
  await router.replace({ path: '/chat', query: nextValue ? { session: nextValue } : undefined })
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

async function ensureSessionForPeer(targetUserId) {
  const existing = sessions.value.find((item) => item.peer.id === targetUserId)
  if (existing) {
    await openSession(existing.id, { force: true })
  }
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
    recorderHint.value = '录音中，再点一次即可发送'
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

function toggleFavorite(item) {
  if (favoriteLookup.value.has(item.word.toLowerCase())) {
    favorites.value = removeFavoriteVocabulary(item.word)
    return
  }
  favorites.value = saveFavoriteVocabulary(item)
}

function saveCurrentVocabulary() {
  let next = favorites.value
  vocabularyCards.value.forEach((item) => {
    next = saveFavoriteVocabulary(item)
  })
  favorites.value = next
  saveNotice.value = `已收藏 ${vocabularyCards.value.length} 个词卡，可在“我的主页”查看。`
  if (saveNoticeTimer) window.clearTimeout(saveNoticeTimer)
  saveNoticeTimer = window.setTimeout(() => {
    saveNotice.value = ''
  }, 2400)
}

function pronounceWord(word) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  const utterance = new SpeechSynthesisUtterance(word)
  utterance.lang = 'en-US'
  utterance.rate = 0.92
  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(utterance)
}

function openImagePreview(url) {
  previewImage.value = url
}

function closeImagePreview() {
  previewImage.value = ''
}

async function ensureLocalCallStream(mode) {
  const needsVideo = mode === 'video'
  const currentStream = localCallStreamRef.value
  const hasVideo = currentStream?.getVideoTracks()?.length

  if (currentStream && (!needsVideo || hasVideo)) {
    await nextTick()
    if (localVideoRef.value) {
      localVideoRef.value.srcObject = currentStream
    }
    return currentStream
  }

  currentStream?.getTracks().forEach((track) => track.stop())
  const stream = await navigator.mediaDevices.getUserMedia({
    audio: true,
    video: needsVideo
  })
  localCallStreamRef.value = stream
  await nextTick()
  if (localVideoRef.value) {
    localVideoRef.value.srcObject = stream
  }
  return stream
}

async function createPeerConnection(targetUserId) {
  closePeerConnection()

  peerConnection = new RTCPeerConnection({
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
  })

  const remoteStream = new MediaStream()
  remoteCallStreamRef.value = remoteStream
  await nextTick()
  if (remoteVideoRef.value) {
    remoteVideoRef.value.srcObject = remoteStream
  }

  peerConnection.ontrack = (event) => {
    event.streams[0].getTracks().forEach((track) => remoteStream.addTrack(track))
  }

  peerConnection.onicecandidate = (event) => {
    if (event.candidate) {
      sendSignal({
        type: 'ice-candidate',
        targetUserId,
        candidate: event.candidate
      })
    }
  }

  peerConnection.onconnectionstatechange = () => {
    const state = peerConnection?.connectionState
    if (state === 'connected') {
      callState.value = 'connected'
      callStatus.value = '连接成功，你们现在可以实时通话了。'
    } else if (state === 'connecting') {
      callState.value = 'connecting'
      callStatus.value = '正在建立实时连接...'
    } else if (state === 'failed' || state === 'disconnected' || state === 'closed') {
      callStatus.value = '通话连接已结束。'
      endCall(false)
    }
  }

  const localStream = localCallStreamRef.value
  if (localStream) {
    localStream.getTracks().forEach((track) => {
      peerConnection.addTrack(track, localStream)
    })
  }
}

function closePeerConnection() {
  if (peerConnection) {
    peerConnection.onicecandidate = null
    peerConnection.ontrack = null
    peerConnection.onconnectionstatechange = null
    peerConnection.close()
    peerConnection = null
  }
}

function cleanupCallResources() {
  closePeerConnection()
  if (localVideoRef.value) {
    localVideoRef.value.srcObject = null
  }
  if (remoteVideoRef.value) {
    remoteVideoRef.value.srcObject = null
  }
  localCallStreamRef.value?.getTracks().forEach((track) => track.stop())
  localCallStreamRef.value = null
  remoteCallStreamRef.value = null
  callMuted.value = false
  callCameraOff.value = false
  callTargetUserId = null
  callMode.value = ''
  incomingCall.value = null
  callState.value = 'idle'
}

function sendSignal(payload) {
  if (!signalSocket || signalSocket.readyState !== WebSocket.OPEN) {
    callStatus.value = '实时信令连接不可用，请刷新页面重试。'
    return
  }
  signalSocket.send(JSON.stringify(payload))
}

async function startCall(mode) {
  if (!activePeer.value) return
  if (!socketReady.value) {
    callStatus.value = '实时连接还未准备好，请稍后重试。'
    return
  }

  callMode.value = mode
  callTargetUserId = activePeer.value.id
  callState.value = 'outgoing'
  callStatus.value = mode === 'video' ? '正在呼叫对方加入视频通话...' : '正在呼叫对方加入语音通话...'

  try {
    await ensureLocalCallStream(mode)
    sendSignal({
      type: 'invite',
      targetUserId: activePeer.value.id,
      mode
    })
  } catch {
    callStatus.value = '无法访问摄像头或麦克风，请检查浏览器权限。'
    cleanupCallResources()
  }
}

async function acceptIncomingCall() {
  if (!incomingCall.value) return
  const { fromUserId, mode } = incomingCall.value
  callMode.value = mode
  callTargetUserId = fromUserId

  try {
    await ensureSessionForPeer(fromUserId)
    await ensureLocalCallStream(mode)
    await createPeerConnection(fromUserId)
    sendSignal({
      type: 'call-accepted',
      targetUserId: fromUserId,
      mode
    })
    callState.value = 'connecting'
    callStatus.value = '已接听，正在等待对方建立连接...'
    incomingCall.value = null
  } catch {
    callStatus.value = '无法接通通话，请检查设备权限。'
    rejectIncomingCall()
  }
}

function rejectIncomingCall() {
  if (!incomingCall.value) return
  sendSignal({
    type: 'call-declined',
    targetUserId: incomingCall.value.fromUserId
  })
  incomingCall.value = null
  cleanupCallResources()
}

async function handleAcceptedCall(message) {
  callState.value = 'connecting'
  callStatus.value = '对方已接听，正在建立连接...'

  await createPeerConnection(message.fromUserId)
  const offer = await peerConnection.createOffer()
  await peerConnection.setLocalDescription(offer)
  sendSignal({
    type: 'offer',
    targetUserId: message.fromUserId,
    mode: callMode.value,
    sdp: offer
  })
}

async function handleOffer(message) {
  callMode.value = message.mode || callMode.value || 'audio'
  callTargetUserId = message.fromUserId
  if (!localCallStreamRef.value) {
    await ensureLocalCallStream(callMode.value)
  }
  await createPeerConnection(message.fromUserId)
  await peerConnection.setRemoteDescription(new RTCSessionDescription(message.sdp))
  const answer = await peerConnection.createAnswer()
  await peerConnection.setLocalDescription(answer)
  sendSignal({
    type: 'answer',
    targetUserId: message.fromUserId,
    sdp: answer
  })
}

async function handleAnswer(message) {
  if (!peerConnection) return
  await peerConnection.setRemoteDescription(new RTCSessionDescription(message.sdp))
}

async function handleIceCandidate(message) {
  if (!peerConnection || !message.candidate) return
  await peerConnection.addIceCandidate(new RTCIceCandidate(message.candidate))
}

function endCall(notifyPeer = true) {
  if (notifyPeer && callTargetUserId) {
    sendSignal({
      type: 'end-call',
      targetUserId: callTargetUserId
    })
  }
  cleanupCallResources()
}

function forceCloseCallModal() {
  cleanupCallResources()
}

function rejectIncomingCallAndClose() {
  if (incomingCall.value) {
    rejectIncomingCall()
    return
  }
  forceCloseCallModal()
}

function toggleMuteCall() {
  callMuted.value = !callMuted.value
  localCallStreamRef.value?.getAudioTracks().forEach((track) => {
    track.enabled = !callMuted.value
  })
}

function toggleCameraCall() {
  callCameraOff.value = !callCameraOff.value
  localCallStreamRef.value?.getVideoTracks().forEach((track) => {
    track.enabled = !callCameraOff.value
  })
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
    return
  }
  if (event.key === 'Escape' && showCallModal.value) {
    forceCloseCallModal()
  }
}

function connectSignalSocket() {
  const userId = currentUserId()
  if (!userId) return

  signalSocket = new WebSocket(`${WS_ORIGIN}/ws/calls?userId=${userId}`)
  signalSocket.onopen = () => {
    socketReady.value = true
  }
  signalSocket.onclose = () => {
    socketReady.value = false
  }
  signalSocket.onerror = () => {
    socketReady.value = false
    callStatus.value = '实时信令连接异常，请刷新页面重试。'
  }
  signalSocket.onmessage = async (event) => {
    const message = JSON.parse(event.data)

    if (message.type === 'connected') return
    if (message.type === 'error') {
      callStatus.value = message.message || '通话信令出错。'
      if (callState.value !== 'connected') {
        cleanupCallResources()
      }
      return
    }

    if (message.type === 'invite') {
      await ensureSessionForPeer(message.fromUserId)
      incomingCall.value = {
        fromUserId: message.fromUserId,
        mode: message.mode
      }
      callMode.value = message.mode
      callTargetUserId = message.fromUserId
      callStatus.value = message.mode === 'video' ? '对方向你发起了视频通话。' : '对方向你发起了语音通话。'
      return
    }

    if (message.type === 'call-accepted') {
      await handleAcceptedCall(message)
      return
    }

    if (message.type === 'call-declined') {
      callStatus.value = '对方拒绝了通话请求。'
      cleanupCallResources()
      return
    }

    if (message.type === 'offer') {
      await handleOffer(message)
      return
    }

    if (message.type === 'answer') {
      await handleAnswer(message)
      return
    }

    if (message.type === 'ice-candidate') {
      await handleIceCandidate(message)
      return
    }

    if (message.type === 'end-call') {
      callStatus.value = '对方已结束通话。'
      endCall(false)
    }
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

watch(
  () => localCallStreamRef.value,
  async (stream) => {
    await nextTick()
    if (localVideoRef.value) {
      localVideoRef.value.srcObject = stream || null
    }
  }
)

watch(
  () => remoteCallStreamRef.value,
  async (stream) => {
    await nextTick()
    if (remoteVideoRef.value) {
      remoteVideoRef.value.srcObject = stream || null
    }
  }
)

onMounted(async () => {
  await refreshSessions()
  resetPoll()
  connectSignalSocket()
  window.addEventListener('keydown', handlePreviewEscape)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (saveNoticeTimer) window.clearTimeout(saveNoticeTimer)
  mediaStream?.getTracks().forEach((track) => track.stop())
  cleanupCallResources()
  signalSocket?.close()
  window.removeEventListener('keydown', handlePreviewEscape)
})
</script>

<template>
  <div class="chat-layout">
    <input ref="imageInput" type="file" accept="image/*" class="chat-hidden-input" @change="handleImageChange" />

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
            <button class="chat-icon-button" type="button" @click="startCall('video')">视频</button>
            <button class="chat-icon-button" type="button" @click="startCall('audio')">语音</button>
            <button class="chat-icon-button" type="button" @click="saveCurrentVocabulary">收藏词卡</button>
          </div>
        </header>

        <div v-if="saveNotice" class="message-box success-box">{{ saveNotice }}</div>

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
              <img :src="msg.isMine ? currentUserAvatar : activePeer.avatar" :alt="msg.senderName" class="chat-bubble-avatar" />
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
          <h2>聊天界面已经准备好</h2>
          <p>现在还没有建立会话。去智能匹配页点击“发起联系”，系统会自动为你打开对应聊天窗口。</p>
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
          <button class="chat-icon-button" type="button" @click="saveCurrentVocabulary">
            收藏 {{ vocabularyCards.length }} 个
          </button>
        </div>

        <div class="chat-word-list">
          <article v-for="item in vocabularyCards" :key="item.word" class="chat-word-card">
            <div class="chat-word-title">
              <div class="chat-word-meta">
                <strong>{{ item.word }}</strong>
                <span class="badge secondary">{{ item.topic }}</span>
              </div>
              <div class="chat-word-actions">
                <button class="chat-audio-button" type="button" @click="pronounceWord(item.word)">发音</button>
                <button
                  class="chat-audio-button"
                  type="button"
                  :class="{ active: item.favored }"
                  @click="toggleFavorite(item)"
                >
                  {{ item.favored ? '已收藏' : '收藏' }}
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

    <div v-if="showCallModal" class="chat-call-overlay" @click="forceCloseCallModal">
      <div class="chat-call-modal" @click.stop>
        <div class="chat-call-header">
          <div>
            <span class="chat-kicker">{{ (incomingCall?.mode || callMode) === 'video' ? '视频通话' : '语音通话' }}</span>
            <h3>
              {{
                incomingCall
                  ? `${callPeer?.name || '对方'} 正在呼叫你`
                  : `与 ${callPeer?.name || '对方'} 的实时通话`
              }}
            </h3>
          </div>
          <button class="ghost-button" type="button" @click.stop.prevent="rejectIncomingCallAndClose">
            {{ incomingCall ? '拒绝' : '挂断' }}
          </button>
        </div>

        <div class="chat-call-preview-grid" :class="{ audio: (incomingCall?.mode || callMode) !== 'video' }">
          <div class="chat-call-preview-card">
            <span class="muted">我的画面</span>
            <video
              v-if="(incomingCall?.mode || callMode) === 'video'"
              ref="localVideoRef"
              class="chat-call-video"
              autoplay
              muted
              playsinline
            ></video>
            <div v-else class="chat-call-audio-state">
              <strong>{{ sessionStore.currentUser?.name || '我' }}</strong>
              <p>麦克风已接入，可直接进行语音通话。</p>
            </div>
          </div>

          <div class="chat-call-preview-card">
            <span class="muted">对方画面</span>
            <video
              v-if="(incomingCall?.mode || callMode) === 'video'"
              ref="remoteVideoRef"
              class="chat-call-video"
              autoplay
              playsinline
            ></video>
            <div v-else class="chat-call-audio-state">
              <strong>{{ callPeer?.name || '对方' }}</strong>
              <p>接听后会自动建立实时语音连接。</p>
            </div>
          </div>
        </div>

        <p class="chat-call-status">{{ callStatus }}</p>

        <div class="chat-call-actions">
          <template v-if="incomingCall">
            <button class="secondary-button" type="button" @click.stop.prevent="rejectIncomingCallAndClose">稍后再接</button>
            <button class="primary-button" type="button" @click.stop.prevent="acceptIncomingCall">立即接听</button>
          </template>
          <template v-else>
            <button class="secondary-button" type="button" @click.stop.prevent="toggleMuteCall">
              {{ callMuted ? '取消静音' : '静音麦克风' }}
            </button>
            <button
              v-if="callMode === 'video'"
              class="secondary-button"
              type="button"
              @click.stop.prevent="toggleCameraCall"
            >
              {{ callCameraOff ? '打开摄像头' : '关闭摄像头' }}
            </button>
            <button class="primary-button" type="button" @click.stop.prevent="forceCloseCallModal">结束通话</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
