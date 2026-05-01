<template>
  <div class="app-container">
    <router-view v-slot="{ Component, route }">
      <transition
        :name="route.meta.animation || 'page-fade'"
        mode="out-in"
      >
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
    <forum-entry-loader
      :visible="entryTransitionLoaderState.visible"
      :kicker="entryTransitionLoaderState.kicker"
      :title="entryTransitionLoaderState.title"
      :subtitle="entryTransitionLoaderState.subtitle"
      :progress="entryTransitionLoaderState.progress"
      :status-text="entryTransitionLoaderState.statusText"
    />
  </div>
</template>

<script setup>
import ForumEntryLoader from '@/components/ForumEntryLoader.vue'
import { entryTransitionLoaderState } from '@/stores/entryLoader'
</script>

<style scoped src="./styles/App.css"></style>
