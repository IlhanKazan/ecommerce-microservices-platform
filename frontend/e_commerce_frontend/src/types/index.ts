import type { OrderStatusType as OrderStatus } from './enums';

export interface Product {
    id: number;
    name: string;
    price: number;
    imageUrl: string;
    description: string;
    category: string;
    rating: number;
}

export interface PaginatedResult<T> {
    data: T[];
    totalElements: number;
    totalPages: number;
    page: number;
    pageSize: number;
}

export interface Order {
    id: string;
    date: string;
    total: number;
    status: OrderStatus;
}

export type { ProductSummary, ProductDetail, PageResponse, ProductSearchPayload } from './product';
export type { BasketItem, BasketResponse, AddItemRequest } from './basket';

