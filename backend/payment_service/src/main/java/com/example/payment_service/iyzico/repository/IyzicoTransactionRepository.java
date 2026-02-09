package com.example.payment_service.iyzico.repository;

import com.example.payment_service.iyzico.entity.IyzicoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IyzicoTransactionRepository extends JpaRepository<IyzicoTransaction, Long> {
}
