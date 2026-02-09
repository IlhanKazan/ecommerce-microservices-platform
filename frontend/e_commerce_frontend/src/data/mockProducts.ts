import type {Product} from "../types";

export const MOCK_PRODUCTS: Product[] = [
    { id: 1, name: 'Minimalist Saat', price: 499.99, imageUrl: '/watch.jpg', description: '...', category: 'Aksesuar', rating: 4.5 },
    { id: 2, name: 'Deri Ceket', price: 1850.00, imageUrl: '/jacket.jpg', description: '...', category: 'Giyim', rating: 4.8 },
    { id: 3, name: 'Akıllı Termos', price: 299.50, imageUrl: '/thermos.jpg', description: '...', category: 'Elektronik', rating: 4.2 },
    { id: 4, name: 'Ergonomik Mouse', price: 199.00, imageUrl: '/mouse.jpg', description: '...', category: 'Elektronik', rating: 4.6 },
];