import { beforeEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { createPinia, setActivePinia } from "pinia";
import { setupGuards } from "@/router/guards";
import { useAuthStore } from "@/stores/auth";

vi.mock("@/api/auth.api", () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}));

const Stub = { template: "<div />" };

const adminUser = {
  id: "u1",
  email: "admin@example.com",
  role: "ADMIN" as const,
  enabled: true,
  dailyLimit: 100,
  createdAt: "2026-01-01T00:00:00Z",
};

function createTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/login", name: "login", component: Stub },
      {
        path: "/history",
        name: "history",
        component: Stub,
        meta: { requiresAuth: true },
      },
      {
        path: "/admin/users",
        name: "admin-users",
        component: Stub,
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  });
  setupGuards(router);
  return router;
}

beforeEach(() => {
  setActivePinia(createPinia());
});

describe("router guard", () => {
  it("nepřihlášeného přesměruje na login a uloží redirect", async () => {
    const router = createTestRouter();
    await router.push("/history");

    expect(router.currentRoute.value.name).toBe("login");
    expect(router.currentRoute.value.query.redirect).toBe("/history");
  });

  it("přihlášeného uživatele z loginu přesměruje na historii", async () => {
    const auth = useAuthStore();
    auth.user = adminUser as typeof auth.user;

    const router = createTestRouter();
    await router.push("/login");

    expect(router.currentRoute.value.name).toBe("history");
  });

  it("běžného uživatele nepustí do administrace", async () => {
    const auth = useAuthStore();
    auth.user = {
      ...adminUser,
      role: "USER",
    } as typeof auth.user;

    const router = createTestRouter();
    await router.push("/admin/users");

    expect(router.currentRoute.value.name).toBe("history");
  });

  it("admina pustí do administrace", async () => {
    const auth = useAuthStore();
    auth.user = adminUser as typeof auth.user;

    const router = createTestRouter();
    await router.push("/admin/users");

    expect(router.currentRoute.value.name).toBe("admin-users");
  });
});
