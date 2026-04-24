export interface BasketItem {
    productId: number;
    productName: string;
    quantity: number;
    price: number;
    imageUrl: string | null;
}

export interface BasketResponse {
    userId: string;
    items: BasketItem[];
    totalPrice: number;
}

export interface AddItemRequest {
    productId: number;
    quantity: number;
}