import type { OrderStatus } from '../types/order';

export const ORDER_STATUS_CONFIG: Record<
    OrderStatus,
    { label: string; color: 'warning' | 'info' | 'success' | 'default' | 'error' }
> = {
    CONFIRMED: { label: 'Onaylandı',      color: 'warning' },
    SHIPPED:   { label: 'Kargoda',         color: 'info' },
    DELIVERED: { label: 'Teslim Edildi',   color: 'success' },
    CANCELLED: { label: 'İptal Edildi',    color: 'default' },
    REFUNDED:  { label: 'İade Edildi',     color: 'error' },
};
