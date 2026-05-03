/**
 * Statistical summary of a numeric series.
 */
export interface NumberSeriesSummary {
  /** Number of values in the series. */
  count: number;
  /** Smallest value in the series. */
  minimum: number;
  /** Largest value in the series. */
  maximum: number;
  /** Sum of all values in the series. */
  total: number;
}

/**
 * Computes a statistical summary (count, min, max, total) for an array of numbers.
 * Returns all-zero values for an empty array.
 *
 * @param values - Array of numbers to summarize.
 * @returns A {@link NumberSeriesSummary} describing the series.
 */
export function summarizeNumberSeries(values: number[]): NumberSeriesSummary {
  if (values.length === 0) {
    return {
      count: 0,
      minimum: 0,
      maximum: 0,
      total: 0
    };
  }

  return values.reduce<NumberSeriesSummary>((summary, value) => ({
    count: summary.count + 1,
    minimum: Math.min(summary.minimum, value),
    maximum: Math.max(summary.maximum, value),
    total: summary.total + value
  }), {
    count: 0,
    minimum: values[0],
    maximum: values[0],
    total: 0
  });
}

/**
 * Normalizes an array of numbers to the [0, 1] range using min-max scaling.
 * Returns an array of zeros if the input is empty or all values are equal.
 *
 * @param values - Array of numbers to normalize.
 * @returns A new array with each value scaled to [0, 1].
 */
export function normalizeNumberSeries(values: number[]): number[] {
  const summary = summarizeNumberSeries(values);
  const range = summary.maximum - summary.minimum;

  if (values.length === 0 || range === 0) {
    return values.map(() => 0);
  }

  return values.map(value => (value - summary.minimum) / range);
}