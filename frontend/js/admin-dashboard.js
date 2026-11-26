import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

let analytics = null;
let promos = [];
let categories = [];
let users = [];

let volumeChart = null;
let statusChart = null;

async function init() {
  await CartPulseUI.initPage('admin');
  const mainEl = document.querySelector('main .container');
  if (!CartPulseUI.requireAuth(['ADMIN'], mainEl)) return;

  bindTabs();
  bindForms();
  await loadAdminData();
}

async function loadAdminData() {
  try {
    const [analyticsData, promoList, catList, userList] = await Promise.all([
      CartPulseAPI.admin.analytics(),
      CartPulseAPI.admin.promos(),
      CartPulseAPI.catalog.categories(),
      CartPulseAPI.admin.users()
    ]);

    analytics = analyticsData;
    promos = promoList;
    categories = catList;
    users = userList;

    renderAll();
    renderCharts();
  } catch (err) {
    CartPulseUI.showToast(err.message || 'Failed to load admin data', 'error');
  }
}

function bindTabs() {
  const btns = document.querySelectorAll('[data-admin-tab]');
  btns.forEach((btn) => {
    btn.addEventListener('click', () => {
      btns.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      ['analytics', 'promos', 'categories', 'users'].forEach((t) => {
        document.getElementById(`admin-tab-${t}`).classList.toggle('hidden', t !== btn.dataset.adminTab);
      });
    });
  });
}

function bindForms() {
  const promoForm = document.getElementById('admin-promo-form');
  promoForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(promoForm);
    const codeEl = document.getElementById('promo-code');
    const valEl = document.getElementById('promo-value');
    let valid = true;

    if (codeEl.value.trim().length < 4) {
      CartPulseUI.setFieldError(codeEl, 'Code must be at least 4 characters.');
      valid = false;
    }
    if (Number(valEl.value) <= 0) {
      CartPulseUI.setFieldError(valEl, 'Enter a positive discount value.');
      valid = false;
    }
    if (!valid) return;

    const btn = promoForm.querySelector('button[type="submit"]');
    CartPulseUI.setButtonLoading(btn, true, 'Creating...');
    try {
      await CartPulseAPI.admin.createPromo({
        code: codeEl.value.trim().toUpperCase(),
        discountType: document.getElementById('promo-type').value,
        discountValue: Number(valEl.value),
        minOrderAmount: Number(document.getElementById('promo-min').value || 0),
        maxUses: Number(document.getElementById('promo-max-uses').value || 100),
        expiresAt: document.getElementById('promo-expiry').value
      });
      promoForm.reset();
      CartPulseUI.showToast('Promo code created');
      await loadAdminData();
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, { code: 'promo-code', discountValue: 'promo-value' });
      CartPulseUI.showToast(err.message || 'Could not create promo', 'error');
    } finally {
      CartPulseUI.setButtonLoading(btn, false);
    }
  });

  document.getElementById('admin-category-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const input = document.getElementById('new-cat-name');
    const name = input.value.trim();
    if (name.length < 2) {
      CartPulseUI.showToast('Enter a valid category name.', 'error');
      return;
    }
    try {
      await CartPulseAPI.admin.createCategory({ name, description: '' });
      input.value = '';
      CartPulseUI.showToast(`Category "${name}" added`);
      await loadAdminData();
    } catch (err) {
      CartPulseUI.showToast(err.message || 'Could not add category', 'error');
    }
  });
}

