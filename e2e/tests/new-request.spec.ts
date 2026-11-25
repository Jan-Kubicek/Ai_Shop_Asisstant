import { expect, test } from "@playwright/test";

const PNG_1X1 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

test.describe("Nový požadavek", () => {
  test.beforeEach(async ({ page }) => {
    const email = process.env.E2E_USER_EMAIL;
    const password = process.env.E2E_USER_PASSWORD;
    test.skip(!email || !password, "Chybí E2E_USER_EMAIL nebo E2E_USER_PASSWORD.");

    await page.goto("/login");
    await page.getByLabel("E-mail").fill(email as string);
    await page.getByLabel("Heslo").fill(password as string);
    await page.getByRole("button", { name: "Přihlásit se" }).click();
    await expect(page).toHaveURL(/\/history/);
  });

  test("uživatel odešle fotku a popis", async ({ page }) => {
    await page.getByTestId("new-request-link").click();
    await expect(page).toHaveURL(/\/requests\/new/);

    await page.getByTestId("photo-input").setInputFiles({
      name: "test.png",
      mimeType: "image/png",
      buffer: Buffer.from(PNG_1X1, "base64"),
    });
    await page.locator("#description").fill("Testovací produkt pro E2E.");
    await page.getByRole("button", { name: "Vytvořit" }).click();

    await expect(page).toHaveURL(/\/requests\/[a-f0-9]+/);
    await expect(page.getByTestId("request-status")).toBeVisible();
  });
});
