import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

let categories = [];
let sellerProducts = [];
let lowStockProducts = [];
let sellerOrders = [];
let analytics = null;

let revenueChart = null;
let categoryChart = null;

const NEXT_STATUS = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'PACKED',
  PACKED: 'SHIPPED',
  SHIPPED: 'DELIVERED'
};

async function init() {
  await CartPulseUI.initPage('seller');
  const mainEl = document.querySelector('main .container');
  if (!CartPulseUI.requireAuth(['SELLER', 'ADMIN'], mainEl)) return;

  bindTabs();
  bindModal();
  bindLogsModal();
  bindSearch();
  await loadDashboardData();
}

async function loadDashboardData() {
  try {
    const [cats, analyticsData, products, lowStock, orders] = await Promise.all([
      CartPulseAPI.catalog.categories(),
      CartPulseAPI.seller.analytics(),
      CartPulseAPI.seller.products(),
      CartPulseAPI.seller.lowStock(),
      CartPulseAPI.seller.orders()
    ]);

    categories = cats;
    analytics = analyticsData;
    sellerProducts = products;
    lowStockProducts = lowStock;
    sellerOrders = orders;

    populateCategoryOptions();
    renderAll();
    renderCharts();
  } catch (err) {
    CartPulseUI.showToast(err.message || 'Failed to load seller portal data', 'error');
  }
}

function populateCategoryOptions() {
  const select = document.getElementById('prod-category');
  select.innerHTML = categories
    .map((c) => `<option value="${c.id}">${CartPulseUI.escapeHtml(c.name)}</option>`)
    .join('');
}

function bindTabs() {
  const navBtns = document.querySelectorAll('[data-tab]');
  navBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      navBtns.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      ['overview', 'products', 'orders'].forEach((t) => {
        document.getElementById(`tab-${t}`).classList.toggle('hidden', t !== btn.dataset.tab);
      });
    });
  });
}

function bindSearch() {
  document.getElementById('seller-product-search').addEventListener('input', renderProductsTable);
}

function renderAll() {
  if (analytics) {
    document.getElementById('kpi-revenue').textContent = CartPulseUI.formatCurrency(analytics.grossRevenue);
    document.getElementById('kpi-orders').textContent = analytics.totalOrders;
    document.getElementById('kpi-products').textContent = analytics.activeProducts;
    document.getElementById('kpi-lowstock').textContent = analytics.lowStockCount;
  }

  renderLowStockTable();
  renderProductsTable();
  renderOrdersTable();
}

function renderLowStockTable() {
  const tbody = document.getElementById('low-stock-tbody');
  if (lowStockProducts.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-muted" style="text-align:center;padding:1.5rem;">All inventory levels are healthy.</td></tr>`;
    return;
  }
  tbody.innerHTML = lowStockProducts
    .map(
      (p) => `
      <tr>
        <td class="fw-600">${CartPulseUI.escapeHtml(p.name)}</td>
        <td>${CartPulseUI.escapeHtml(p.categoryName)}</td>
        <td class="tabular-nums">${CartPulseUI.getStockBadge(p.stockQuantity, p.lowStockThreshold)}</td>
        <td class="tabular-nums">${p.lowStockThreshold}</td>
        <td>
          <button type="button" class="btn btn-secondary btn-sm" data-restock="${p.id}">+10 Restock</button>
        </td>
      </tr>`
    )
    .join('');

  tbody.querySelectorAll('[data-restock]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const id = Number(btn.dataset.restock);
      CartPulseUI.setButtonLoading(btn, true, 'Saving...');
      try {
        await CartPulseAPI.seller.restock(id, 10);
        CartPulseUI.showToast('Inventory restocked (+10 units)');
        await loadDashboardData();
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Restock failed', 'error');
        CartPulseUI.setButtonLoading(btn, false);
      }
    });
  });
}