function renderAll() {
  if (analytics) {
    const kpiCards = document.querySelectorAll('#admin-tab-analytics .kpi-value');
    if (kpiCards[0]) kpiCards[0].textContent = CartPulseUI.formatCurrency(analytics.platformGmv);
    if (kpiCards[1]) kpiCards[1].textContent = analytics.totalOrders;
    document.getElementById('admin-kpi-promos').textContent = analytics.activePromos;
    document.getElementById('admin-kpi-users').textContent = analytics.totalUsers;
  }

  const promoTbody = document.getElementById('admin-promos-tbody');
  promoTbody.innerHTML = promos
    .map(
      (p) => `
      <tr>
        <td class="fw-700">${CartPulseUI.escapeHtml(p.code)}</td>
        <td>${p.discountType === 'PERCENT' ? `${p.discountValue}% off` : `${CartPulseUI.formatCurrency(p.discountValue)} off`}</td>
        <td class="tabular-nums">${CartPulseUI.formatCurrency(p.minOrderAmount)}</td>
        <td class="tabular-nums">${p.usedCount} / ${p.maxUses}</td>
        <td>${CartPulseUI.escapeHtml(p.expiresAt)}</td>
        <td><span class="badge ${p.active ? 'badge-success' : 'badge-muted'}">${p.active ? 'Active' : 'Disabled'}</span></td>
        <td><button type="button" class="btn btn-secondary btn-sm" data-toggle-promo="${p.id}">${p.active ? 'Disable' : 'Enable'}</button></td>
      </tr>`
    )
    .join('');

  promoTbody.querySelectorAll('[data-toggle-promo]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        await CartPulseAPI.admin.togglePromo(Number(btn.dataset.togglePromo));
        await loadAdminData();
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Could not toggle promo', 'error');
      }
    });
  });

  document.getElementById('admin-categories-tbody').innerHTML = categories
    .map(
      (c) => `
      <tr>
        <td class="tabular-nums">${c.id}</td>
        <td class="fw-600">${CartPulseUI.escapeHtml(c.name)}</td>
        <td class="text-muted">${CartPulseUI.escapeHtml(c.slug)}</td>
      </tr>`
    )
    .join('');

  const userTbody = document.getElementById('admin-users-tbody');
  userTbody.innerHTML = users
    .map(
      (u) => `
      <tr>
        <td class="fw-600">${CartPulseUI.escapeHtml(u.fullName)}</td>
        <td>${CartPulseUI.escapeHtml(u.email)}</td>
        <td><span class="badge badge-primary">${u.role}</span></td>
        <td><span class="badge ${u.active ? 'badge-success' : 'badge-danger'}">${u.active ? 'Active' : 'Suspended'}</span></td>
        <td>
          ${
            u.role === 'ADMIN'
              ? '<span class="text-muted" style="font-size:0.82rem;">Protected</span>'
              : `<button type="button" class="btn btn-secondary btn-sm" data-toggle-user="${u.id}">
                  ${u.active ? 'Suspend' : 'Activate'}
                </button>`
          }
        </td>
      </tr>`
    )
    .join('');

  userTbody.querySelectorAll('[data-toggle-user]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        const updated = await CartPulseAPI.admin.toggleUser(Number(btn.dataset.toggleUser));
        CartPulseUI.showToast(`User ${updated.fullName} is now ${updated.active ? 'Active' : 'Suspended'}`);
        await loadAdminData();
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Could not update user', 'error');
      }
    });
  });
}

function renderCharts() {
  if (typeof Chart === 'undefined' || !analytics) return;

  if (volumeChart) volumeChart.destroy();
  if (statusChart) statusChart.destroy();

  volumeChart = new Chart(document.getElementById('admin-volume-chart'), {
    type: 'bar',
    data: {
      labels: analytics.monthlyVolume?.labels || [],
      datasets: [
        {
          label: 'GMV ($)',
          data: (analytics.monthlyVolume?.values || []).map(Number),
          backgroundColor: '#f2e205',
          borderColor: '#111111',
          borderWidth: 1.5,
          borderRadius: 2
        }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
  });

  const statusEntries = Object.entries(analytics.ordersByStatus || {});
  const totalCount = statusEntries.reduce((sum, [, v]) => sum + Number(v), 0);

  statusChart = new Chart(document.getElementById('admin-status-chart'), {
    type: 'pie',
    data: {
      labels: totalCount > 0 ? statusEntries.map(([k]) => k) : ['No orders yet'],
      datasets: [
        {
          data: totalCount > 0 ? statusEntries.map(([, v]) => Number(v)) : [1],
          backgroundColor: totalCount > 0
            ? ['#f2e205', '#111111', '#0369a1', '#b45309', '#059669', '#e11d48']
            : ['#e4e4df']
        }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false }
  });
}

init();
