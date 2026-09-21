<template>
  <main class="app-shell">
    <AppHeader />
    <section class="portal-hero">
      <p class="eyebrow">{{ activeSystem.eyebrow }}</p>
      <h1>{{ activeSystem.title }}<em>{{ activeSystem.accent }}</em></h1>
      <p>{{ activeSystem.description }}</p>
      <div class="hero-metrics">
        <span v-for="metric in activeSystem.metrics" :key="metric.label"
          ><b>{{ metric.value }}</b> {{ metric.label }}</span
        >
      </div>
    </section>
    <section class="system-grid" aria-label="系统入口">
      <RouterLink
        class="system-card system-active"
        to="/landlord/puzzles"
        aria-label="进入斗地主残局实验室"
        @mouseenter="activeSystem = puzzleSystem"
        @mouseleave="resetActiveSystem"
        @focus="activeSystem = puzzleSystem"
      >
        <div class="system-card__top">
          <span class="card-symbol">♠</span
          ><StatusBadge tone="success">READY</StatusBadge>
        </div>
        <p class="eyebrow">PUZZLE LAB / CLASSIC_V1</p>
        <h2>斗地主残局实验室</h2>
        <p>配置残局、与机器人博弈，并得到严格验证的必胜策略。</p>
      </RouterLink>
      <a
        class="system-card system-external"
        :href="OPS_DESK_URL"
        target="_blank"
        rel="noopener noreferrer"
        aria-label="打开 OpsDesk"
        @mouseenter="activeSystem = opsDeskSystem"
        @mouseleave="resetActiveSystem"
        @focus="activeSystem = opsDeskSystem"
      >
        <div class="system-card__top">
          <span class="card-symbol">◈</span
          ><StatusBadge tone="info">EXTERNAL</StatusBadge>
        </div>
        <p class="eyebrow">OPERATIONS WORKBENCH</p>
        <h2>OpsDesk</h2>
        <p>独立运维工作台，使用其自身的登录和权限体系。</p>
      </a>
    </section>
  </main>
</template>
<script setup lang="ts">
import { ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { OPS_DESK_URL } from "../config/systems";

type PortalMetric = {
  value: string;
  label: string;
};

type PortalSystem = {
  eyebrow: string;
  title: string;
  accent: string;
  description: string;
  metrics: PortalMetric[];
};

const puzzleSystem: PortalSystem = {
  eyebrow: "PUZZLE LAB / CLASSIC_V1",
  title: "把每一次决策，",
  accent: "变成可验证的路径。",
  description: "双人明牌斗地主残局实验室，连接题库、对局与严格多分支证明。",
  metrics: [
    { value: "2", label: "独立对手" },
    { value: "100%", label: "完全明牌" },
    { value: "5", label: "条已验证路线" },
  ],
};

const opsDeskSystem: PortalSystem = {
  eyebrow: "OPERATIONS WORKBENCH",
  title: "让每一项运维，",
  accent: "保持清晰可追踪。",
  description: "独立运维工作台，使用自身的登录和权限体系管理工作事项。",
  metrics: [
    { value: "OPS", label: "运维工作台" },
    { value: "SSO", label: "独立权限体系" },
    { value: "HTTPS", label: "安全外链" },
  ],
};

const activeSystem = ref<PortalSystem>(puzzleSystem);

function resetActiveSystem(): void {
  activeSystem.value = puzzleSystem;
}
</script>
