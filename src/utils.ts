/** Lifecycle states an order can occupy. */
export type OrderStatus = 'pending' | 'paid' | 'packed' | 'shipped' | 'cancelled';

/** A product available for purchase. */
export interface Product {
  id: string;
  name: string;
  price: number;
  stock: number;
}

/** A single line item as supplied by the caller when creating an order. */
export interface OrderItemInput {
  productId: string;
  quantity: number;
}

/** Payload required to create a new order. */
export interface CreateOrderInput {
  customerName: string;
  customerEmail: string;
  items: OrderItemInput[];
}

/** A resolved line item stored on an order. */
export interface OrderItem {
  productId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/** A complete order record. */
export interface Order {
  id: string;
  customerName: string;
  customerEmail: string;
  items: OrderItem[];
  total: number;
  status: OrderStatus;
  createdAt: string;
}

const products: Product[] = [
  { id: 'coffee-beans', name: 'Coffee Beans', price: 14.5, stock: 40 },
  { id: 'travel-mug', name: 'Travel Mug', price: 22, stock: 18 },
  { id: 'brew-scale', name: 'Brew Scale', price: 38.75, stock: 12 }
];

const orders: Order[] = [];

/** Returns a shallow copy of every available product. */
export function listProducts(): Product[] {
  return products.map(product => ({ ...product }));
}

/** Returns a deep copy of all orders. */
export function listOrders(): Order[] {
  return orders.map(order => ({ ...order, items: order.items.map(item => ({ ...item })) }));
}

/**
 * Finds an order by its ID.
 * @returns A deep copy of the order, or `undefined` if not found.
 */
export function findOrder(id: string): Order | undefined {
  const order = orders.find(candidate => candidate.id === id);
  return order ? { ...order, items: order.items.map(item => ({ ...item })) } : undefined;
}

/**
 * Creates and persists a new order from an untyped request payload.
 * @throws {Error} If required fields are missing, the email is invalid, no items are provided, or a product ID is unknown.
 */
export function createOrder(payload: unknown): Order {
  const input = payload as CreateOrderInput;

  if (!input.customerName || !input.customerEmail || !input.customerEmail.includes('@')) {
    throw new Error('A customer name and valid email are required.');
  }

  if (!Array.isArray(input.items) || input.items.length === 0) {
    throw new Error('At least one order item is required.');
  }

  const items = input.items.map(item => {
    const product = products.find(candidate => candidate.id === item.productId);
    if (!product) {
      throw new Error(`Unknown product: ${item.productId}`);
    }

    const quantity = Number(item.quantity) || 1;
    return {
      productId: product.id,
      name: product.name,
      quantity,
      unitPrice: product.price,
      subtotal: product.price * quantity
    };
  });

  const order: Order = {
    id: Math.random().toString(36).slice(2),
    customerName: input.customerName.trim(),
    customerEmail: input.customerEmail.trim().toLowerCase(),
    items,
    total: items.reduce((sum, item) => sum + item.subtotal, 0),
    status: 'pending',
    createdAt: new Date().toISOString()
  };

  orders.push(order);
  return { ...order, items: order.items.map(item => ({ ...item })) };
}

/**
 * Updates the status of an existing order.
 * @returns A deep copy of the updated order, or `undefined` if the order was not found.
 * @throws {Error} If `status` is not a valid `OrderStatus` value.
 */
export function updateOrderStatus(id: string, status: OrderStatus): Order | undefined {
  const allowedStatuses: OrderStatus[] = ['pending', 'paid', 'packed', 'shipped', 'cancelled'];
  const order = orders.find(candidate => candidate.id === id);

  if (!order) {
    return undefined;
  }

  if (!allowedStatuses.includes(status)) {
    throw new Error(`Unsupported order status: ${status}`);
  }

  order.status = status;
  return { ...order, items: order.items.map(item => ({ ...item })) };
}

/**
 * Removes an order from the store.
 * @returns `true` if the order was found and deleted, `false` otherwise.
 */
export function cancelOrder(id: string): boolean {
  const index = orders.findIndex(order => order.id === id);
  if (index === -1) {
    return false;
  }

  orders.splice(index, 1);
  return true;
}

/** Clears all orders from the in-memory store. Intended for use in tests. */
export function resetOrders(): void {
  orders.splice(0, orders.length);
}
