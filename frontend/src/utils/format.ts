export function formatDateTime(value?: string): string {
  if (!value) {
    return "–";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("cs-CZ", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return bytes + " B";
  }
  const units = ["kB", "MB", "GB"];
  let value = bytes / 1024;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return value.toFixed(1) + " " + units[unit];
}
