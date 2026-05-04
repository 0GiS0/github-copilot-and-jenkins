const state = {
  products: [],
  orders: [],
  cart: new Map()
};

const statusLabels = ['pending', 'paid', 'packed', 'shipped', 'cancelled'];
const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const productList = document.querySelector('#productList');
const ordersList = document.querySelector('#ordersList');
const cartItems = document.querySelector('#cartItems');
const cartTotal = document.querySelector('#cartTotal');
const orderForm = document.querySelector('#orderForm');
const toast = document.querySelector('#toast');
const apiStatus = document.querySelector('#apiStatus');

function showToast(message) {
  toast.textContent = message;
  toast.classList.add('is-visible');
  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => toast.classList.remove('is-visible'), 2800);
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"]/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;'
  })[character]);
}

async function requestJson(url, options) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  const payload = await response.json();

  if (!response.ok) {
    throw new Error(payload.error || 'Request failed');
  }

  return payload;
}

function updateApiStatus(isOk) {
  apiStatus.textContent = isOk ? 'API conectada' : 'API no disponible';
  apiStatus.classList.toggle('is-ok', isOk);
  apiStatus.classList.toggle('is-error', !isOk);
}

function getCartTotal() {
  return [...state.cart.entries()].reduce((total, [productId, quantity]) => {
    const product = state.products.find(candidate => candidate.id === productId);
    return product ? total + product.price * quantity : total;
  }, 0);
}

function renderProducts() {
  productList.innerHTML = '';

  state.products.forEach(product => {
    const quantity = state.cart.get(product.id) || 1;
    const article = document.createElement('article');
    article.className = 'product-card';
    const productName = escapeHtml(product.name);
    article.innerHTML = `
      <div>
        <h3>${productName}</h3>
        <p>${escapeHtml(product.stock)} unidades disponibles</p>
      </div>
      <div class="product-price">${money.format(product.price)}</div>
      <div class="quantity-row">
        <input type="number" min="1" max="99" value="${quantity}" aria-label="Cantidad de ${productName}">
        <button class="secondary-button" type="button">Anadir</button>
      </div>
    `;

    const input = article.querySelector('input');
    article.querySelector('button').addEventListener('click', () => {
      state.cart.set(product.id, Math.max(1, Number(input.value) || 1));
      renderCart();
      showToast(`${product.name} actualizado en el carrito`);
    });

    productList.append(article);
  });
}

function renderCart() {
  cartItems.innerHTML = '';

  if (state.cart.size === 0) {
    cartItems.innerHTML = '<div class="empty-state">Selecciona productos del catalogo.</div>';
    cartTotal.textContent = money.format(0);
    return;
  }

  state.cart.forEach((quantity, productId) => {
    const product = state.products.find(candidate => candidate.id === productId);
    if (!product) {
      return;
    }

    const row = document.createElement('div');
    row.className = 'cart-item';
    const productName = escapeHtml(product.name);
    row.innerHTML = `
      <span>${productName} x ${escapeHtml(quantity)}</span>
      <button class="icon-button" type="button" aria-label="Quitar ${productName}" title="Quitar ${productName}">&times;</button>
    `;
    row.querySelector('button').addEventListener('click', () => {
      state.cart.delete(productId);
      renderCart();
    });
    cartItems.append(row);
  });

  cartTotal.textContent = money.format(getCartTotal());
}

function renderOrders() {
  ordersList.innerHTML = '';

  if (state.orders.length === 0) {
    ordersList.innerHTML = '<div class="empty-state">Todavia no hay pedidos creados.</div>';
    return;
  }

  state.orders.forEach(order => {
    const article = document.createElement('article');
    article.className = 'order-card';
    const orderId = escapeHtml(order.id);
    article.innerHTML = `
      <div class="order-meta">
        <div>
          <h3>${escapeHtml(order.customerName)}</h3>
          <div class="order-id">${orderId}</div>
        </div>
        <select class="status-select" aria-label="Estado del pedido ${orderId}">
          ${statusLabels.map(status => `<option value="${status}" ${status === order.status ? 'selected' : ''}>${status}</option>`).join('')}
        </select>
      </div>
      <ol class="order-items">
        ${order.items.map(item => `<li>${escapeHtml(item.name)} x ${escapeHtml(item.quantity)} - ${money.format(item.subtotal)}</li>`).join('')}
      </ol>
      <div class="order-actions">
        <div>
          <div class="order-total">${money.format(order.total)}</div>
          <div class="order-date">${new Date(order.createdAt).toLocaleString()}</div>
        </div>
        <button class="danger-button" type="button">Cancelar</button>
      </div>
    `;

    article.querySelector('select').addEventListener('change', event => updateStatus(order.id, event.target.value));
    article.querySelector('button').addEventListener('click', () => deleteOrder(order.id));
    ordersList.append(article);
  });
}

async function loadData() {
  try {
    const [health, productsPayload, ordersPayload] = await Promise.all([
      requestJson('/health'),
      requestJson('/products'),
      requestJson('/orders')
    ]);
    updateApiStatus(health.status === 'ok');
    state.products = productsPayload.products;
    state.orders = ordersPayload.orders;
    renderProducts();
    renderCart();
    renderOrders();
  } catch (error) {
    updateApiStatus(false);
    showToast(error.message);
  }
}

async function updateStatus(orderId, status) {
  try {
    await requestJson(`/orders/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    await loadData();
    showToast('Estado actualizado');
  } catch (error) {
    showToast(error.message);
  }
}

async function deleteOrder(orderId) {
  try {
    await requestJson(`/orders/${orderId}`, { method: 'DELETE' });
    await loadData();
    showToast('Pedido cancelado');
  } catch (error) {
    showToast(error.message);
  }
}

orderForm.addEventListener('submit', async event => {
  event.preventDefault();

  if (state.cart.size === 0) {
    showToast('Anade al menos un producto');
    return;
  }

  const formData = new FormData(orderForm);
  const items = [...state.cart.entries()].map(([productId, quantity]) => ({ productId, quantity }));

  try {
    await requestJson('/orders', {
      method: 'POST',
      body: JSON.stringify({
        customerName: formData.get('customerName'),
        customerEmail: formData.get('customerEmail'),
        items
      })
    });
    state.cart.clear();
    orderForm.reset();
    await loadData();
    showToast('Pedido creado');
  } catch (error) {
    showToast(error.message);
  }
});

document.querySelector('#refreshButton').addEventListener('click', loadData);
loadData();