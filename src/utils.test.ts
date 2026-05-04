import { cancelOrder, createOrder, findOrder, listOrders, listProducts, resetOrders, updateOrderStatus } from './utils';

describe('order API domain', () => {
  beforeEach(() => {
    resetOrders();
  });

  it('lists the product catalog', () => {
    const products = listProducts();

    expect(products).toHaveLength(3);
    expect(products[0]).toMatchObject({ id: 'coffee-beans', name: 'Coffee Beans' });
  });

  it('creates an order with a calculated total', () => {
    const order = createOrder({
      customerName: 'Ada Lovelace',
      customerEmail: 'ADA@example.com',
      items: [
        { productId: 'coffee-beans', quantity: 2 },
        { productId: 'travel-mug', quantity: 1 }
      ]
    });

    expect(order.customerEmail).toBe('ada@example.com');
    expect(order.total).toBe(51);
    expect(listOrders()).toHaveLength(1);
  });

  it('updates an order status', () => {
    const order = createOrder({
      customerName: 'Grace Hopper',
      customerEmail: 'grace@example.com',
      items: [{ productId: 'brew-scale', quantity: 1 }]
    });

    const updated = updateOrderStatus(order.id, 'paid');

    expect(updated?.status).toBe('paid');
    expect(findOrder(order.id)?.status).toBe('paid');
  });

  it('cancels an order', () => {
    const order = createOrder({
      customerName: 'Katherine Johnson',
      customerEmail: 'katherine@example.com',
      items: [{ productId: 'travel-mug', quantity: 1 }]
    });

    expect(cancelOrder(order.id)).toBe(true);
    expect(findOrder(order.id)).toBeUndefined();
  });

  it('rejects unknown products', () => {
    expect(() => createOrder({
      customerName: 'Alan Turing',
      customerEmail: 'alan@example.com',
      items: [{ productId: 'unknown', quantity: 1 }]
    })).toThrow('Unknown product: unknown');
  });
});
