import { describe, expect, it } from "vitest";
import { formatBytes, formatDateTime } from "@/utils/format";

describe("formatBytes", () => {
  it("formátuje malé hodnoty v bajtech", () => {
    expect(formatBytes(512)).toBe("512 B");
  });

  it("formátuje kilobajty a megabajty", () => {
    expect(formatBytes(2048)).toBe("2.0 kB");
    expect(formatBytes(5 * 1024 * 1024)).toBe("5.0 MB");
  });
});

describe("formatDateTime", () => {
  it("vrací pomlčku pro prázdnou hodnotu", () => {
    expect(formatDateTime()).toBe("–");
  });
});