function renderProductsTable() {
  const tbody = document.getElementById('seller-products-tbody');
  const q = document.getElementById('seller-product-search').value.trim().toLowerCase();
  const rows = sellerProducts.filter((p) => p.name.toLowerCase().includes(q));

  if (rows.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-muted" style="text-align:center;padding:1.5rem;">No products found.</td></tr>`;
    return;
  }

  tbody.innerHTML = rows
    .map(
      (p) => `
      <tr>
        <td class="fw-600">${CartPulseUI.escapeHtml(p.name)}</td>
        <td>${CartPulseUI.escapeHtml(p.categoryName)}</td>
        <td class="tabular-nums">${CartPulseUI.formatCurrency(p.price)}</td>
        <td class="tabular-nums">${CartPulseUI.getStockBadge(p.stockQuantity, p.lowStockThreshold)}</td>
        <td><span class="badge ${p.active ? 'badge-success' : 'badge-muted'}">${p.active ? 'Active' : 'Inactive'}</span></td>
        <td class="flex-gap">
          <button type="button" class="btn btn-secondary btn-sm" data-edit="${p.id}">Edit</button>
          <button type="button" class="btn btn-ghost btn-sm" data-logs="${p.id}">Logs</button>
          <button type="button" class="btn btn-ghost btn-sm" data-toggle="${p.id}">${p.active ? 'Deactivate' : 'Activate'}</button>
        </td>
      </tr>`
    )
    .join('');

  tbody.querySelectorAll('[data-edit]').forEach((btn) => {
    btn.addEventListener('click', () => openModalForEdit(Number(btn.dataset.edit)));
  });
  tbody.querySelectorAll('[data-logs]').forEach((btn) => {
    btn.addEventListener('click', () => openInventoryLogs(Number(btn.dataset.logs)));
  });
  tbody.querySelectorAll('[data-toggle]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      try {
        const updated = await CartPulseAPI.seller.toggleProduct(Number(btn.dataset.toggle));
        CartPulseUI.showToast(`Product ${updated.active ? 'activated' : 'deactivated'}`);
        await loadDashboardData();
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Could not update product', 'error');
      }
    });
  });
}

function renderOrdersTable() {
  const tbody = document.getElementById('seller-orders-tbody');
  if (sellerOrders.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-muted" style="text-align:center;padding:1.5rem;">No customer orders received yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = sellerOrders
    .map((o) => {
      const next = NEXT_STATUS[o.status];
      return `
        <tr>
          <td class="fw-700">${CartPulseUI.escapeHtml(o.orderNumber)}</td>
          <td>${CartPulseUI.escapeHtml(o.customerName)}</td>
          <td class="tabular-nums fw-600">${CartPulseUI.formatCurrency(o.totalAmount)}</td>
          <td>${CartPulseUI.getStatusBadge(o.status)}</td>
          <td>
            ${
              next
                ? `<button type="button" class="btn btn-primary btn-sm" data-advance="${o.id}" data-next="${next}">Mark as ${next}</button>`
                : `<span class="text-muted" style="font-size:0.82rem;">Completed / Closed</span>`
            }
          </td>
        </tr>`;
    })
    .join('');

  tbody.querySelectorAll('[data-advance]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const id = Number(btn.dataset.advance);
      const nextStatus = btn.dataset.next;
      CartPulseUI.setButtonLoading(btn, true, 'Updating...');
      try {
        const updated = await CartPulseAPI.seller.updateOrderStatus(id, nextStatus);
        CartPulseUI.showToast(`Order ${updated.orderNumber} updated to ${updated.status}`);
        await loadDashboardData();
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Could not update status', 'error');
        CartPulseUI.setButtonLoading(btn, false);
      }
    });
  });
}

function bindModal() {
  const modal = document.getElementById('product-modal');
  const form = document.getElementById('product-form');

  const close = () => {
    modal.classList.remove('open');
    CartPulseUI.clearFormErrors(form);
    form.reset();
    document.getElementById('prod-id').value = '';
  };

  document.getElementById('open-product-modal-btn').addEventListener('click', () => {
    document.getElementById('product-modal-title').textContent = 'Add Product';
    modal.classList.add('open');
  });
  document.getElementById('close-product-modal').addEventListener('click', close);
  document.getElementById('cancel-product-modal').addEventListener('click', close);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(form);

    const id = document.getElementById('prod-id').value;
    const nameEl = document.getElementById('prod-name');
    const catEl = document.getElementById('prod-category');
    const priceEl = document.getElementById('prod-price');
    const stockEl = document.getElementById('prod-stock');
    const thresholdEl = document.getElementById('prod-threshold');
    const descEl = document.getElementById('prod-desc');
    const imgEl = document.getElementById('prod-image');

    let valid = true;
    if (nameEl.value.trim().length < 3) {
      CartPulseUI.setFieldError(nameEl, 'Product name must be at least 3 characters.');
      valid = false;
    }
    if (Number(priceEl.value) <= 0) {
      CartPulseUI.setFieldError(priceEl, 'Price must be greater than $0.');
      valid = false;
    }
    if (stockEl.value === '' || Number(stockEl.value) < 0) {
      CartPulseUI.setFieldError(stockEl, 'Stock quantity cannot be negative.');
      valid = false;
    }
    if (descEl.value.trim().length < 10) {
      CartPulseUI.setFieldError(descEl, 'Provide at least 10 characters of description.');
      valid = false;
    }
    if (!valid) return;

    const payload = {
      name: nameEl.value.trim(),
      categoryId: Number(catEl.value),
      price: Number(priceEl.value),
      stockQuantity: Number(stockEl.value),
      lowStockThreshold: Number(thresholdEl.value || 5),
      imageUrl: imgEl.value.trim(),
      description: descEl.value.trim()
    };

    const submitBtn = form.querySelector('button[type="submit"]');
    CartPulseUI.setButtonLoading(submitBtn, true, 'Saving...');

    try {
      if (id) {
        await CartPulseAPI.seller.updateProduct(Number(id), payload);
        CartPulseUI.showToast('Product updated');
      } else {
        await CartPulseAPI.seller.createProduct(payload);
        CartPulseUI.showToast('Product created');
      }
      close();
      await loadDashboardData();
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, {
        name: 'prod-name',
        categoryId: 'prod-category',
        price: 'prod-price',
        stockQuantity: 'prod-stock',
        lowStockThreshold: 'prod-threshold',
        description: 'prod-desc'
      });
      CartPulseUI.showToast(err.message || 'Could not save product', 'error');
    } finally {
      CartPulseUI.setButtonLoading(submitBtn, false);
    }
  });
}

