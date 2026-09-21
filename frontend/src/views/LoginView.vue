<template>
  <main class="login-page">
    <section class="login-brand">
      <RouterLink class="brand" to="/">NEXUS<span>HUB</span></RouterLink>
      <p class="eyebrow">STRATEGY SYSTEM / ACCESS</p>
      <h1>在清晰的局面中，<em>验证每一步。</em></h1>
      <p>登录后可以管理你的残局、保存版本并查看策略分析任务。</p>
      <div class="login-points">
        <span>双人独立对手</span><span>完全明牌</span><span>多分支证明</span>
      </div>
    </section>
    <section class="login-card">
      <p class="eyebrow">WELCOME BACK</p>
      <h2>登录 NexusHub</h2>
      <p class="login-hint">使用系统管理员为你创建的账号。</p>
      <form @submit.prevent="submit">
        <label
          >用户名<input
            v-model.trim="username"
            autocomplete="username"
            required
            minlength="3"
            placeholder="例如 admin" /></label
        ><label
          >密码<input
            v-model="password"
            type="password"
            autocomplete="current-password"
            required
            placeholder="输入密码"
        /></label>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="button login-button" :disabled="submitting">
          {{ submitting ? "正在登录…" : "登录" }}
        </button>
      </form>
      <p class="session-note">会话空闲 30 分钟后需要重新登录。</p>
    </section>
  </main>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ApiError } from "../api/http";
import { useSessionStore } from "../stores/session";
const username = ref("");
const password = ref("");
const error = ref("");
const submitting = ref(false);
const router = useRouter();
const route = useRoute();
const session = useSessionStore();
async function submit() {
  submitting.value = true;
  error.value = "";
  try {
    await session.signIn(username.value, password.value);
    const redirect =
      typeof route.query.redirect === "string" ? route.query.redirect : "/";
    await router.push(redirect);
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : "登录暂时不可用。";
  } finally {
    submitting.value = false;
  }
}
</script>
