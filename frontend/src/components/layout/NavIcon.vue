<script setup lang="ts">
defineProps<{
  name: string
}>()
</script>

<template>
  <svg class="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.55" stroke-linecap="round" stroke-linejoin="round" focusable="false" aria-hidden="true">
    <path v-if="name === 'overview'" d="M3.75 8.25 10 3l6.25 5.25v7.5H3.75v-7.5Z" />
    <path v-if="name === 'overview'" d="M7.35 15.75v-4.4h5.3v4.4" />

    <path v-else-if="name === 'workbench'" d="M4 4.25h12v3.2H4zM4 8.4h12v3.2H4zM4 12.55h8.2v3.2H4z" />
    <path v-else-if="name === 'knowledge'" d="M4.2 4.15h7.15a2.45 2.45 0 0 1 2.45 2.45v9.25H6.7a2.5 2.5 0 0 0-2.5 2.5V4.15Z" />
    <path v-else-if="name === 'knowledge'" d="M13.8 6.6h2v11.75H6.7a2.5 2.5 0 0 1 2.5-2.5h4.6V6.6Z" />

    <path v-else-if="name === 'evidence'" d="M6.65 6.9 4.5 9.05a2.75 2.75 0 0 0 3.9 3.9l2.15-2.15M13.35 13.1l2.15-2.15a2.75 2.75 0 1 0-3.9-3.9L9.45 9.2" />
    <path v-else-if="name === 'evidence'" d="m7.7 12.3 4.6-4.6" />

    <path v-else-if="name === 'trace'" d="M5 4.25v11.5M5 5.5h7.8a2.2 2.2 0 0 1 0 4.4H5M5 10.9h8.4a2.2 2.2 0 0 1 0 4.4H5" />
    <path v-else-if="name === 'review'" d="m4.2 10.2 3.35 3.35 8.25-8.25" />
    <circle v-else-if="name === 'review'" cx="10" cy="10" r="7.15" />

    <path v-else-if="name === 'evaluation'" d="M4 15.75V10.9M8 15.75V7.9M12 15.75V11.5M16 15.75V4.75" />
    <path v-else d="M4 10h12M10 4v12" />
  </svg>
</template>

<style scoped>
.nav-icon {
  display: block;
  width: 15px;
  height: 15px;
}
</style>