function openModalForEdit(id) {
  const p = sellerProducts.find((item) => item.id === id);
  if (!p) return;
  document.getElementById('product-modal-title').textContent = 'Edit Product';
  document.getElementById('prod-id').value = p.id;
  document.getElementById('prod-name').value = p.name;
  document.getElementById('prod-category').value = p.categoryId;
  document.getElementById('prod-price').value = p.price;
  document.getElementById('prod-stock').value = p.stockQuantity;
  document.getElementById('prod-threshold').value = p.lowStockThreshold;
  document.getElementById('prod-image').value = p.imageUrl || '';
  document.getElementById('prod-desc').value = p.description;
  document.getElementById('product-modal').classList.add('open');
}

function bindLogsModal() {
  const modal = document.getElementById('logs-modal');
  document.getElementById('close-logs-modal').addEventListener('click', () => modal.classList.remove('open'));
}

async function openInventoryLogs(productId) {
  const modal = document.getElementById('logs-modal');
  const tbody = document.getElementById('logs-modal-tbody');
  const product = sellerProducts.find((p) => p.id === productId);
  document.getElementById('logs-modal-title').textContent = `Inventory Audit — ${product?.name || 'Product'}`;
  tbody.innerHTML = `<tr><td colspan="4" class="text-muted" style="text-align:center;">Loading logs...</td></tr>`;
  modal.classList.add('open');

  try {
    const logs = await CartPulseAPI.seller.inventoryLogs(productId);
    if (logs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4" class="text-muted" style="text-align:center;">No inventory movements logged yet.</td></tr>`;
      return;
    }
    tbody.innerHTML = logs
      .map(
        (l) => `
        <tr>
          <td>${CartPulseUI.formatDateTime(l.createdAt)}</td>
          <td><span class="badge badge-muted">${CartPulseUI.escapeHtml(l.reason)}</span></td>
          <td class="tabular-nums fw-700 ${l.changeAmount >= 0 ? 'text-success' : 'text-danger'}">
            ${l.changeAmount >= 0 ? `+${l.changeAmount}` : l.changeAmount}
          </td>
          <td class="tabular-nums text-muted">${l.referenceId ? `#${l.referenceId}` : '—'}</td>
        </tr>`
      )
      .join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" class="text-danger" style="text-align:center;">${CartPulseUI.escapeHtml(err.message)}</td></tr>`;
  }
}

function renderCharts() {
  if (typeof Chart === 'undefined' || !analytics) return;

  if (revenueChart) revenueChart.destroy();
  if (categoryChart) categoryChart.destroy();

  revenueChart = new Chart(document.getElementById('seller-revenue-chart'), {
    type: 'line',
    data: {
      labels: analytics.dailyRevenue?.labels || [],
      datasets: [
        {
          label: 'Revenue ($)',
          data: (analytics.dailyRevenue?.values || []).map(Number),
          borderColor: '#111111',
          backgroundColor: 'rgba(242, 226, 5, 0.35)',
          fill: true,
          tension: 0.35
        }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
  });

  const catEntries = Object.entries(analytics.salesByCategory || {});
  const hasCatData = catEntries.length > 0;
  categoryChart = new Chart(document.getElementById('seller-category-chart'), {
    type: 'doughnut',
    data: {
      labels: hasCatData ? catEntries.map(([k]) => k) : ['No sales yet'],
      datasets: [
        {
          data: hasCatData ? catEntries.map(([, v]) => Number(v)) : [1],
          backgroundColor: hasCatData
            ? ['#f2e205', '#111111', '#059669', '#0369a1', '#d97706']
            : ['#e4e4df']
        }
      ]
    },
    options: { responsive: true, maintainAspectRatio: false }
  });
}

init();
