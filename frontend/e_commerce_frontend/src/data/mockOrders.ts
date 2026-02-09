import type {Order} from '../types';
import { OrderStatus } from '../types/enums';

export const mockOrders: Order[] = [
    { id: 'ORD1001', date: '2025-10-15', total: 450.00, status: OrderStatus.COMPLETED },
    { id: 'ORD1002', date: '2025-10-18', total: 120.50, status: OrderStatus.SHIPPED },
    { id: 'ORD1003', date: '2025-10-20', total: 75.90, status: OrderStatus.PROCESSING },
];