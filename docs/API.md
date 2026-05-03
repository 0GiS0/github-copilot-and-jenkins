# API Reference — `src/utils.ts`

Utility functions exported from `src/utils.ts` for use throughout the application.

---

## `sum(numbers: number[]): number`

Returns the sum of all values in the array. Returns `0` for an empty array.

```ts
sum([1, 2, 3]) // 6
sum([])         // 0
```

---

## `average(numbers: number[]): number`

Returns the arithmetic mean of the array. Returns `0` for an empty array.

```ts
average([10, 20, 30]) // 20
average([])            // 0
```

---

## `formatDate(date: Date): string`

Formats a `Date` as `YYYY-MM-DD` (UTC).

```ts
formatDate(new Date('2024-03-15T10:30:00Z')) // '2024-03-15'
```

---

## `normalizeWhitespace(value: string): string`

Trims leading and trailing whitespace, then collapses any internal sequence of whitespace characters (spaces, tabs, newlines) into a single space.

```ts
normalizeWhitespace('  hello   from\njenkins  ') // 'hello from jenkins'
```

---

## `isValidEmail(email: string): boolean`

Returns `true` if `email` matches the pattern `local@domain.tld`. Rejects addresses missing a local part, domain, or TLD.

```ts
isValidEmail('user@example.com') // true
isValidEmail('no@domain')        // false
```

---

## `generateRandomString(length: number): string`

Returns a random alphanumeric string (`A-Z`, `a-z`, `0-9`) of the requested length. Not cryptographically secure.

```ts
generateRandomString(10) // e.g. 'aB3xKp91Qz'
```

---

## `deepClone<T>(obj: T): T`

Returns a deep copy of `obj` via `JSON.parse(JSON.stringify(...))`. Loses non-JSON-serialisable values (functions, `undefined`, `Date` objects, etc.).

```ts
const copy = deepClone({ a: 1, b: { c: 2 } })
// mutations to copy do not affect the original
```

---

## `debounce<T>(func: T, wait: number): (...args) => void`

Returns a debounced wrapper that delays invocation of `func` by `wait` milliseconds, resetting the timer on each call.

```ts
const onInput = debounce(handleSearch, 300)
```

---

## `retry<T>(fn: () => Promise<T>, maxRetries?: number, baseDelay?: number): Promise<T>`

Executes `fn`, retrying up to `maxRetries` times (default `3`) on failure with exponential backoff starting at `baseDelay` ms (default `1000`). Throws the last error when all attempts are exhausted.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxRetries` | `3` | Maximum number of attempts |
| `baseDelay` | `1000` | Initial delay in ms; doubles each retry |

```ts
const data = await retry(() => fetchData(), 3, 500)
```
