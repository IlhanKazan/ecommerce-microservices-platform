/**
 * Bu dosya artık yalnızca routing katmanı olarak görev yapar.
 * - MerchantProducts  → gerçek implementasyon: MerchantProductsPage
 * - MerchantOrders    → stub (Order Service henüz yazılmadı)
 * - MerchantReviews   → stub
 *
 * Depo yönetimi (Warehouse), MerchantProducts sekmesinden kaldırılıp
 * MerchantWarehousePage'e taşındı. /merchant/warehouses route'u eklenirse
 * App.tsx'te lazy import yapılabilir.
 */
export { default as MerchantProducts } from './MerchantProductsPage';
export { default as MerchantWarehouse } from './MerchantWarehousePage';
export { default as MerchantReviews } from './MerchantReviewsPage';
export { MerchantOrders } from './MerchantOrdersStub';