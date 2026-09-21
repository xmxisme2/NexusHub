import { request, refreshCsrf } from "../http";

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  role: "GUEST" | "USER" | "ADMIN";
  status: "ENABLED" | "DISABLED";
  rowVersion: number;
  permissions: string[];
};

export async function login(username: string, password: string) {
  await refreshCsrf();
  return request<CurrentUser>({
    method: "POST",
    url: "/auth/login",
    data: { username, password },
  });
}
export async function getCurrentUser() {
  await refreshCsrf();
  return request<CurrentUser>({ method: "POST", url: "/auth/me", data: {} });
}
export function logout() {
  return request<Record<string, never>>({
    method: "POST",
    url: "/auth/logout",
    data: {},
  });
}
export function createGuest() {
  return request<CurrentUser>({ method: "POST", url: "/auth/guest", data: {} });
}
