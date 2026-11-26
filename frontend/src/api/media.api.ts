import { requestBlob } from "./http";

export const mediaApi = {
  download(id: string): Promise<Blob> {
    return requestBlob("/media/" + id);
  },
};
