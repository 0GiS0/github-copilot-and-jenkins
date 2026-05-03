export interface NumberSeriesSummary {
  count: number;
  minimum: number;
  maximum: number;
  total: number;
}

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

export function normalizeNumberSeries(values: number[]): number[] {
  const summary = summarizeNumberSeries(values);
  const range = summary.maximum - summary.minimum;

  if (values.length === 0 || range === 0) {
    return values.map(() => 0);
  }

  return values.map(value => (value - summary.minimum) / range);
}