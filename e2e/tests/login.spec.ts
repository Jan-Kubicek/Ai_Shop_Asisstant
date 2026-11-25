import { expect, test } from "@playwright/test";

test.describe("Přihlášení", () => {
  test("nepřihlášený uživatel je přesměrován na přihlášení", async ({ page }) => {
    await page.goto("/history");
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByTestId("login-form")).toBeVisible();
  });

  test("admin se přihlásí a vidí historii", async ({ page }) => {
    const email = process.env.E2E_ADMIN_EMAIL;
    const password = process.env.E2E_ADMIN_PASSWORD;
    test.skip(!email || !password, "Chybí E2E_ADMIN_EMAIL nebo E2E_ADMIN_PASSWORD.");

    await page.goto("/login");
    await page.getByLabel("E-mail").fill(email as string);
    await page.getByLabel("Heslo").fill(password as string);
    await page.getByRole("button", { name: "Přihlásit se" }).click();

    await expect(page).toHaveURL(/\/history/);
    await expect(page.getByTestId("history-view")).toBeVisible();
  });
});
