# PR Documentation

## Overview

This PR introduces a working **Order API** (Node.js/TypeScript) and updates the two Jenkins pipelines that power the demo — `docs-generator` and `code-review`.

---

## Source Changes (`src/`)

### `src/utils.ts` — Order domain logic

Provides the in-memory data layer for a simple e-commerce order API.

| Export | Description |
|---|---|
| `listProducts()` | Returns a shallow copy of the product catalog (3 items: Coffee Beans, Travel Mug, Brew Scale). |
| `listOrders()` | Returns a deep copy of all current orders. |
| `findOrder(id)` | Finds an order by ID; returns `undefined` if not found. |
| `createOrder(payload)` | Validates input, resolves products, calculates totals, and appends a new `Order`. Normalises `customerEmail` to lower-case. |
| `updateOrderStatus(id, status)` | Transitions an order through `pending → paid → packed → shipped → cancelled`. |
| `cancelOrder(id)` | Removes an order by ID; returns `true` on success. |
| `resetOrders()` | Empties the order list (test helper). |

**Key types:** `OrderStatus`, `Product`, `Order`, `CreateOrderInput`, `OrderItem`, `OrderItemInput`.

**Validation rules enforced by `createOrder`:**
- `customerName` must be non-empty.
- `customerEmail` must contain `@`.
- `items` must be a non-empty array.
- Every `productId` must exist in the catalog; unknown IDs throw `Unknown product: <id>`.

### `src/index.ts` — HTTP server

A minimal `node:http` server that exposes the domain functions as a REST API.

| Method | Path | Handler |
|---|---|---|
| `GET` | `/health` | Returns `{ status: 'ok', uptime }`. |
| `GET` | `/products` | Returns the product catalog. |
| `GET` | `/orders` | Returns all orders. |
| `GET` | `/orders/:id` | Returns a single order or `404`. |
| `POST` | `/orders` | Creates a new order from a JSON body. |
| `PATCH` | `/orders/:id/status` | Updates the order status (`{ status: OrderStatus }`). |
| `DELETE` | `/orders/:id` | Cancels (removes) an order. |

Errors in request handling return HTTP `400` with `{ error: "<message>" }`.  
The server listens on `PORT` (default `3000`) and exports `server` for testing.

### `src/utils.test.ts` — Unit tests

Jest test suite covering the domain layer:

- Product catalog listing.
- Order creation with computed totals and email normalisation.
- Order status updates.
- Order cancellation.
- Rejection of unknown product IDs.

---

## Pipeline Changes

### `pipelines/docs-generator.jenkinsfile`

**New options / parameters:**

| Parameter | Default | Purpose |
|---|---|---|
| `DOC_TYPE` | `ALL` | Choose between `README`, `SOURCE_COMMENTS`, or `ALL`. |
| `PR_NUMBER` | _(empty)_ | Override the PR number; auto-detected in multibranch builds. |
| `WRITE_BACK_TO_PR` | `true` | Commit generated docs back to the PR branch when safe. |

**New / changed stages:**

| Stage | What it does |
|---|---|
| `🔧 Setup` | Installs GitHub Copilot CLI. |
| `🔎 PR Context` | Detects PR metadata, author permissions, and documentation candidates. |
| `📚 README` | Runs `generate-readme.mjs` (only when `DOC_TYPE` is `README` or `ALL`). |
| `💬 Source Comments` | Runs `generate-source-comments.mjs` (only when `DOC_TYPE` is `SOURCE_COMMENTS` or `ALL`). |
| `✅ Validate` | Validates generated documentation changes. |
| `⬆️ Write Back` | Commits docs back to the PR branch (only when `WRITE_BACK_TO_PR` is `true`). |
| `📣 PR Comment` | Posts a summary comment to the PR. |

`disableConcurrentBuilds(abortPrevious: true)` is now set to prevent race conditions on the same PR.  
Artifacts include `docs/*.md` in addition to `reports/*`.  
The HTML report is published as **Copilot Documentation Generator Report**.

**Environment:** `COPILOT_MODEL` is set to `claude-sonnet-4.6`.

---

### `pipelines/code-review.jenkinsfile`

Largely unchanged in structure. Key details:

- Requires a multibranch PR build (`CHANGE_ID` + `CHANGE_TARGET` must be set).
- Authenticates `git fetch` using `GH_TOKEN` to retrieve the base branch.
- **Stages:** PR Context → Setup → Fetch Base Branch → Files → Review → Summary → PR Comment.
- Report archived as **Copilot Code Review Report** (`reports/code-review.html`).
- `COPILOT_MODEL` is set to `claude-sonnet-4.6`.
