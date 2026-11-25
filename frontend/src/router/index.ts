import { createRouter, createWebHistory } from "vue-router";
import { setupGuards } from "./guards";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: "/history",
    },
    {
      path: "/login",
      name: "login",
      component: () => import("@/views/LoginView.vue"),
    },
    {
      path: "/history",
      name: "history",
      component: () => import("@/views/HistoryView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/requests/new",
      name: "request-new",
      component: () => import("@/views/NewRequestView.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/requests/:id",
      name: "request-detail",
      component: () => import("@/views/RequestDetailView.vue"),
      meta: { requiresAuth: true },
      props: true,
    },
    {
      path: "/admin/shop-queue",
      name: "admin-shop-queue",
      component: () => import("@/views/admin/ShopQueueView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: "/admin/users",
      name: "admin-users",
      component: () => import("@/views/admin/UsersView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: "/admin/logs",
      name: "admin-logs",
      component: () => import("@/views/admin/LogsView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: "/admin/stats",
      name: "admin-stats",
      component: () => import("@/views/admin/StatsView.vue"),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: () => import("@/views/NotFoundView.vue"),
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

setupGuards(router);
