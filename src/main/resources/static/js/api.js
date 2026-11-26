/**
 * Centralized REST API client for CartPulse Spring Boot backend.
 * Uses same-origin session cookies and standard JSON error parsing.
 */
export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, options = {}) {
  const config = {
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  };

  if (config.body && typeof config.body !== 'string') {
    config.body = JSON.stringify(config.body);
  }

  let response;
  try {
    response = await fetch(path, config);
  } catch {
    throw new ApiError(0, 'Unable to reach the server. Make sure the Spring Boot application is running.');
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const message = data?.message || `Request failed (${response.status})`;
    const fieldErrors = data?.fieldErrors || {};
    throw new ApiError(response.status, message, fieldErrors);
  }

  return data;
}

export const CartPulseAPI = {
  auth: {
    me: () => request('/api/auth/me'),
    login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),
    register: (payload) => request('/api/auth/register', { method: 'POST', body: payload }),
    logout: () => request('/api/auth/logout', { method: 'POST' })
  },
  catalog: {
    categories: () => request('/api/categories'),
    products: (params = {}) => {
      const qs = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== '' && v !== null && v !== undefined && v !== false) {
          qs.set(k, String(v));
        }
      });
      return request(`/api/products?${qs.toString()}`);
    },
    product: (id) => request(`/api/products/${id}`)
  },
  reviews: {
    list: (productId) => request(`/api/products/${productId}/reviews`),
    create: (productId, payload) => request(`/api/products/${productId}/reviews`, { method: 'POST', body: payload })
  },
  cart: {
    get: () => request('/api/cart'),
    addItem: (productId, quantity = 1) => request('/api/cart/items', { method: 'POST', body: { productId, quantity } }),
    updateItem: (productId, quantity) => request(`/api/cart/items/${productId}`, { method: 'PUT', body: { quantity } }),
    removeItem: (productId) => request(`/api/cart/items/${productId}`, { method: 'DELETE' }),
    clear: () => request('/api/cart', { method: 'DELETE' }),
    previewPromo: (code) => request(`/api/cart/promo-preview?code=${encodeURIComponent(code)}`)
  },
  orders: {
    myOrders: () => request('/api/orders'),
    checkout: (payload) => request('/api/orders/checkout', { method: 'POST', body: payload }),
    cancel: (orderId) => request(`/api/orders/${orderId}/cancel`, { method: 'POST' })
  },
  seller: {
    analytics: () => request('/api/seller/analytics'),
    products: () => request('/api/seller/products'),
    lowStock: () => request('/api/seller/products/low-stock'),
    createProduct: (payload) => request('/api/seller/products', { method: 'POST', body: payload }),
    updateProduct: (id, payload) => request(`/api/seller/products/${id}`, { method: 'PUT', body: payload }),
    restock: (id, amount) => request(`/api/seller/products/${id}/restock`, { method: 'POST', body: { amount } }),
    toggleProduct: (id) => request(`/api/seller/products/${id}/toggle`, { method: 'PATCH' }),
    inventoryLogs: (id) => request(`/api/seller/products/${id}/inventory-logs`),
    orders: () => request('/api/seller/orders'),
    updateOrderStatus: (id, status) => request(`/api/seller/orders/${id}/status`, { method: 'PATCH', body: { status } })
  },
  admin: {
    analytics: () => request('/api/admin/analytics'),
    createCategory: (payload) => request('/api/admin/categories', { method: 'POST', body: payload }),
    promos: () => request('/api/admin/promos'),
    createPromo: (payload) => request('/api/admin/promos', { method: 'POST', body: payload }),
    togglePromo: (id) => request(`/api/admin/promos/${id}/toggle`, { method: 'PATCH' }),
    users: () => request('/api/admin/users'),
    toggleUser: (id) => request(`/api/admin/users/${id}/toggle`, { method: 'PATCH' })
  }
};
