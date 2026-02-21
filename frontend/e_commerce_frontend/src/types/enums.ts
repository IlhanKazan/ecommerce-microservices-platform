export const BusinessType = {
  CORPORATE: 'CORPORATE',
  INDIVIDUAL: 'INDIVIDUAL'
} as const;
export type BusinessType = typeof BusinessType[keyof typeof BusinessType];

export const AddressType = {
  SHIPPING: 'SHIPPING',
  BILLING: 'BILLING'
} as const;
export type AddressType = typeof AddressType[keyof typeof AddressType];

export const TenantStatus = {
  ACTIVE: 'ACTIVE',
  CLOSED: 'CLOSED',
  PASSIVE: 'PASSIVE',
  SUSPENDED: 'SUSPENDED',
  PENDING_PAYMENT: 'PENDING_PAYMENT',
  PAYMENT_FAILED: 'PAYMENT_FAILED',

} as const;
export type TenantStatus = typeof TenantStatus[keyof typeof TenantStatus];

export const OrderStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SHIPPED: 'SHIPPED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
} as const;
export type OrderStatusType = typeof OrderStatus[keyof typeof OrderStatus];

export const TenantRole = {
  OWNER: 'OWNER',
  ADMIN: 'ADMIN',
  STAFF: 'STAFF',
  ACCOUNTANT: 'ACCOUNTANT'
} as const;
export type TenantRole = typeof TenantRole[keyof typeof TenantRole];
