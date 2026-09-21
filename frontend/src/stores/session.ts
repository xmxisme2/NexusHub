import { defineStore } from "pinia";
import {
  getCurrentUser,
  createGuest,
  login,
  logout,
  type CurrentUser,
} from "../api/modules/auth";

export const useSessionStore = defineStore("session", {
  state: () => ({ user: null as CurrentUser | null, checked: false }),
  actions: {
    async restore() {
      try {
        this.user = await getCurrentUser();
      } catch {
        try {
          this.user = await createGuest();
        } catch {
          this.user = null;
        }
      } finally {
        this.checked = true;
      }
    },
    async signIn(username: string, password: string) {
      this.user = await login(username, password);
    },
    async signOut() {
      try {
        await logout();
      } finally {
        this.user = null;
      }
    },
  },
});
