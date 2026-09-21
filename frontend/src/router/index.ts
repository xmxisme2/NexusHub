import { createRouter, createWebHistory } from "vue-router";
import PortalHome from "../views/PortalHome.vue";
import PuzzleLibrary from "../views/PuzzleLibrary.vue";
import PuzzleEditor from "../views/PuzzleEditor.vue";
import LoginView from "../views/LoginView.vue";
import GameTable from "../views/GameTable.vue";
import ReplayView from "../views/ReplayView.vue";
import PuzzleConfigView from "../views/PuzzleConfigView.vue";
import PuzzleStrategyView from "../views/PuzzleStrategyView.vue";
import { useSessionStore } from "../stores/session";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: PortalHome, meta: { title: "系统门户" } },
    { path: "/login", component: LoginView, meta: { title: "登录" } },
    {
      path: "/landlord/puzzles",
      component: PuzzleLibrary,
      meta: { title: "残局库" },
    },
    {
      path: "/landlord/puzzles/new",
      component: PuzzleConfigView,
      meta: { title: "新建残局", requiresAuth: true },
    },
    {
      path: "/landlord/puzzles/:id/edit",
      component: PuzzleConfigView,
      props: true,
      meta: { title: "编辑残局", requiresAuth: true },
    },
    {
      path: "/landlord/puzzles/:id/strategy",
      component: PuzzleStrategyView,
      props: true,
      meta: { title: "已证明策略" },
    },
    {
      path: "/landlord/puzzles/:id",
      component: PuzzleEditor,
      props: true,
      meta: { title: "残局详情" },
    },
    {
      path: "/landlord/games/:id",
      component: GameTable,
      props: true,
      meta: { title: "明牌对局" },
    },
    {
      path: "/landlord/games/:id/replay",
      component: ReplayView,
      props: true,
      meta: { title: "对局复盘" },
    },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});

router.afterEach((to) => {
  document.title = `${to.meta.title || "NexusHub"} · NexusHub`;
});

router.beforeEach(async (to) => {
  const session = useSessionStore();
  if (to.path !== "/login" && !session.checked) await session.restore();
  if (to.meta.requiresAuth) {
    if (!session.user || session.user.role === "GUEST")
      return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && session.user && session.user.role !== "GUEST")
    return { path: "/" };
  return true;
});
