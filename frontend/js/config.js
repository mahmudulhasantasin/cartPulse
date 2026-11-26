/**
 * Environment-aware Backend API URL configuration.
 * - Localhost: points to local Spring Boot on port 8081 (or same-origin when served by Spring Boot).
 * - Production (Vercel): set RENDER_API_URL below to your deployed Render Web Service URL.
 */
const RENDER_API_URL = 'https://cartpulse-api.onrender.com';

const API_OVERRIDE_KEY = 'cartpulse_api_url';
const params = new URLSearchParams(window.location.search);
if (params.has('api')) {
  const custom = params.get('api').trim().replace(/\/+$/, '');
  if (custom) {
    localStorage.setItem(API_OVERRIDE_KEY, custom);
  } else {
    localStorage.removeItem(API_OVERRIDE_KEY);
  }
}

function resolveApiBaseUrl() {
  const override = localStorage.getItem(API_OVERRIDE_KEY);
  if (override) return override;

  const host = window.location.hostname;
  if (host === 'localhost' || host === '127.0.0.1') {
    return window.location.port === '8081' ? '' : 'http://localhost:8081';
  }
  return RENDER_API_URL.replace(/\/+$/, '');
}

export const API_BASE_URL = resolveApiBaseUrl();
