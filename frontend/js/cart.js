import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

let currentCart = { items: [], subtotal: 0, totalItems: 0 };
let appliedPromo = null;

async function init() {
  await CartPulseUI.initPage('cart');
  if (!CartPulseUI.requireAuth()) return;

  document.getElementById('clear-cart-btn').addEventListener('click', async () => {
    try {
      currentCart = await CartPulseAPI.cart.clear();
      appliedPromo = null;
      CartPulseUI.setCartBadgeCount(0);
      renderCart();
      CartPulseUI.showToast('Cart cleared');
    } catch (err) {
      CartPulseUI.showToast(err.message || 'Could not clear cart', 'error');
    }
  });

  bindPromoForm();
  bindCheckoutForm();
  await loadCart();
}

async function loadCart() {
  const host = document.getElementById('cart-items-host');
  try {
    currentCart = await CartPulseAPI.cart.get();
    CartPulseUI.setCartBadgeCount(currentCart.totalItems || 0);
    if (appliedPromo) {
      await revalidatePromo();
    }
    renderCart();
  } catch (err) {
    CartPulseUI.renderStateBox(host, {
      title: 'Failed to load cart',
      message: err.message,
      isError: true
    });
  }
}

function renderCart() {
  const host = document.getElementById('cart-items-host');
  const items = currentCart.items || [];

  if (items.length === 0) {
    CartPulseUI.renderStateBox(host, {
      title: 'Your cart is empty',
      message: 'Browse our catalog and add products to start checkout.',
      actionHtml: `<a href="index.html" class="btn btn-primary">Browse Products</a>`
    });
    updateTotals();
    return;
  }

  host.innerHTML = items
    .map(
      (item) => `
      <div class="cart-item-row">
        <img src="${CartPulseUI.escapeHtml(item.imageUrl)}" alt="${CartPulseUI.escapeHtml(item.productName)}" class="cart-item-img">
        <div>
          <div class="text-muted" style="font-size:0.78rem;font-weight:600;">Seller: ${CartPulseUI.escapeHtml(item.sellerName)}</div>
          <a href="product.html?id=${item.productId}" class="fw-700" style="color:var(--text);">${CartPulseUI.escapeHtml(item.productName)}</a>
          <div class="text-muted tabular-nums" style="font-size:0.86rem;">
            ${CartPulseUI.formatCurrency(item.unitPrice)} each &bull; ${item.availableStock} in stock
          </div>
        </div>
        <div style="display:flex;flex-direction:column;align-items:flex-end;gap:0.45rem;">
          <div class="qty-control">
            <button type="button" data-dec="${item.productId}" data-qty="${item.quantity}">-</button>
            <input type="number" value="${item.quantity}" readonly aria-label="Quantity">
            <button type="button" data-inc="${item.productId}" data-qty="${item.quantity}">+</button>
          </div>
          <div class="fw-700 tabular-nums">${CartPulseUI.formatCurrency(item.lineTotal)}</div>
          <button type="button" class="btn btn-ghost btn-sm text-danger" data-remove="${item.productId}">Remove</button>
        </div>
      </div>`
    )
    .join('');

  host.querySelectorAll('[data-dec]').forEach((b) =>
    b.addEventListener('click', () => updateQty(Number(b.dataset.dec), Number(b.dataset.qty) - 1))
  );
  host.querySelectorAll('[data-inc]').forEach((b) =>
    b.addEventListener('click', () => updateQty(Number(b.dataset.inc), Number(b.dataset.qty) + 1))
  );
  host.querySelectorAll('[data-remove]').forEach((b) =>
    b.addEventListener('click', () => updateQty(Number(b.dataset.remove), 0))
  );

  updateTotals();
}

async function updateQty(productId, nextQty) {
  try {
    currentCart = await CartPulseAPI.cart.updateItem(productId, Math.max(0, nextQty));
    CartPulseUI.setCartBadgeCount(currentCart.totalItems || 0);
    if (appliedPromo) {
      await revalidatePromo();
    }
    renderCart();
  } catch (err) {
    CartPulseUI.showToast(err.message || 'Unable to update quantity', 'warning');
  }
}

async function revalidatePromo() {
  if (!appliedPromo || Number(currentCart.subtotal) <= 0) {
    appliedPromo = null;
    return;
  }
  try {
    appliedPromo = await CartPulseAPI.cart.previewPromo(appliedPromo.code);
  } catch {
    appliedPromo = null;
  }
}

function updateTotals() {
  const subtotal = Number(currentCart.subtotal || 0);
  const discount = appliedPromo ? Number(appliedPromo.discountAmount || 0) : 0;
  const total = Math.max(0, subtotal - discount);

  const badge = document.getElementById('promo-applied-badge');
  if (appliedPromo && discount > 0) {
    badge.classList.remove('hidden');
    badge.textContent = `Promo ${appliedPromo.code} applied (-${CartPulseUI.formatCurrency(discount)})`;
  } else {
    badge.classList.add('hidden');
  }

  document.getElementById('summary-subtotal').textContent = CartPulseUI.formatCurrency(subtotal);
  document.getElementById('summary-discount').textContent = `-${CartPulseUI.formatCurrency(discount)}`;
  document.getElementById('summary-total').textContent = CartPulseUI.formatCurrency(total);
}

function bindPromoForm() {
  const form = document.getElementById('promo-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const input = document.getElementById('promo-input');
    const feedback = document.getElementById('promo-feedback');
    const code = input.value.trim().toUpperCase();
    input.classList.remove('is-invalid');
    feedback.style.display = 'none';

    if (!code) {
      input.classList.add('is-invalid');
      feedback.style.display = 'block';
      feedback.textContent = 'Enter a promo code first.';
      return;
    }

    try {
      appliedPromo = await CartPulseAPI.cart.previewPromo(code);
      updateTotals();
      CartPulseUI.showToast(appliedPromo.message || `Promo ${code} applied`);
    } catch (err) {
      appliedPromo = null;
      updateTotals();
      input.classList.add('is-invalid');
      feedback.style.display = 'block';
      feedback.textContent = err.message || 'Invalid promo code.';
    }
  });
}

function bindCheckoutForm() {
  const form = document.getElementById('checkout-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(form);

    if (!currentCart.items || currentCart.items.length === 0) {
      CartPulseUI.showToast('Add at least one item before placing an order.', 'error');
      return;
    }

    const addressEl = document.getElementById('shipping-address');
    const paymentEl = document.getElementById('payment-method');
    if (addressEl.value.trim().length < 10) {
      CartPulseUI.setFieldError(addressEl, 'Please enter a complete delivery address (at least 10 characters).');
      return;
    }

    const btn = document.getElementById('place-order-btn');
    CartPulseUI.setButtonLoading(btn, true, 'Reserving stock...');

    try {
      const orders = await CartPulseAPI.orders.checkout({
        shippingAddress: addressEl.value.trim(),
        paymentMethod: paymentEl.value,
        promoCode: appliedPromo ? appliedPromo.code : null
      });
      CartPulseUI.setCartBadgeCount(0);
      CartPulseUI.showToast(`Placed ${orders.length} order(s) successfully!`);
      setTimeout(() => {
        window.location.href = 'orders.html';
      }, 650);
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, { shippingAddress: 'shipping-address' });
      CartPulseUI.showToast(err.message || 'Checkout failed', 'error');
      await loadCart();
    } finally {
      CartPulseUI.setButtonLoading(btn, false);
    }
  });
}

init();
