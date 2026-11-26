import { CartPulseAPI } from './api.js';

/**
 * CartPulse Shared UI, Session, and Validation Utilities
 */
export const CartPulseUI = (() => {
  let currentUser = null;

  function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatCurrency(amount) {
    const num = Number(amount || 0);
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(num);
  }

  function formatDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  function formatDateTime(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.2s ease';
      setTimeout(() => toast.remove(), 220);
    }, 3400);
  }

  function setButtonLoading(button, isLoading, loadingText = 'Processing...') {
    if (!button) return;
    if (isLoading) {
      button.dataset.originalHtml = button.innerHTML;
      button.disabled = true;
      button.classList.add('btn-loading');
      button.innerHTML = `<span class="spinner"></span><span>${escapeHtml(loadingText)}</span>`;
    } else {
      button.disabled = false;
      button.classList.remove('btn-loading');
      if (button.dataset.originalHtml) {
        button.innerHTML = button.dataset.originalHtml;
      }
    }
  }

  function setFieldError(inputEl, message) {
    if (!inputEl) return;
    const group = inputEl.closest('.form-group');
    let feedback = group ? group.querySelector('.invalid-feedback') : null;
    if (!feedback && group) {
      feedback = document.createElement('div');
      feedback.className = 'invalid-feedback';
      group.appendChild(feedback);
    }
    if (message) {
      inputEl.classList.add('is-invalid');
      if (group) group.classList.add('has-error');
      if (feedback) feedback.textContent = message;
    } else {
      inputEl.classList.remove('is-invalid');
      if (group) group.classList.remove('has-error');
      if (feedback) feedback.textContent = '';
    }
  }

  function clearFormErrors(formEl) {
    if (!formEl) return;
    formEl.querySelectorAll('.is-invalid').forEach((el) => setFieldError(el, ''));
  }

  function applyFieldErrors(fieldErrors, fieldMap) {
    if (!fieldErrors) return;
    Object.entries(fieldErrors).forEach(([field, msg]) => {
      const inputId = fieldMap[field];
      if (inputId) {
        setFieldError(document.getElementById(inputId), msg);
      }
    });
  }

  function renderSkeletonCards(container, count = 6) {
    container.style.display = 'grid';
    container.innerHTML = Array.from({ length: count })
      .map(
        () => `
      <div class="card product-card">
        <div class="skeleton" style="height:180px;border-radius:0;"></div>
        <div class="product-body">
          <div class="skeleton" style="height:12px;width:45%;margin-bottom:10px;"></div>
          <div class="skeleton" style="height:18px;width:85%;margin-bottom:8px;"></div>
          <div class="skeleton" style="height:14px;width:60%;margin-bottom:18px;"></div>
          <div class="skeleton" style="height:34px;width:100%;"></div>
        </div>
      </div>`
      )
      .join('');
  }

  function renderStateBox(container, { title, message, isError = false, actionHtml = '' }) {
    container.innerHTML = `
      <div class="state-box ${isError ? 'error' : ''}">
        <div class="state-box-icon">
          ${
            isError
              ? `<svg width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 8v4m0 4h.01"/></svg>`
              : `<svg width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>`
          }
        </div>
        <h3>${escapeHtml(title)}</h3>
        <p class="text-muted">${escapeHtml(message)}</p>
        ${actionHtml}
      </div>`;
  }

  function getStatusBadge(status) {
    const map = {
      PENDING: { cls: 'badge-warning', label: 'Pending' },
      CONFIRMED: { cls: 'badge-info', label: 'Confirmed' },
      PACKED: { cls: 'badge-primary', label: 'Packed' },
      SHIPPED: { cls: 'badge-primary', label: 'Shipped' },
      DELIVERED: { cls: 'badge-success', label: 'Delivered' },
      CANCELLED: { cls: 'badge-danger', label: 'Cancelled' }
    };
    const info = map[status] || { cls: 'badge-muted', label: status };
    return `<span class="badge ${info.cls}">${escapeHtml(info.label)}</span>`;
  }

  function getStockBadge(stock, threshold = 5) {
    if (stock <= 0) {
      return `<span class="badge badge-danger">Out of Stock</span>`;
    }
    if (stock <= threshold) {
      return `<span class="badge badge-warning">Only ${stock} left</span>`;
    }
    return `<span class="badge badge-success">In Stock (${stock})</span>`;
  }

  function setCartBadgeCount(total) {
    document.querySelectorAll('[data-cart-badge]').forEach((el) => {
      el.textContent = total;
      el.classList.toggle('hidden', !total || total <= 0);
    });
  }

  async function refreshCartBadge() {
    if (!currentUser) {
      setCartBadgeCount(0);
      return;
    }
    try {
      const cart = await CartPulseAPI.cart.get();
      setCartBadgeCount(cart.totalItems || 0);
    } catch {
      setCartBadgeCount(0);
    }
  }

  async function initPage(activePage = 'store') {
    try {
      const res = await CartPulseAPI.auth.me();
      currentUser = res?.authenticated ? res.user : null;
    } catch {
      currentUser = null;
    }
    renderNavbar(activePage);
    renderFooter();
    await refreshCartBadge();
    return currentUser;
  }

  function getCurrentUser() {
    return currentUser;
  }

  function requireAuth(allowedRoles = null, containerEl = null) {
    if (!currentUser) {
      const target = encodeURIComponent(window.location.pathname + window.location.search);
      window.location.href = `login.html?redirect=${target}`;
      return false;
    }
    if (allowedRoles && !allowedRoles.includes(currentUser.role)) {
      if (containerEl) {
        renderStateBox(containerEl, {
          title: 'Access Restricted',
          message: `This page requires ${allowedRoles.join(' or ')} privileges. You are signed in as ${currentUser.role}.`,
          isError: true,
          actionHtml: `<a href="index.html" class="btn btn-primary">Return to Storefront</a>`
        });
      }
      return false;
    }
    return true;
  }

  function renderNavbar(activePage = 'store') {
    const banner = document.getElementById('preview-banner');
    if (banner) banner.remove();

    const navHost = document.getElementById('site-navbar');
    if (!navHost) return;

    const user = currentUser;
    const role = user?.role || 'GUEST';

    const roleLinks = [];
    roleLinks.push(`<a href="index.html" class="nav-link ${activePage === 'store' ? 'active' : ''}">Storefront</a>`);
    if (user) {
      roleLinks.push(`<a href="orders.html" class="nav-link ${activePage === 'orders' ? 'active' : ''}">My Orders</a>`);
    }
    if (role === 'SELLER') {
      roleLinks.push(`<a href="seller-dashboard.html" class="nav-link ${activePage === 'seller' ? 'active' : ''}">Seller Portal</a>`);
    }
    if (role === 'ADMIN') {
      roleLinks.push(`<a href="seller-dashboard.html" class="nav-link ${activePage === 'seller' ? 'active' : ''}">Seller Portal</a>`);
      roleLinks.push(`<a href="admin-dashboard.html" class="nav-link ${activePage === 'admin' ? 'active' : ''}">Admin Panel</a>`);
    }

    const authSection = user
      ? `
        <span class="badge badge-primary">${escapeHtml(user.role)}</span>
        <span class="fw-600" style="font-size:0.88rem;">${escapeHtml(user.fullName)}</span>
        <button type="button" class="btn btn-secondary btn-sm" id="logout-btn">Logout</button>
      `
      : `
        <a href="login.html" class="btn btn-ghost btn-sm">Sign In</a>
        <a href="register.html" class="btn btn-primary btn-sm">Create Account</a>
      `;

    navHost.innerHTML = `
      <header class="navbar">
        <div class="container navbar-inner">
          <a href="index.html" class="brand">
            <span class="brand-mark">
              <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" viewBox="0 0 24 24">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <path d="M16 10a4 4 0 0 1-8 0"/>
              </svg>
            </span>
            <span>CartPulse</span>
          </a>

          <form class="nav-search" id="global-search-form" role="search">
            <svg class="nav-search-icon" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input type="search" id="global-search-input" placeholder="Search electronics, desk gear, books..." aria-label="Search products">
          </form>

          <nav class="nav-links" aria-label="Primary Navigation">
            ${roleLinks.join('')}
            <a href="cart.html" class="nav-link cart-pill ${activePage === 'cart' ? 'active' : ''}">
              <span>Cart</span>
              <span class="cart-badge hidden" data-cart-badge>0</span>
            </a>
          </nav>

          <div class="nav-actions">
            ${authSection}
          </div>

          <button type="button" class="mobile-menu-btn" id="mobile-menu-toggle" aria-label="Toggle navigation menu">
            <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
              <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
        </div>
      </header>
      <div class="mobile-drawer" id="mobile-drawer">
        ${roleLinks.join('')}
        <a href="cart.html" class="nav-link">Cart <span class="cart-badge hidden" data-cart-badge>0</span></a>
        <div class="flex-gap mt-1">${authSection}</div>
      </div>
    `;

    const toggleBtn = document.getElementById('mobile-menu-toggle');
    const drawer = document.getElementById('mobile-drawer');
    if (toggleBtn && drawer) {
      toggleBtn.addEventListener('click', () => drawer.classList.toggle('open'));
    }

    const searchForm = document.getElementById('global-search-form');
    if (searchForm) {
      searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const q = document.getElementById('global-search-input').value.trim();
        if (window.location.pathname.endsWith('index.html') || window.location.pathname === '/') {
          window.dispatchEvent(new CustomEvent('cartpulse:search', { detail: q }));
        } else {
          window.location.href = `index.html?q=${encodeURIComponent(q)}`;
        }
      });
    }

    document.querySelectorAll('#logout-btn').forEach((btn) => {
      btn.addEventListener('click', async () => {
        try {
          await CartPulseAPI.auth.logout();
        } catch {
          // ignore network errors on logout
        }
        currentUser = null;
        showToast('Signed out successfully');
        setTimeout(() => {
          window.location.href = 'index.html';
        }, 350);
      });
    });
  }

  function renderFooter() {
    const footerHost = document.getElementById('site-footer');
    if (!footerHost) return;
    footerHost.innerHTML = `
      <footer class="footer">
        <div class="container footer-inner">
          <div>
            <strong>CartPulse</strong> — Smart Multi-Vendor E-Commerce &amp; Inventory Analytics
          </div>
          <div class="flex-gap">
            <a href="index.html">Catalog</a>
            <a href="orders.html">Orders</a>
            <a href="seller-dashboard.html">Seller Portal</a>
            <a href="admin-dashboard.html">Admin Panel</a>
          </div>
        </div>
      </footer>
    `;
  }

  return {
    escapeHtml,
    formatCurrency,
    formatDate,
    formatDateTime,
    showToast,
    setButtonLoading,
    setFieldError,
    clearFormErrors,
    applyFieldErrors,
    renderSkeletonCards,
    renderStateBox,
    getStatusBadge,
    getStockBadge,
    setCartBadgeCount,
    refreshCartBadge,
    initPage,
    getCurrentUser,
    requireAuth,
    renderNavbar,
    renderFooter
  };
})();
