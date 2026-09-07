<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { clearOidcSession, isOidcAuthEnabled, isOidcConfigured, isSignedIn, startOidcLogin } from '../../auth/oidc'

const enabled = isOidcAuthEnabled()
const configured = isOidcConfigured()
const signedIn = ref(enabled && isSignedIn())
const busy = ref(false)
const errorMessage = ref('')

function sync() {
  signedIn.value = enabled && isSignedIn()
}

async function login() {
  errorMessage.value = ''
  busy.value = true
  try {
    await startOidcLogin()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录启动失败。'
    busy.value = false
  }
}

function logout() {
  clearOidcSession()
  sync()
}

onMounted(() => window.addEventListener('ticket-copilot:auth-change', sync))
onBeforeUnmount(() => window.removeEventListener('ticket-copilot:auth-change', sync))
</script>

<template>
  <div v-if="enabled" class="auth-control" aria-live="polite">
    <span v-if="!configured" class="auth-control__warning" title="请在部署环境中配置 OIDC issuer 与 client id">OIDC 未配置</span>
    <span v-else-if="signedIn" class="auth-control__signed-in"><span class="auth-control__dot" aria-hidden="true"></span>已登录</span>
    <button v-if="configured && !signedIn" type="button" class="auth-control__button" :disabled="busy" @click="login">{{ busy ? '跳转中…' : '企业登录' }}</button>
    <button v-else-if="configured && signedIn" type="button" class="auth-control__button auth-control__button--quiet" @click="logout">退出</button>
    <span v-if="errorMessage" class="auth-control__error" role="alert">{{ errorMessage }}</span>
  </div>
</template>

<style scoped>
.auth-control { display: inline-flex; align-items: center; gap: 8px; min-width: 0; }
.auth-control__button { min-height: 30px; border: 1px solid #0071e3; border-radius: 999px; background: #0071e3; padding: 0 11px; color: #fff; font-size: 11px; font-weight: 650; white-space: nowrap; }
.auth-control__button:disabled { cursor: wait; opacity: .55; }
.auth-control__button--quiet { border-color: #d2d2d7; background: #fff; color: #1d1d1f; }
.auth-control__signed-in, .auth-control__warning { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; white-space: nowrap; }
.auth-control__signed-in { color: #248a3d; }
.auth-control__warning { color: #a05a00; }
.auth-control__dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.auth-control__error { position: absolute; top: calc(100% + 6px); right: 12px; max-width: 280px; border: 1px solid #f0c1c6; border-radius: 9px; background: #fff8f8; padding: 7px 9px; color: #983b45; font-size: 10px; line-height: 1.4; white-space: normal; z-index: 3; }
@media (max-width: 960px) { .auth-control__signed-in, .auth-control__warning { display: none; } .auth-control__error { right: 0; } }
</style>
