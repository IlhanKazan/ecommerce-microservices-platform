export interface AdminCategory {
    id: number;
    name: string;
    slug: string;
    description: string | null;
    imageUrl: string | null;
    icon: string | null;
    level: number;
    fullPath: string;
    displayOrder: number | null;
    isActive: boolean;
    parentId: number | null;
    subCategories: AdminCategory[];
}

export interface CreateCategoryRequest {
    name: string;
    description?: string | null;
    imageUrl?: string | null;
    icon?: string | null;
    displayOrder?: number | null;
    isActive?: boolean;
    parentId?: number | null;
}

export interface UpdateCategoryRequest {
    name?: string | null;
    description?: string | null;
    imageUrl?: string | null;
    icon?: string | null;
    displayOrder?: number | null;
    isActive?: boolean;
}

// ── Orders ──────────────────────────────────────────────────────────────
export interface AdminOrderSummary {
    orderId: number;
    tenantId: number;
    userId: string;
    buyerEmail: string | null;
    status: string;
    totalAmount: number;
    currency: string;
    createdAt: string;
}

export interface AdminOrderItem {
    productId: number;
    sku: string | null;
    productName: string;
    productImageUrl: string | null;
    unitPrice: number;
    quantity: number;
}

export interface AdminOrderDetail extends AdminOrderSummary {
    shippingAddressJson: string | null;
    cancellationReason: string | null;
    items: AdminOrderItem[];
}

export interface AdminOrderStats {
    totalOrders: number;
    totalGmv: number;
    byStatus: Record<string, number>;
}

// ── Users ───────────────────────────────────────────────────────────────
export interface AdminUser {
    id: number;
    email: string;
    firstName: string | null;
    lastName: string | null;
    profileImageUrl: string | null;
    isActive: boolean;
    createdAt: string;
}

// ── Payments / Gelir ──────────────────────────────────────────────────────
export interface AdminMonthlyRevenue {
    month: string;          // 'YYYY-MM'
    subscription: number;
    commission: number;
}

export interface AdminPaymentStats {
    commissionRevenue: number;
    subscriptionRevenue: number;
    totalRevenue: number;
    productPaymentCount: number;
    subscriptionPaymentCount: number;
    monthly: AdminMonthlyRevenue[];
}

export interface AdminProductStats {
    totalProducts: number;
    activeProducts: number;
}

export interface AdminProduct {
    id: number;
    tenantId: number;
    categoryId: number | null;
    name: string;
    sku: string;
    price: number;
    currency: string;
    status: string;
    salesStatus: string;
    mainImageUrl: string | null;
    createdAt: string;
}

// ── Transactions (ödemeler) ───────────────────────────────────────────────
export interface AdminTransaction {
    id: number;
    orderId: number | null;
    subscriptionId: number | null;
    tenantId: number | null;
    tenantName: string | null;
    customerId: number | null;
    buyerEmail: string | null;
    buyerName: string | null;
    paymentType: string;
    amount: number;
    commissionAmount: number | null;
    commissionRate: number | null;
    netAmount: number | null;
    refundedAmount: number | null;
    paymentStatus: string;
    paymentMethod: string | null;
    iyzicoTransactionId: string | null;
    paidAt: string | null;
    createdAt: string;
}
