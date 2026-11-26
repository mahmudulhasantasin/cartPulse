import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

const STEPS = ['PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED', 'DELIVERED'];
let orders = [];

async function init() {
  await CartPulseUI.initPage('orders');
  if (!CartPulseUI.requireAuth()) return;

  document.getElementById('order-status-filter').addEventListener('change', renderOrders);
  await loadOrders();
}

async function loadOrders() {
  const host = document.getElementById('orders-list-host');
  try {
    orders = await CartPulseAPI.orders.myOrders();
    renderOrders();
  } catch (err) {
    CartPulseUI.renderStateBox(host, {
      title: 'Failed to load orders',
      message: err.message,
      isError: true,
      actionHtml: `<button type="button" class="btn btn-primary" id="retry-orders-btn">Retry</button>`
    });
    document.getElementById('retry-orders-btn')?.addEventListener('click', loadOrders);
  }
}

function renderOrders() {
  const host = document.getElementById('orders-list-host');
  const filter = document.getElementById('order-status-filter').value;
  const visible = filter ? orders.filter((o) => o.status === filter) : orders;

  if (visible.length === 0) {
    CartPulseUI.renderStateBox(host, {
      title: 'No orders found',
      message: filter
        ? `You have no orders in "${filter}" status.`
        : 'Once you place an order, you can track its lifecycle here.',
      actionHtml: `<a href="index.html" class="btn btn-primary">Shop Now</a>`
    });
    return;
  }

  host.innerHTML = visible.map(renderOrderCard).join('');

  host.querySelectorAll('[data-cancel-order]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const id = Number(btn.dataset.cancelOrder);
      CartPulseUI.setButtonLoading(btn, true, 'Cancelling...');
      try {
        const updated = await CartPulseAPI.orders.cancel(id);
        orders = orders.map((o) => (o.id === id ? updated : o));
        renderOrders();
        CartPulseUI.showToast(`Order ${updated.orderNumber} cancelled & stock restored.`);
      } catch (err) {
        CartPulseUI.showToast(err.message || 'Could not cancel order', 'error');
        CartPulseUI.setButtonLoading(btn, false);
      }
    });
  });
}

function renderOrderCard(order) {
  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED';
  const stepIndex = STEPS.indexOf(order.status);

  const stepperHtml =
    order.status === 'CANCELLED'
      ? `<div class="alert alert-danger">This order was cancelled and inventory quantities were restored.</div>`
      : `
        <div class="stepper">
          ${STEPS.map((step, idx) => {
            const cls = idx < stepIndex ? 'completed' : idx === stepIndex ? 'active' : '';
            const marker = idx < stepIndex ? '✓' : idx + 1;
            return `
              <div class="step ${cls}">
                <div class="step-dot">${marker}</div>
                <span>${step}</span>
              </div>
            `;
          }).join('')}
        </div>
      `;

  return `
    <article class="card">
      <div class="card-header">
        <div>
          <span class="fw-700">${CartPulseUI.escapeHtml(order.orderNumber)}</span>
          <span class="text-muted" style="font-size:0.84rem;margin-left:0.5rem;">
            Placed ${CartPulseUI.formatDate(order.createdAt)} &bull; Seller: <strong>${CartPulseUI.escapeHtml(order.sellerName)}</strong>
          </span>
        </div>
        <div class="flex-gap">
          ${CartPulseUI.getStatusBadge(order.status)}
          ${
            canCancel
              ? `<button type="button" class="btn btn-danger btn-sm" data-cancel-order="${order.id}">Cancel Order</button>`
              : ''
          }
        </div>
      </div>

      <div class="card-body">
        ${stepperHtml}

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Unit Price</th>
                <th>Qty</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${order.items
                .map(
                  (item) => `
                <tr>
                  <td class="fw-600">${CartPulseUI.escapeHtml(item.productName)}</td>
                  <td class="tabular-nums">${CartPulseUI.formatCurrency(item.unitPrice)}</td>
                  <td class="tabular-nums">${item.quantity}</td>
                  <td class="tabular-nums fw-600">${CartPulseUI.formatCurrency(item.lineTotal)}</td>
                  <td style="text-align:right;">
                    ${
                      order.status === 'DELIVERED'
                        ? `<a href="product.html?id=${item.productId}#review-form" class="btn btn-secondary btn-sm">Write Review</a>`
                        : ''
                    }
                  </td>
                </tr>`
                )
                .join('')}
            </tbody>
          </table>
        </div>

        <div class="flex-between mt-2" style="padding-top:0.85rem;border-top:1px solid var(--border);">
          <div class="text-muted" style="font-size:0.85rem;">
            Ship to: ${CartPulseUI.escapeHtml(order.shippingAddress)}
          </div>
          <div class="tabular-nums" style="text-align:right;">
            <span class="text-muted" style="font-size:0.85rem;margin-right:0.75rem;">
              Subtotal: ${CartPulseUI.formatCurrency(order.subtotalAmount)} | Discount: -${CartPulseUI.formatCurrency(order.discountAmount)}
            </span>
            <strong style="font-size:1.05rem;">Paid: ${CartPulseUI.formatCurrency(order.totalAmount)}</strong>
          </div>
        </div>
      </div>
    </article>
  `;
}

init();
