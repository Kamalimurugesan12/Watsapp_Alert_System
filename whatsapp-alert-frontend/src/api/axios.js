// src/api/axios.js
// No JWT. No Authorization header. Just a base URL and timeout.
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// Response interceptor — handle "no DB connected" gracefully
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 503) {
      // Backend returns 503 when DynamicDataSourceManager has no active connection
      window.location.href = '/';   // redirect to Connection Screen
    }
    return Promise.reject(error);
  }
);

export default api;
