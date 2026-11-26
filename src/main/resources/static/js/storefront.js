import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

const PAGE_SIZE = 6;

const state = {
  search: '',
  categoryId: '',
  minPrice: '',
  maxPrice: '',
  minRating: 0,
  inStockOnly: false,
  sort: 'featured',
  page: 0
};

let categories = [];

async function init() {
  await CartPulseUI.initPage('store');

  const urlParams = new URLSearchParams(window.location.search);
  const initialQ = urlParams.get('q');
  if (initialQ) {
    state.search = initialQ;
    const searchInput = document.getElementById('global-search-input');
    if (searchInput) searchInput.value = initialQ;
  }

  bindEvents();
  await loadCategories();
  await loadCatalog();
}

async function loadCategories() {
  try {
    categories = await CartPulseAPI.catalog.categories();
    renderCategories();
  } catch (err) {
    CartPulseUI.showToast(err.message || 'Could not load categories', 'error');
  }
}

function renderCategories() {
  const strip = document.getElementById('category-strip');
  const select = document.getElementById('filter-category');

  strip.innerHTML = [
    `<button type="button" class="chip ${state.categoryId === '' ? 'active' : ''}" data-cat="">All Products</button>`,
    ...categories.map(
      (c) => `<button type="button" class="chip ${String(c.id) === String(state.categoryId) ? 'active' : ''}" data-cat="${c.id}">${CartPulseUI.escapeHtml(c.name)}</button>`
    )
  ].join('');

  const prev = select.value;
  select.innerHTML = `<option value="">All Categories</option>` +
    categories.map((c) => `<option value="${c.id}">${CartPulseUI.escapeHtml(c.name)}</option>`).join('');
  select.value = state.categoryId || prev || '';
}

function bindEvents() {
  document.getElementById('category-strip').addEventListener('click', (e) => {
    const btn = e.target.closest('[data-cat]');
    if (!btn) return;
    state.categoryId = btn.dataset.cat;
    state.page = 0;
    document.getElementById('filter-category').value = state.categoryId;
    renderCategories();
    loadCatalog();
  });

  document.getElementById('apply-filters-btn').addEventListener('click', () => {
    state.categoryId = document.getElementById('filter-category').value;
    state.minPrice = document.getElementById('filter-min-price').value;
    state.maxPrice = document.getElementById('filter-max-price').value;
    state.minRating = Number(document.getElementById('filter-rating').value || 0);
    state.inStockOnly = document.getElementById('filter-instock').checked;
    state.page = 0;
    renderCategories();
    loadCatalog();
  });

  document.getElementById('reset-filters-btn').addEventListener('click', () => {
    state.search = '';
    state.categoryId = '';
    state.minPrice = '';
    state.maxPrice = '';
    state.minRating = 0;
    state.inStockOnly = false;
    state.sort = 'featured';
    state.page = 0;

    document.getElementById('filter-category').value = '';
    document.getElementById('filter-min-price').value = '';
    document.getElementById('filter-max-price').value = '';
    document.getElementById('filter-rating').value = '0';
    document.getElementById('filter-instock').checked = false;
    document.getElementById('sort-select').value = 'featured';
    const searchInput = document.getElementById('global-search-input');
    if (searchInput) searchInput.value = '';

    renderCategories();
    loadCatalog();
  });

  document.getElementById('sort-select').addEventListener('change', (e) => {
    state.sort = e.target.value;
    state.page = 0;
    loadCatalog();
  });

  window.addEventListener('cartpulse:search', (e) => {
    state.search = e.detail || '';
    state.page = 0;
    loadCatalog();
  });
}

