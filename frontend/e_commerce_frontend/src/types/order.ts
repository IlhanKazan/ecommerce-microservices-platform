export type OrderStatus =
    | 'CONFIRMED'
    | 'SHIPPED'
    | 'DELIVERED'
    | 'CANCELLED'
    | 'REFUNDED'
    | 'RETURN_REQUESTED'
    | 'RETURNED'
    | 'RETURN_REJECTED';

/** İade talebi — merchant/admin paneli listesi. */
export interface OrderReturn {
    returnId: number;
    orderId: number;
    tenantId: number;
    reasonCode: string | null;
    reason: string | null;
    status: string;
    orderTotal: number | null;
    buyerEmail: string | null;
    createdAt: string;
}

export interface OrderItemDetail {
    productId: number;
    sku: string;
    productName: string;
    productImageUrl: string | null;
    unitPrice: number;
    quantity: number;
}

export interface OrderDetail {
    orderId: number;
    status: OrderStatus;
    totalAmount: number;
    currency: string;
    shippingAddressJson: string;
    createdAt: string;
    deliveredAt: string | null;
    items: OrderItemDetail[];
}

export interface CheckoutCardInfo {
    holderName: string;
    number: string;
    expireMonth: string;
    expireYear: string;
    cvc: string;
}

export interface CheckoutBuyer {
    id: string;
    name: string;
    surname: string;
    email: string;
    gsmNumber: string;
    identityNumber: string;
    ip: string;
    city: string;
    country: string;
    zipCode: string;
    fullAddress: string;
}

export interface CheckoutAddress {
    contactName: string;
    city: string;
    country: string;
    fullAddress: string;
    zipCode: string;
}

export interface CheckoutRequest {
    shippingAddressJson: string;
    cardInfo: CheckoutCardInfo;
    buyer: CheckoutBuyer;
    billingAddress: CheckoutAddress;
}

export interface OrderResponse {
    orderId: number;
    status: OrderStatus;
    totalAmount: number;
    currency: string;
    createdAt: string;
}

export interface OrderPageResponse<T> {
    content: T[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    isLast: boolean;
}

// ── Merchant satış analitiği ───────────────────────────────────────────────
export interface MerchantTopProduct {
    productId: number;
    productName: string;
    unitsSold: number;
    revenue: number;
    orderCount: number;
}

export interface MerchantAnalytics {
    totalRevenue: number;
    totalCommission: number;
    totalNet: number;
    totalOrders: number;
    totalUnits: number;
    topProducts: MerchantTopProduct[];
}

/** Tek ürünün satış metriği — toplam + varyant kırılımı (breakdown satırları MerchantTopProduct ile aynı şekil). */
export interface ProductSalesMetrics {
    productId: number;
    totalUnits: number;
    totalRevenue: number;
    totalOrders: number;
    breakdown: MerchantTopProduct[];
}
