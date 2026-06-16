package com.ecommerce.productservice.product.service;

import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.productservice.product.query.ProductValidationInfo;
import com.ecommerce.productservice.product.query.StockGroupInfo;

import java.util.List;

public interface InternalProductService {
    ProductValidationInfo validateAndGetProduct(Long productId, Long tenantId);
    int reindexAllProducts();
    void updateAiReport(Long productId, String aiReviewReport);

    // Satış metriği için: ürünün kendisi + tüm varyant id'leri (order-service agregasyonu çağırır)
    List<Long> getSalesAggregationIds(Long productId);

    // ES stok agregasyonu: stoğu değişen ürünün search-index hedefi + inStock'unu belirleyen id'ler
    // (stock-service çağırır; varyant stoğu parent dokümanına OR'lanır)
    StockGroupInfo resolveStockGroup(Long productId);

    // Popülerlik: sipariş onaylanınca satılan kalemlerin sale_count'unu artırır (katalog/parent bazında).
    void recordSales(List<OrderItemSnapshotPayload> items);
}