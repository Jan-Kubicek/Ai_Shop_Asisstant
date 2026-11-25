import { ref } from "vue";

export interface CompressOptions {
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  mimeType?: string;
}

export function useImageCompress() {
  const compressing = ref(false);
  const error = ref<string | null>(null);

  async function compress(file: File, options: CompressOptions = {}): Promise<File> {
    const maxWidth = options.maxWidth ?? 1920;
    const maxHeight = options.maxHeight ?? 1920;
    const quality = options.quality ?? 0.85;
    const mimeType = options.mimeType ?? "image/jpeg";

    if (!file.type.startsWith("image/")) {
      return file;
    }

    compressing.value = true;
    error.value = null;

    try {
      const bitmap = await createImageBitmap(file);
      const scale = Math.min(1, maxWidth / bitmap.width, maxHeight / bitmap.height);

      if (scale === 1 && file.size <= 1_500_000) {
        return file;
      }

      const canvas = document.createElement("canvas");
      canvas.width = Math.round(bitmap.width * scale);
      canvas.height = Math.round(bitmap.height * scale);

      const context = canvas.getContext("2d");
      if (!context) {
        return file;
      }
      context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);

      const blob = await new Promise<Blob | null>((resolve) => {
        canvas.toBlob(resolve, mimeType, quality);
      });
      if (!blob) {
        return file;
      }

      const dot = file.name.lastIndexOf(".");
      const baseName = dot > 0 ? file.name.substring(0, dot) : file.name;
      return new File([blob], baseName + ".jpg", { type: mimeType });
    } catch {
      error.value = "Fotku se nepodařilo zmenšit, odešle se v původní podobě.";
      return file;
    } finally {
      compressing.value = false;
    }
  }

  return { compressing, error, compress };
}
