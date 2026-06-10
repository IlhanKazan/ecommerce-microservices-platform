package com.ecommerce.paymentservice.payment.service.impl;

import com.ecommerce.paymentservice.iyzico.entity.IyzicoTransaction;
import com.ecommerce.paymentservice.iyzico.service.IyzicoTransactionService;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.repository.PaymentRepository;
import com.ecommerce.paymentservice.payment.service.PaymentProvisioningFailureRecorder;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.payment.strategy.PaymentStrategy;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.service.TenantSubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.Options;
import com.iyzipay.request.CreatePaymentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ObjectMapper objectMapper;
    private final PaymentProvisioningFailureRecorder provisioningFailureRecorder;

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
        BigDecimal amount;
        if (context.getAmount() != null && context.getType() == PaymentType.PRODUCT_ORDER) {
            amount = context.getAmount();
        } else {
            amount = strategy.calculatePrice(context.getReferenceId());
        }

        BigDecimal commissionRate = BigDecimal.ZERO;
        BigDecimal commissionAmount = BigDecimal.ZERO;
        BigDecimal netAmount = amount;

        if (context.getType() == PaymentType.PRODUCT_ORDER) {
            commissionRate = resolveCommissionRate(context.getTenantId(), context.getCommissionRate());
            commissionAmount = amount.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            netAmount = amount.subtract(commissionAmount);
            log.info("Komisyon hesaplandı — tenantId: {}, tutar: {}, oran: {}%, komisyon: {}, net: {}",
                    context.getTenantId(), amount, commissionRate, commissionAmount, netAmount);
        }

        Payment payment = Payment.builder()
                .tenantId(context.getTenantId())
                .customerId(context.getCustomerId())
                .amount(amount)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
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

        PaymentContext enrichedContext = PaymentContext.builder()
                .type(context.getType())
                .referenceId(context.getReferenceId())
                .customerId(context.getCustomerId())
                .tenantId(context.getTenantId())
                .cardInfo(context.getCardInfo())
                .buyer(context.getBuyer())
                .billingAddress(context.getBillingAddress())
                .shippingAddress(context.getShippingAddress())
                .amount(context.getAmount())
                .currency(context.getCurrency())
                .subMerchantKey(context.getSubMerchantKey())
                .commissionRate(commissionRate)
                .contactEmail(context.getContactEmail())
                .build();

        CreatePaymentRequest iyzicoRequest = strategy.prepareIyzicoRequest(payment, enrichedContext);

        com.iyzipay.model.Payment iyzicoResponse = callIyzico(payment, iyzicoRequest);

        return handleIyzicoResponse(payment, iyzicoResponse, enrichedContext);
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

    @Override
    @Transactional
    public void refundByOrderId(Long orderId, String transactionId) {
        paymentRepository.findByOrderId(orderId).ifPresentOrElse(payment -> {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setFailureReason("İade edildi");
            payment.setFailedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("[REFUND] OrderID: {} için ödeme REFUNDED olarak işaretlendi. PaymentID: {}",
                    orderId, payment.getId());
        }, () -> log.warn("[REFUND] OrderID: {} için ödeme kaydı bulunamadı — iade atlandı.", orderId));
    }

    private BigDecimal resolveCommissionRate(Long tenantId, BigDecimal contextRate) {
        if (contextRate != null && contextRate.compareTo(BigDecimal.ZERO) > 0) {
            return contextRate;
        }
        try {
            Optional<TenantSubscription> sub = tenantSubscriptionService.findLatestSubscription(tenantId);
            if (sub.isPresent() && sub.get().getCommissionRate() != null) {
                return sub.get().getCommissionRate();
            }
        } catch (Exception e) {
            log.warn("Komisyon oranı alınamadı (tenantId={}), varsayılan %8 kullanılıyor: {}", tenantId, e.getMessage());
        }
        return new BigDecimal("8.00");
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
            payment.setIyzicoTransactionId(iyzicoResponse.getPaymentId());

            if (context != null && context.getType() == PaymentType.SUBSCRIPTION) {
                try {
                    TenantSubscription sub = tenantSubscriptionService.createActiveSubscription(
                            context.getTenantId(),
                            context.getReferenceId(),
                            iyzicoResponse.getCardToken(),
                            payment.getAmount(),
                            context.getContactEmail()
                    );
                    outboxService.publishSubscriptionActivatedEvent(sub);
                } catch (Exception e) {
                    // iyzico parayı çekti ama abonelik provisioning'i patladı.
                    // Para izini AYRI tx'te (REQUIRES_NEW) kalıcı yaz, sonra ana tx'in rollback olmasına izin ver.
                    // Böylece çift provision olmaz, UTS tenant'ı PENDING'de tutar, para sessizce kaybolmaz.
                    log.error("KRİTİK: iyzico ödeme başarılı ama abonelik provisioning patladı. tenantId={}, iyzicoTxnId={}",
                            context.getTenantId(), iyzicoResponse.getPaymentId(), e);
                    provisioningFailureRecorder.recordProvisionFailure(
                            context.getTenantId(),
                            context.getCustomerId(),
                            context.getReferenceId(),
                            payment.getAmount(),
                            iyzicoResponse.getPaymentId(),
                            e.getMessage()
                    );
                    throw e;
                }
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
                .rawRequest(maskCardData(request))
                .rawResponse("iyzico entitysinde rawresponse degiskeni olmadigindan dolayi burasi bos")

                // Kartın son 4 hanesi
                .cardLastFour(request.getPaymentCard() != null ? request.getPaymentCard().getCardNumber().substring(12) : null)
                .build();

        // REQUIRES_NEW olduğu için burası ana işlemden bağımsız commitlenir
        IyzicoTransaction response = iyzicoTransactionService.save(transaction);
        log.info("Iyzico txn object: {}", response);
    }


    private String maskCardData(CreatePaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return json.replaceAll("\"cardNumber\"\\s*:\\s*\"(\\d{6})\\d+(\\d{4})\"",
                                   "\"cardNumber\":\"$1******$2\"")
                       .replaceAll("\"cvc\"\\s*:\\s*\"\\d+\"", "\"cvc\":\"***\"");
        } catch (Exception e) {
            log.warn("Kart verisi maskeleme hatası: {}", e.getMessage());
            return "[MASKED - serialization error]";
        }
    }

    private com.iyzipay.model.Payment callIyzico(Payment payment, CreatePaymentRequest request) {
        com.iyzipay.model.Payment response = com.iyzipay.model.Payment.create(request, iyzicoOptions);
        log.info("iyzico yanıtı — status: {}, paymentId: {}, error: {}",
                response.getStatus(), response.getPaymentId(), response.getErrorMessage());
        try {
            saveTransactionLogs(payment, request, response);
        } catch (Exception e) {
            log.error("IyzicoTransaction log kaydı başarısız (ödeme etkilenmez): {}", e.getMessage(), e);
        }
        return response;
    }
}
