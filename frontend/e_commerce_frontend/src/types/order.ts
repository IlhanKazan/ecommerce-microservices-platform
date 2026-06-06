export type OrderStatus = 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED';

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
