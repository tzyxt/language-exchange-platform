<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { currentUserId, request, uploadChatMedia } from '../lib/api'

const route = useRoute()
const posts = ref([])
const searchKeyword = ref('')
const searchResult = ref(null)
const imageInput = ref(null)
const uploading = ref(false)
const selectedTopics = ref([])

const topicOptions = ['校园生活', '口语练习', '文化交流', '城市探索', '学习打卡']

const form = reactive({
  content: '',
  imageUrl: ''
})

const normalizedSearchKeyword = computed(() => {
  return (route.query.keyword || searchKeyword.value || '').toString().trim()
})

async function load() {
  posts.value = await request('/posts?sortBy=latest')
}

function toggleTopic(topic) {
  if (selectedTopics.value.includes(topic)) {
    selectedTopics.value = selectedTopics.value.filter((item) => item !== topic)
  } else {
    selectedTopics.value = [...selectedTopics.value, topic]
  }
}

function openImagePicker() {
  imageInput.value?.click()
}

async function handleImageChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const uploaded = await uploadChatMedia(file)
    form.imageUrl = uploaded.url
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

async function publish() {
  if (!form.content.trim()) return

  await request('/posts', {
    method: 'POST',
    body: JSON.stringify({
      userId: currentUserId(),
      content: form.content.trim(),
      topics: selectedTopics.value,
      images: form.imageUrl ? [form.imageUrl] : []
    })
  })

  form.content = ''
  form.imageUrl = ''
  selectedTopics.value = []
  await load()
}

async function action(postId, actionType) {
  await request(`/posts/${postId}/actions`, {
    method: 'POST',
    body: JSON.stringify({ userId: currentUserId(), actionType })
  })
  await load()
}

async function comment(postId) {
  await request(`/posts/${postId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ userId: currentUserId(), content: '这条内容很有共鸣，欢迎继续交流。' })
  })
  await load()
}

async function search() {
  if (!searchKeyword.value.trim()) {
    searchResult.value = null
    return
  }
  searchResult.value = await request(`/search?keyword=${encodeURIComponent(searchKeyword.value.trim())}`)
}

watch(
  () => route.query.keyword,
  async (keyword) => {
    if (keyword) {
      searchKeyword.value = String(keyword)
      await search()
    }
  },
  { immediate: true }
)

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-4">
      <h2>发布动态</h2>

      <div class="field">
        <label>文字内容</label>
        <textarea v-model="form.content" />
      </div>

      <div class="field">
        <label>话题标签</label>
        <div class="community-topic-picker">
          <button
            v-for="topic in topicOptions"
            :key="topic"
            type="button"
            class="badge secondary community-topic-option"
            :class="{ active: selectedTopics.includes(topic) }"
            @click="toggleTopic(topic)"
          >
            {{ topic }}
          </button>
        </div>
      </div>

      <div class="field">
        <label>本地图片</label>
        <input ref="imageInput" type="file" accept="image/*" class="chat-hidden-input" @change="handleImageChange" />
        <div class="actions">
          <button class="secondary-button" type="button" @click="openImagePicker">
            {{ uploading ? '上传中...' : '选择本地图片' }}
          </button>
        </div>
        <img v-if="form.imageUrl" :src="form.imageUrl" alt="动态预览" class="community-upload-preview" />
      </div>

      <div class="actions">
        <button class="primary-button" @click="publish">立即发布</button>
      </div>
    </section>

    <section class="card span-8">
      <div class="actions">
        <input
          v-model="searchKeyword"
          placeholder="搜索用户、帖子或话题"
          style="flex: 1; padding: 12px 14px; border-radius: 16px; border: 1px solid var(--line);"
        />
        <button class="secondary-button" @click="search">搜索</button>
      </div>

      <div v-if="searchResult" class="item-card" style="margin-top: 16px;">
        <strong>搜索结果</strong>
        <p class="muted">用户 {{ searchResult.users.length }} 个，帖子 {{ searchResult.posts.length }} 条，话题 {{ searchResult.topics.length }} 个。</p>
      </div>

      <div class="list" style="margin-top: 16px;">
        <div
          v-for="post in posts"
          :key="post.id"
          class="item-card"
          :class="{ 'community-focus-card': route.query.focusPost === String(post.id) }"
        >
          <div class="actions">
            <strong>{{ post.author.name }}</strong>
            <span class="badge secondary">{{ post.author.school }}</span>
          </div>

          <p>{{ post.content }}</p>

          <div v-if="post.images?.length" class="community-post-image-list">
            <img
              v-for="image in post.images"
              :key="image"
              :src="image"
              alt="动态图片"
              class="community-post-image"
            />
          </div>

          <div class="actions">
            <span v-for="topic in post.topics" :key="topic" class="badge">{{ topic }}</span>
          </div>

          <p class="muted">点赞 {{ post.likeCount }} · 收藏 {{ post.favoriteCount }} · 评论 {{ post.commentCount }}</p>

          <div class="actions">
            <button class="secondary-button" @click="action(post.id, 'LIKE')">点赞</button>
            <button class="secondary-button" @click="action(post.id, 'FAVORITE')">收藏</button>
            <button class="secondary-button" @click="action(post.id, 'SHARE')">分享</button>
            <button class="ghost-button" @click="comment(post.id)">评论</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
