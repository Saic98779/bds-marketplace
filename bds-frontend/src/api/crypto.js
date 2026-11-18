import axios from 'axios';

let keyPromise = null;

function base64ToBuffer(b64) {
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

function bufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  bytes.forEach((b) => {
    binary += String.fromCharCode(b);
  });
  return btoa(binary);
}

async function publicKey() {
  if (!keyPromise) {
    keyPromise = axios.get('/api/v1/auth/public-key').then(({ data }) =>
      window.crypto.subtle.importKey('spki', base64ToBuffer(data.data.publicKey), { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt'])
    ).catch((e) => {
      keyPromise = null;
      throw e;
    });
  }
  return keyPromise;
}

export async function protect(value) {
  if (!value || !window.crypto?.subtle) return value;
  try {
    const key = await publicKey();
    const encrypted = await window.crypto.subtle.encrypt({ name: 'RSA-OAEP' }, key, new TextEncoder().encode(value));
    return `rsa:${bufferToBase64(encrypted)}`;
  } catch {
    keyPromise = null;
    return value;
  }
}
