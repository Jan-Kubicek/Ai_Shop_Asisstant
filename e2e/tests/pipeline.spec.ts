import { expect, test } from "@playwright/test";

const PNG_1X1 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

test.describe("Pipeline s mockovanou AI", () => {
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

  test("požadavek projde pipeline, výsledek jde upravit, schválit a stáhnout", async ({ page }) => {
    await page.getByTestId("new-request-link").click();
    await page.getByTestId("photo-input").setInputFiles({
      name: "test.png",
      mimeType: "image/png",
      buffer: Buffer.from(PNG_1X1, "base64"),
    });
    await page.locator("#description").fill("Testovací vlněná čepice pro E2E pipeline.");
    await page.getByRole("button", { name: "Vytvořit" }).click();

    await expect(page).toHaveURL(/\/requests\/[a-f0-9]+/);
    await expect(page.getByTestId("request-status")).toContainText("Ke kontrole", {
      timeout: 30000,
    });

    // AI běží s fake klientem – výsledek musí být k dispozici.
    await expect(page.getByTestId("result-ready")).toBeVisible();
    await expect(page.getByText("Ručně pletená čepice, šedá")).toBeVisible();

    // Úprava výsledku.
    await page.getByTestId("edit-result").click();
    await page.locator("#edit-long").fill("Upravený popis produktu pro E2E kontrolu.");
    await page.getByTestId("save-result").click();
    await expect(
      page.getByTestId("result-ready").getByText("Upravený popis produktu pro E2E kontrolu."),
    ).toBeVisible();

    // Schválení přesune požadavek do fronty e-shopu.
    await page.getByTestId("approve-request").click();
    await expect(page.getByTestId("request-status")).toContainText("Ve frontě e-shopu");

    // Stažení ZIPu.
    const downloadPromise = page.waitForEvent("download");
    await page.getByTestId("download-request").click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toMatch(/\.zip$/);
  });
});