async function loadCatalog() {
  const grid = document.getElementById('product-grid');
  const summary = document.getElementById('results-summary');
  const paginationHost = document.getElementById('pagination-host');

  CartPulseUI.renderSkeletonCards(grid, 6);

  try {
    const pageData = await CartPulseAPI.catalog.products({
      q: state.search,
      categoryId: state.categoryId,
      minPrice: state.minPrice,
      maxPrice: state.maxPrice,
      minRating: state.minRating > 0 ? state.minRating : '',
      inStockOnly: state.inStockOnly,
      sort: state.sort,
      page: state.page,
      size: PAGE_SIZE
    });

    const items = pageData.content || [];
    const total = pageData.totalElements ?? items.length;
    const totalPages = pageData.totalPages ?? 1;

    summary.textContent = `Showing ${items.length} of ${total} product${total === 1 ? '' : 's'}`;

    if (items.length === 0) {
      grid.style.display = 'block';
      CartPulseUI.renderStateBox(grid, {
        title: 'No matching products found',
        message: 'Try adjusting your search keywords or clearing your filters.',
        actionHtml: `<button type="button" class="btn btn-secondary" id="empty-reset-btn">Clear All Filters</button>`
      });
      document.getElementById('empty-reset-btn')?.addEventListener('click', () => {
        document.getElementById('reset-filters-btn').click();
      });
      paginationHost.innerHTML = '';
      return;
    }

    grid.style.display = 'grid';
    grid.innerHTML = items.map(renderProductCard).join('');

    grid.querySelectorAll('[data-add-cart]').forEach((btn) => {
      btn.addEventListener('click', () => addToCart(Number(btn.dataset.addCart), btn));
    });

    renderPagination(paginationHost, totalPages);
  } catch (err) {
    grid.style.display = 'block';
    summary.textContent = 'Error loading catalog';
    CartPulseUI.renderStateBox(grid, {
      title: 'Unable to load products',
      message: err.message || 'Database connection error.',
      isError: true,
      actionHtml: `<button type="button" class="btn btn-primary" id="retry-catalog-btn">Retry</button>`
    });
    document.getElementById('retry-catalog-btn')?.addEventListener('click', loadCatalog);
    paginationHost.innerHTML = '';
  }
}

function renderProductCard(p) {
  const out = p.stockQuantity <= 0;
  return `
    <article class="card product-card">
      <a href="product.html?id=${p.id}" class="product-thumb">
        <img src="${CartPulseUI.escapeHtml(p.imageUrl)}" alt="${CartPulseUI.escapeHtml(p.name)}" loading="lazy">
        <div class="product-thumb-badge">
          ${CartPulseUI.getStockBadge(p.stockQuantity, p.lowStockThreshold)}
        </div>
      </a>
      <div class="product-body">
        <span class="product-category">${CartPulseUI.escapeHtml(p.categoryName)}</span>
        <a href="product.html?id=${p.id}" class="product-title">${CartPulseUI.escapeHtml(p.name)}</a>
        <div class="product-meta">
          <span>by ${CartPulseUI.escapeHtml(p.sellerName)}</span>
          <span class="tabular-nums">★ ${Number(p.avgRating || 0).toFixed(1)} (${p.reviewCount})</span>
        </div>
        <div class="product-footer">
          <span class="product-price tabular-nums">${CartPulseUI.formatCurrency(p.price)}</span>
          <button type="button" class="btn btn-primary btn-sm" data-add-cart="${p.id}" ${out ? 'disabled' : ''}>
            ${out ? 'Sold Out' : 'Add to Cart'}
          </button>
        </div>
      </div>
    </article>
  `;
}

async function addToCart(productId, btn) {
  if (!CartPulseUI.getCurrentUser()) {
    CartPulseUI.showToast('Please sign in to add items to your cart.', 'warning');
    setTimeout(() => {
      window.location.href = `login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
    }, 550);
    return;
  }

  CartPulseUI.setButtonLoading(btn, true, 'Adding...');
  try {
    const cart = await CartPulseAPI.cart.addItem(productId, 1);
    CartPulseUI.setCartBadgeCount(cart.totalItems || 0);
    CartPulseUI.showToast('Added to your cart');
  } catch (err) {
    CartPulseUI.showToast(err.message || 'Could not add item to cart', 'error');
  } finally {
    CartPulseUI.setButtonLoading(btn, false);
  }
}

function renderPagination(host, totalPages) {
  if (totalPages <= 1) {
    host.innerHTML = '';
    return;
  }
  const buttons = [];
  for (let i = 0; i < totalPages; i++) {
    buttons.push(
      `<button type="button" class="btn btn-sm ${i === state.page ? 'btn-primary' : 'btn-secondary'}" data-page="${i}">${i + 1}</button>`
    );
  }
  host.innerHTML = buttons.join('');
  host.querySelectorAll('[data-page]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.page = Number(btn.dataset.page);
      loadCatalog();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  });
}

init();
