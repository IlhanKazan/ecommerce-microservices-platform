package com.example.payment_service.payment.service.impl;

import com.example.payment_service.iyzico.entity.IyzicoTransaction;
import com.example.payment_service.iyzico.service.IyzicoTransactionService;
import com.example.payment_service.payment.constant.PaymentStatus;
import com.example.payment_service.payment.constant.PaymentType;
import com.example.payment_service.payment.entity.Payment;
import com.example.payment_service.payment.domain.PaymentContext;
import com.example.payment_service.payment.repository.PaymentRepository;
import com.example.payment_service.payment.service.PaymentService;
import com.example.payment_service.payment.strategy.PaymentStrategy;
import com.example.payment_service.subscription.service.TenantSubscriptionService;
import com.iyzipay.Options;
import com.iyzipay.request.CreatePaymentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentStrategy> strategies;
    private final PaymentRepository paymentRepository;
    private final TenantSubscriptionService tenantSubscriptionService;
    private final Options iyzicoOptions;
    private final IyzicoTransactionService iyzicoTransactionService;

    @Override
    @Transactional
    public Payment processPayment(PaymentContext context) {
        PaymentStrategy strategy = findStrategy(context.getType());
        BigDecimal amount = strategy.calculatePrice(context.getReferenceId());

        Payment payment = Payment.builder()
                .tenantId(context.getTenantId())
                .customerId(context.getCustomerId())
                .amount(amount)
                .netAmount(amount)
                .currency("TRY")
                .paymentType(context.getType())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        if (context.getType() == PaymentType.SUBSCRIPTION) {
            payment.setSubscriptionId(context.getReferenceId());
        } else if (context.getType() == PaymentType.PRODUCT_ORDER) {
            payment.setOrderId(context.getReferenceId());
        }

        paymentRepository.save(payment);

        CreatePaymentRequest iyzicoRequest = strategy.prepareIyzicoRequest(payment, context);

        com.iyzipay.model.Payment iyzicoResponse = callIyzico(payment, iyzicoRequest);

        return handleIyzicoResponse(payment, iyzicoResponse, context);
    }

    @Override
    @Transactional
    public Payment processRenewalPayment(Long tenantId, String cardToken, BigDecimal amount) {
        PaymentStrategy strategy = findStrategy(PaymentType.SUBSCRIPTION);

        Payment payment = Payment.builder()
                .tenantId(tenantId)
                .amount(amount)
                .netAmount(amount)
                .currency("TRY")
                .paymentType(PaymentType.SUBSCRIPTION)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        CreatePaymentRequest iyzicoRequest = strategy.prepareRenewalRequest(payment, cardToken);

        com.iyzipay.model.Payment iyzicoResponse = callIyzico(payment, iyzicoRequest);

        return handleIyzicoResponse(payment, iyzicoResponse, null);
    }

    private PaymentStrategy findStrategy(PaymentType type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Desteklenmeyen ödeme tipi: " + type));
    }

    private Payment handleIyzicoResponse(Payment payment, com.iyzipay.model.Payment iyzicoResponse, PaymentContext context) {
        if ("success".equalsIgnoreCase(iyzicoResponse.getStatus())) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            if (context != null && context.getType() == PaymentType.SUBSCRIPTION) {
                tenantSubscriptionService.createActiveSubscription(
                        context.getTenantId(),
                        context.getReferenceId(),
                        iyzicoResponse.getCardToken(),
                        payment.getAmount()
                );
            }

            return paymentRepository.save(payment);

        } else {
            payment.setPaymentStatus(PaymentStatus.FAILURE);
            payment.setFailureReason(iyzicoResponse.getErrorMessage());
            payment.setFailureCode(iyzicoResponse.getErrorCode());
            payment.setFailedAt(LocalDateTime.now());

            paymentRepository.save(payment);

            // Burada try-catch ile özel exception da fırlatılabilir, ya da enum döndürüp çağıran metotta kontrol sağlarız

            return payment;
        }
    }

    private void saveTransactionLogs(Payment payment, CreatePaymentRequest request, com.iyzipay.model.Payment iyzicoResponse) {
        IyzicoTransaction transaction = IyzicoTransaction.builder()
                .paymentId(payment.getId())
                .iyzicoTxnId(iyzicoResponse.getPaymentId())
                .cardLastFour(iyzicoResponse.getLastFourDigits())
                .cardType(iyzicoResponse.getCardType())
                .cardAssociation(iyzicoResponse.getCardAssociation())
                .cardFamily(iyzicoResponse.getCardFamily())
                .installment(iyzicoResponse.getInstallment())
                .transactionDate(LocalDateTime.now())
                .status(iyzicoResponse.getStatus())
                .errorCode(iyzicoResponse.getErrorCode())
                .errorMessage(iyzicoResponse.getErrorMessage())
                // ToString yerine JSON çevirici kullanılabilir
                // TODO [28.12.2025 06:04]: requestteki kart bilgileri gibi hassas veriler maskelenecek !!!!!!
                .rawRequest(request.toString())
                .rawResponse("iyzico entitysinde rawresponse degiskeni olmadigindan dolayi burasi bos")

                // Kartın son 4 hanesi
                .cardLastFour(request.getPaymentCard() != null ? request.getPaymentCard().getCardNumber().substring(12) : null)
                .build();

        // REQUIRES_NEW olduğu için burası ana işlemden bağımsız commitlenir
        IyzicoTransaction response = iyzicoTransactionService.save(transaction);
        log.info("Iyzico txn object: {}", response);
    }


    // Tek bir yerden yönetildiği için loglama unutulmuyor
    private com.iyzipay.model.Payment callIyzico(Payment payment, CreatePaymentRequest request) {
        // Iyzıcoya isteği burada geçiyoruz
        com.iyzipay.model.Payment response = com.iyzipay.model.Payment.create(request, iyzicoOptions);
        saveTransactionLogs(payment, request, response);

        return response;
    }
}