import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
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
const frontendRoot = join(__dirname, 'frontend');
const frontendAssets: Record<string, { fileName: string; contentType: string }> = {
  '/': { fileName: 'index.html', contentType: 'text/html; charset=utf-8' },
  '/index.html': { fileName: 'index.html', contentType: 'text/html; charset=utf-8' },
  '/app.js': { fileName: 'app.js', contentType: 'text/javascript; charset=utf-8' },
  '/styles.css': { fileName: 'styles.css', contentType: 'text/css; charset=utf-8' }
};

/**
 * Writes a JSON response with the given HTTP status code.
 */
function sendJson(response: ServerResponse, statusCode: number, payload: unknown): void {
  response.writeHead(statusCode, { 'Content-Type': 'application/json' });
  response.end(JSON.stringify(payload));
}

async function sendFrontendAsset(path: string, response: ServerResponse): Promise<boolean> {
  const asset = frontendAssets[path];

  if (!asset) {
    return false;
  }

  const content = await readFile(join(frontendRoot, asset.fileName), 'utf8');
  response.writeHead(200, { 'Content-Type': asset.contentType });
  response.end(content);
  return true;
}

/**
 * Reads the full request body and parses it as JSON.
 * Returns an empty object when the body is absent.
 */
async function readJsonBody(request: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];

  for await (const chunk of request) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }

  const rawBody = Buffer.concat(chunks).toString('utf8');
  return rawBody ? JSON.parse(rawBody) : {};
}

/**
 * Routes an incoming HTTP request to the appropriate handler and writes the JSON response.
 * Responds with 400 on any thrown error and 404 when no route matches.
 */
async function handleRequest(request: IncomingMessage, response: ServerResponse): Promise<void> {
  const method = request.method || 'GET';
  const url = new URL(request.url || '/', `http://${request.headers.host || 'localhost'}`);
  const path = url.pathname;
  const orderMatch = path.match(/^\/orders\/([^/]+)$/);
  const statusMatch = path.match(/^\/orders\/([^/]+)\/status$/);

  try {
    if (method === 'GET' && await sendFrontendAsset(path, response)) {
      return;
    }

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

/** HTTP server instance for the Order API. */
export const server = createServer((request, response) => {
  void handleRequest(request, response);
});

if (require.main === module) {
  server.listen(port, () => {
    console.log(`Order API listening on port ${port}`);
  });
}
