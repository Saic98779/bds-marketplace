import axios from 'axios';

const STORAGE_KEY = 'bds.session';

export const session = {
  get() {
    try {
      return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) || null;
    } catch {
      return null;
    }
  },
  set(value) {
    if (value) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
    window.dispatchEvent(new Event('bds-session'));
  }
};

const http = axios.create({ baseURL: '/api/v1', timeout: 30000 });

http.interceptors.request.use((config) => {
  const current = session.get();
  if (current?.accessToken) {
    config.headers.Authorization = `Bearer ${current.accessToken}`;
  }
  return config;
});

let refreshing = null;

async function refreshTokens() {
  const current = session.get();
  if (!current?.refreshToken) {
    throw new Error('No session');
  }
  const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken: current.refreshToken });
  const tokens = data.data;
  session.set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken, user: tokens.user });
  return tokens.accessToken;
}

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (status === 401 && !original._retry && !isAuthCall && session.get()?.refreshToken) {
      original._retry = true;
      try {
        refreshing = refreshing || refreshTokens();
        const token = await refreshing;
        refreshing = null;
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      } catch {
        refreshing = null;
        session.set(null);
        window.location.assign('/login?expired=1');
      }
    }
    return Promise.reject(error);
  }
);

export function errorMessage(error) {
  const body = error?.response?.data;
  if (body?.errors?.length) {
    return body.errors.map((e) => (e.field ? `${humanize(e.field)}: ${e.message}` : e.message)).join(' · ');
  }
  if (body?.message) {
    return body.message;
  }
  if (error?.code === 'ECONNABORTED') {
    return 'The request timed out. Please try again.';
  }
  if (!error?.response) {
    return 'Unable to reach BDS Marketplace. Check your connection.';
  }
  return 'Something went wrong. Please try again.';
}

export function fieldErrors(error) {
  const out = {};
  (error?.response?.data?.errors || []).forEach((e) => {
    if (e.field) out[e.field] = e.message;
  });
  return out;
}

function humanize(field) {
  return field.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
}

function clean(params) {
  if (!params) return undefined;
  const out = {};
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  });
  return out;
}

export const api = {
  async get(url, params) {
    const { data } = await http.get(url, { params: clean(params) });
    return data.data;
  },
  async post(url, body) {
    const { data } = await http.post(url, body ?? {});
    return data;
  },
  async put(url, body) {
    const { data } = await http.put(url, body ?? {});
    return data;
  },
  async patch(url, body) {
    const { data } = await http.patch(url, body ?? {});
    return data;
  },
  async del(url) {
    const { data } = await http.delete(url);
    return data;
  },
  async upload(url, fields) {
    const form = new FormData();
    Object.entries(fields).forEach(([k, v]) => {
      if (v === undefined || v === null) return;
      if (Array.isArray(v)) {
        v.forEach((item) => form.append(k, item));
      } else if (v instanceof Blob) {
        form.append(k, v);
      } else if (typeof v === 'object') {
        form.append(k, new Blob([JSON.stringify(v)], { type: 'application/json' }));
      } else {
        form.append(k, v);
      }
    });
    const { data } = await http.post(url, form);
    return data;
  },
  async download(url, params, fallbackName = 'download') {
    const response = await http.get(url, { params: clean(params), responseType: 'blob' });
    const disposition = response.headers['content-disposition'] || '';
    const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
    const name = match ? decodeURIComponent(match[1]) : fallbackName;
    const href = URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = href;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(href), 1000);
  }
};

export default http;
