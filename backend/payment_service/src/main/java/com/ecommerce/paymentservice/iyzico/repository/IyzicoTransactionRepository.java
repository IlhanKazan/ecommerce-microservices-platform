package com.ecommerce.paymentservice.iyzico.repository;

import com.ecommerce.paymentservice.iyzico.entity.IyzicoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IyzicoTransactionRepository extends JpaRepository<IyzicoTransaction, Long> {
}
