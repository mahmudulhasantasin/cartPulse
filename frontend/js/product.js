import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

const params = new URLSearchParams(window.location.search);
const productId = Number(params.get('id'));

async function init() {
  await CartPulseUI.initPage('store');

  if (!productId) {
    renderMissing('No product specified.');
    return;
  }

  await loadProduct();
  await loadReviews();
  bindReviewForm();
}

function renderMissing(msg) {
  const host = document.getElementById('product-detail-host');
  CartPulseUI.renderStateBox(host, {
    title: 'Product not found',
    message: msg,
    isError: true,
    actionHtml: `<a href="index.html" class="btn btn-primary">Return to Storefront</a>`
  });
}

async function loadProduct() {
  const host = document.getElementById('product-detail-host');
  try {
    const product = await CartPulseAPI.catalog.product(productId);
    const out = product.stockQuantity <= 0;

    host.innerHTML = `
      <div class="product-detail-grid">
        <div class="product-gallery">
          <img src="${CartPulseUI.escapeHtml(product.imageUrl)}" alt="${CartPulseUI.escapeHtml(product.name)}">
        </div>
        <div class="card card-body">
          <div class="flex-between mb-1">
            <span class="badge badge-primary">${CartPulseUI.escapeHtml(product.categoryName)}</span>
            ${CartPulseUI.getStockBadge(product.stockQuantity, product.lowStockThreshold)}
          </div>
          <h1 class="page-title">${CartPulseUI.escapeHtml(product.name)}</h1>
          <p class="text-muted" style="font-size:0.9rem;">
            Sold by <strong>${CartPulseUI.escapeHtml(product.sellerName)}</strong> &bull;
            ★ ${Number(product.avgRating || 0).toFixed(1)} (${product.reviewCount} reviews)
          </p>
          <div class="product-price tabular-nums" style="font-size:1.85rem;margin:0.75rem 0 1rem;">
            ${CartPulseUI.formatCurrency(product.price)}
          </div>
          <p>${CartPulseUI.escapeHtml(product.description)}</p>

          <div class="flex-gap mt-2" style="padding-top:1rem;border-top:1px solid var(--border);">
            <div class="qty-control">
              <button type="button" id="qty-dec" ${out ? 'disabled' : ''}>-</button>
              <input type="number" id="qty-input" value="1" min="1" max="${Math.max(1, product.stockQuantity)}" ${out ? 'disabled' : ''}>
              <button type="button" id="qty-inc" ${out ? 'disabled' : ''}>+</button>
            </div>
            <button type="button" id="add-cart-btn" class="btn btn-primary btn-lg" ${out ? 'disabled' : ''}>
              ${out ? 'Currently Out of Stock' : 'Add to Cart'}
            </button>
          </div>

          <div class="alert alert-info mt-2" style="margin-bottom:0;">
            <span>Stock is locked atomically during checkout to prevent overselling.</span>
          </div>
        </div>
      </div>
    `;

    const qtyInput = document.getElementById('qty-input');
    document.getElementById('qty-dec')?.addEventListener('click', () => {
      qtyInput.value = Math.max(1, Number(qtyInput.value) - 1);
    });
    document.getElementById('qty-inc')?.addEventListener('click', () => {
      qtyInput.value = Math.min(product.stockQuantity, Number(qtyInput.value) + 1);
    });

    const addBtn = document.getElementById('add-cart-btn');
    addBtn?.addEventListener('click', async () => {
      if (!CartPulseUI.getCurrentUser()) {
        CartPulseUI.showToast('Please sign in to add items to your cart.', 'warning');
        setTimeout(() => {
          window.location.href = `login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        }, 550);
        return;
      }

      const qty = Math.max(1, Math.min(product.stockQuantity, Number(qtyInput.value || 1)));
      CartPulseUI.setButtonLoading(addBtn, true, 'Adding...');
      try {
        const cart = await CartPulseAPI.cart.addItem(product.id, qty);
        CartPulseUI.setCartBadgeCount(cart.totalItems || 0);
        CartPulseUI.showToast(`Added ${qty} × "${product.name}" to cart`);
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Unable to add to cart', 'error');
      } finally {
        CartPulseUI.setButtonLoading(addBtn, false);
      }
    });
  } catch (err) {
    renderMissing(err.message || 'This product could not be loaded.');
  }
}

async function loadReviews() {
  const list = document.getElementById('reviews-list');
  try {
    const reviews = await CartPulseAPI.reviews.list(productId);
    if (reviews.length === 0) {
      CartPulseUI.renderStateBox(list, {
        title: 'No reviews yet',
        message: 'Verified buyers whose orders are DELIVERED can leave the first review.'
      });
      return;
    }

    list.innerHTML = reviews
      .map(
        (r) => `
        <div style="padding:0.9rem 0;border-bottom:1px solid var(--border);">
          <div class="flex-between">
            <strong>${CartPulseUI.escapeHtml(r.customerName)}</strong>
            <span class="text-muted" style="font-size:0.8rem;">${CartPulseUI.formatDate(r.createdAt)}</span>
          </div>
          <div class="text-warning fw-700" style="font-size:0.88rem;margin:0.2rem 0;">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</div>
          <p style="margin:0;">${CartPulseUI.escapeHtml(r.comment)}</p>
        </div>`
      )
      .join('');
  } catch (err) {
    CartPulseUI.renderStateBox(list, {
      title: 'Could not load reviews',
      message: err.message,
      isError: true
    });
  }
}

function bindReviewForm() {
  const form = document.getElementById('review-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(form);

    if (!CartPulseUI.getCurrentUser()) {
      CartPulseUI.showToast('Sign in with a verified buyer account to post a review.', 'warning');
      return;
    }

    const ratingEl = document.getElementById('review-rating');
    const commentEl = document.getElementById('review-comment');
    let valid = true;

    if (!ratingEl.value) {
      CartPulseUI.setFieldError(ratingEl, 'Please select a star rating.');
      valid = false;
    }
    if (commentEl.value.trim().length < 10) {
      CartPulseUI.setFieldError(commentEl, 'Review must be at least 10 characters.');
      valid = false;
    }
    if (!valid) return;

    const btn = form.querySelector('button[type="submit"]');
    CartPulseUI.setButtonLoading(btn, true, 'Submitting...');
    try {
      await CartPulseAPI.reviews.create(productId, {
        rating: Number(ratingEl.value),
        comment: commentEl.value.trim()
      });
      form.reset();
      await loadProduct();
      await loadReviews();
      CartPulseUI.showToast('Verified review published!');
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, { rating: 'review-rating', comment: 'review-comment' });
      CartPulseUI.showToast(err.message || 'Could not submit review', 'error');
    } finally {
      CartPulseUI.setButtonLoading(btn, false);
    }
  });
}

init();
