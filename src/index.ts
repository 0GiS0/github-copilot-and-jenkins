import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { URL } from 'node:url';
import {
  cancelOrder,
  createOrder,
  findOrder,
  listOrders,
  listProducts,
  OrderStatus,
  updateOrderStatus
} from './utils';

const port = Number(process.env.PORT || 3000);

function sendJson(response: ServerResponse, statusCode: number, payload: unknown): void {
  response.writeHead(statusCode, { 'Content-Type': 'application/json' });
  response.end(JSON.stringify(payload));
}

async function readJsonBody(request: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }

  const rawBody = Buffer.concat(chunks).toString('utf8');
  return rawBody ? JSON.parse(rawBody) : {};
}

async function handleRequest(request: IncomingMessage, response: ServerResponse): Promise<void> {
  const method = request.method || 'GET';
  const url = new URL(request.url || '/', `http://${request.headers.host || 'localhost'}`);
  const path = url.pathname;
  const orderMatch = path.match(/^\/orders\/([^/]+)$/);
  const statusMatch = path.match(/^\/orders\/([^/]+)\/status$/);

  try {
    if (method === 'GET' && path === '/health') {
      sendJson(response, 200, { status: 'ok', uptime: process.uptime() });
      return;
    }

    if (method === 'GET' && path === '/products') {
      sendJson(response, 200, { products: listProducts() });
      return;
    }

    if (method === 'GET' && path === '/orders') {
      sendJson(response, 200, { orders: listOrders() });
      return;
    }

    if (method === 'GET' && orderMatch) {
      const order = findOrder(orderMatch[1]);
      sendJson(response, order ? 200 : 404, order || { error: 'Order not found' });
      return;
    }

    if (method === 'POST' && path === '/orders') {
      const order = createOrder(await readJsonBody(request));
      sendJson(response, 201, order);
      return;
    }

    if (method === 'PATCH' && statusMatch) {
      const body = await readJsonBody(request) as { status?: OrderStatus };
      const order = updateOrderStatus(statusMatch[1], body.status || 'pending');
      sendJson(response, order ? 200 : 404, order || { error: 'Order not found' });
      return;
    }

    if (method === 'DELETE' && orderMatch) {
      const deleted = cancelOrder(orderMatch[1]);
      sendJson(response, deleted ? 200 : 404, deleted ? { deleted: true } : { error: 'Order not found' });
      return;
    }

    sendJson(response, 404, { error: 'Route not found' });
  } catch (error) {
    sendJson(response, 400, { error: error instanceof Error ? error.message : 'Invalid request' });
  }
}

export const server = createServer((request, response) => {
  void handleRequest(request, response);
});

if (require.main === module) {
  server.listen(port, () => {
    console.log(`Order API listening on port ${port}`);
  });
}
