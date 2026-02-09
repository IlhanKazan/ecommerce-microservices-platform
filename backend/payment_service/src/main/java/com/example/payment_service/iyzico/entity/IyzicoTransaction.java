package com.example.payment_service.iyzico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "iyzico_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IyzicoTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "iyzico_txn_id")
    private String iyzicoTxnId;

    @Column(name = "payment_card_hash")
    private String paymentCardHash;

    @Column(columnDefinition = "TEXT")
    private String rawRequest;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    private LocalDateTime transactionDate;
    private String status;
    private String errorCode;
    private String errorMessage;

    private String cardLastFour;
    private String cardType;
    private String cardAssociation;
    private String cardFamily;
    private Integer installment;
}
