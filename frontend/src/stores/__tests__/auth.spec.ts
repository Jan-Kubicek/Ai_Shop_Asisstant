import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/api/auth.api";
import { getAccessToken, setAccessToken } from "@/api/http";
import type { User } from "@/types";

vi.mock("@/api/auth.api", () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  },
}));

const adminUser: User = {
  id: "u1",
  email: "admin@example.com",
  role: "ADMIN",
  enabled: true,
  dailyLimit: 100,
  createdAt: "2026-01-01T00:00:00Z",
};

beforeEach(() => {
  setActivePinia(createPinia());
  setAccessToken(null);
  vi.clearAllMocks();
});

describe("store auth", () => {
  it("login nastaví uživatele, roli a token", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "access-123",
      expiresIn: 900,
      user: adminUser,
    });

    const store = useAuthStore();
    await store.login("admin@example.com", "tajne");

    expect(store.user).toEqual(adminUser);
    expect(store.isAuthenticated).toBe(true);
    expect(store.isAdmin).toBe(true);
    expect(getAccessToken()).toBe("access-123");
    expect(store.loading).toBe(false);
  });

  it("login při chybě ponechá store prázdný", async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error("Neplatné přihlášení."));

    const store = useAuthStore();
    await expect(store.login("admin@example.com", "spatne")).rejects.toThrow(
      "Neplatné přihlášení.",
    );

    expect(store.user).toBeNull();
    expect(store.isAuthenticated).toBe(false);
    expect(getAccessToken()).toBeNull();
  });

  it("bootstrap obnoví session", async () => {
    vi.mocked(authApi.refresh).mockResolvedValue({
      accessToken: "refreshed",
      expiresIn: 900,
      user: adminUser,
    });

    const store = useAuthStore();
    await store.bootstrap();

    expect(store.user).toEqual(adminUser);
    expect(store.initialized).toBe(true);
    expect(getAccessToken()).toBe("refreshed");
  });

  it("bootstrap při chybě vyčistí session", async () => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error("expired"));

    const store = useAuthStore();
    await store.bootstrap();

    expect(store.user).toBeNull();
    expect(store.initialized).toBe(true);
    expect(getAccessToken()).toBeNull();
  });

  it("logout zavolá API a vyčistí store i při chybě", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: "token",
      expiresIn: 900,
      user: adminUser,
    });
    const store = useAuthStore();
    await store.login("admin@example.com", "tajne");

    vi.mocked(authApi.logout).mockRejectedValue(new Error("network"));
    await expect(store.logout()).rejects.toThrow("network");

    expect(store.user).toBeNull();
    expect(getAccessToken()).toBeNull();
  });
});
