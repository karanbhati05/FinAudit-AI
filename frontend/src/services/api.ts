import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

// In-memory token storage (NOT stored in localStorage as per security specification)
let inMemoryToken: string | null = null;

export const setAuthToken = (token: string | null) => {
  inMemoryToken = token;
};

export const getAuthToken = (): string | null => {
  return inMemoryToken;
};

const rawBaseUrl = (import.meta.env.VITE_API_URL || '/api').trim();
const normalizedBaseUrl = rawBaseUrl.endsWith('/api')
  ? rawBaseUrl
  : (rawBaseUrl.endsWith('/') ? `${rawBaseUrl}api` : (rawBaseUrl === '' ? '/api' : `${rawBaseUrl}/api`));

export const api = axios.create({
  baseURL: normalizedBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor attaches in-memory JWT
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (inMemoryToken && config.headers) {
      config.headers.Authorization = `Bearer ${inMemoryToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor handles auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Invalidate in-memory token on 401 Unauthorized
      inMemoryToken = null;
    }
    return Promise.reject(error);
  }
);
