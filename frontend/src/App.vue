<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminShell from './components/AdminShell.vue'
import AppShell from './components/AppShell.vue'

const route = useRoute()

const layout = computed(() => route.meta.layout || 'user')
</script>

<template>
  <div class="app-root">
    <router-view v-if="layout === 'plain'" />
    <AdminShell v-else-if="layout === 'admin'">
      <router-view />
    </AdminShell>
    <AppShell v-else>
      <router-view />
    </AppShell>
  </div>
</template>
