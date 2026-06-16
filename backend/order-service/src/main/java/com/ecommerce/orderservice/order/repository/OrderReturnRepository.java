package com.ecommerce.orderservice.order.repository;

import com.ecommerce.orderservice.order.constant.ReturnStatus;
import com.ecommerce.orderservice.order.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    // Bir siparişin açık (REQUESTED) iade talebi — onay/red ve duplicate guard için
    Optional<OrderReturn> findByOrderIdAndStatus(Long orderId, ReturnStatus status);

    List<OrderReturn> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, ReturnStatus status);

    List<OrderReturn> findByStatusOrderByCreatedAtDesc(ReturnStatus status);
}
