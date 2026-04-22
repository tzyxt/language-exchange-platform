<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../../lib/api'

const reports = ref([])

async function load() {
  reports.value = await request('/admin/reports')
}

async function process(id, status) {
  await request(`/admin/reports/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  })
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <h2>举报与内容审核</h2>
      <div class="table-list">
        <div v-for="item in reports" :key="item.id" class="table-item">
          <div>
            <strong>{{ item.targetType }} #{{ item.targetId }}</strong>
            <p class="muted">{{ item.reportUser.name }}</p>
          </div>
          <span>{{ item.reason }}</span>
          <span class="badge">{{ item.status }}</span>
          <div class="actions">
            <button class="secondary-button" @click="process(item.id, 'RESOLVED')">通过</button>
            <button class="ghost-button" @click="process(item.id, 'REJECTED')">驳回</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
