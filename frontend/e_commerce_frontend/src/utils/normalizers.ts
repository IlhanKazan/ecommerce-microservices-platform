export const asRecord = (u: unknown): Record<string, unknown> => (u && typeof u === 'object') ? (u as Record<string, unknown>) : {};

export const getString = (raw: Record<string, unknown>, ...keys: string[]): string | undefined => {
  for (const k of keys) {
    const v = raw[k];
    if (typeof v === 'string') return v;
    if (typeof v === 'number') return String(v);
  }
  return undefined;
};

export const getNumber = (raw: Record<string, unknown>, ...keys: string[]): number | undefined => {
  for (const k of keys) {
    const v = raw[k];
    if (typeof v === 'number') return v;
    if (typeof v === 'string' && !Number.isNaN(Number(v))) return Number(v);
  }
  return undefined;
};

export const getBoolean = (raw: Record<string, unknown>, ...keys: string[]): boolean => {
  for (const k of keys) {
    const v = raw[k];
    if (typeof v === 'boolean') return v;
    if (typeof v === 'string') return v === 'true';
    if (typeof v === 'number') return v !== 0;
  }
  return false;
};
