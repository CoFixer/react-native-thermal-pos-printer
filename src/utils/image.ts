export async function fetchImageAsBase64(url: string): Promise<string> {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Failed to fetch image: ${res.status} ${res.statusText}`);
  }
  const buf = await res.arrayBuffer();
  const bytes = new Uint8Array(buf);
  let binary = '';
  const chunkSize = 0x8000; // 32KB
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const sub = bytes.subarray(i, i + chunkSize);
    binary += String.fromCharCode.apply(null, Array.from(sub) as any);
  }
  // Convert binary string to base64
  // In RN, global btoa may exist; otherwise, polyfill via Buffer if available
  // Use both strategies for portability
  // @ts-ignore
  if (typeof btoa === 'function') {
    // @ts-ignore
    return btoa(binary);
  }
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { Buffer } = require('buffer');
  return Buffer.from(bytes).toString('base64');
}

export function dataUrlToBase64(dataUrl: string): string {
  const comma = dataUrl.indexOf(',');
  if (comma === -1) return '';
  return dataUrl.substring(comma + 1);
}
