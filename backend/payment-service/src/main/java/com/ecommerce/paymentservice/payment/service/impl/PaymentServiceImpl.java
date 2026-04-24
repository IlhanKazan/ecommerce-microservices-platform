package com.ecommerce.paymentservice.payment.service.impl;

import com.ecommerce.paymentservice.iyzico.entity.IyzicoTransaction;
import com.ecommerce.paymentservice.iyzico.service.IyzicoTransactionService;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.repository.PaymentRepository;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.payment.strategy.PaymentStrategy;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.service.TenantSubscriptionService;
import com.iyzipay.Options;
import com.iyzipay.request.CreatePaymentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final List<PaymentStrategy> strategies;
    private final PaymentRepository paymentRepository;
    private final TenantSubscriptionService tenantSubscriptionService;
    private final Options iyzicoOptions;
    private final IyzicoTransactionService iyzicoTransactionService;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public Payment processPayment(PaymentContext context) {

        // Idompotency burada saglaniyor
        if (context.getType() == PaymentType.SUBSCRIPTION) {
            Optional<TenantSubscription> existingSub = tenantSubscriptionService.findLatestSubscription(context.getTenantId());

            if (existingSub.isPresent() && existingSub.get().getStatus() == TenantSubscriptionStatus.ACTIVE) {
                log.warn("DİKKAT! TenantId: {} için zaten AKTİF bir abonelik var. Iyzico'ya tekrar gidilmeyecek, süreç başarılı sayılacak.", context.getTenantId());

                Payment dummyPayment = new Payment();
                dummyPayment.setPaymentStatus(PaymentStatus.SUCCESS);
                dummyPayment.setAmount(existingSub.get().getFeeAmount());
                dummyPayment.setTenantId(context.getTenantId());
                dummyPayment.setPaymentType(PaymentType.SUBSCRIPTION);
                return dummyPayment;
            }
        }

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
                TenantSubscription sub = tenantSubscriptionService.createActiveSubscription(
                        context.getTenantId(),
                        context.getReferenceId(),
                        iyzicoResponse.getCardToken(),
                        payment.getAmount()
                );
                outboxService.publishSubscriptionActivatedEvent(sub);
            }

            Payment saved = paymentRepository.save(payment);
            outboxService.publishPaymentSuccessEvent(saved);
            return saved;

        } else {
            payment.setPaymentStatus(PaymentStatus.FAILURE);
            payment.setFailureReason(iyzicoResponse.getErrorMessage());
            payment.setFailureCode(iyzicoResponse.getErrorCode());
            payment.setFailedAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            outboxService.publishPaymentFailedEvent(saved);
            return saved;
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
        com.iyzipay.model.Payment response = com.iyzipay.model.Payment.create(request, iyzicoOptions);
        saveTransactionLogs(payment, request, response);

        return response;
    }
}
