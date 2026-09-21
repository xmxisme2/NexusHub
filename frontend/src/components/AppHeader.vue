<template>
  <header class="app-header">
    <RouterLink class="brand" to="/" aria-label="NexusHub 首页"
      >NEXUS<span>HUB</span></RouterLink
    >
    <nav aria-label="主导航">
      <RouterLink to="/">门户</RouterLink
      ><RouterLink to="/landlord/puzzles">残局库</RouterLink>
    </nav>
    <div class="header-actions">
      <span class="environment">CLASSIC_V1</span
      ><RouterLink class="profile-link" to="/login">{{
        session.user?.role === "GUEST"
          ? `游客 #${session.user.id}`
          : session.user?.displayName || "登录"
      }}</RouterLink>
    </div>
  </header>
</template>
<script setup lang="ts">
import { useSessionStore } from "../stores/session";
const session = useSessionStore();
</script>
