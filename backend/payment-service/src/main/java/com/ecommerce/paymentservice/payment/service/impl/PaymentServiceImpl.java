package com.ecommerce.paymentservice.payment.service.impl;

import com.ecommerce.paymentservice.iyzico.entity.IyzicoTransaction;
import com.ecommerce.paymentservice.iyzico.service.IyzicoTransactionService;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.domain.PaymentContext;
import com.ecommerce.paymentservice.payment.domain.RefundResult;
import com.ecommerce.paymentservice.payment.repository.PaymentRepository;
import com.ecommerce.paymentservice.payment.domain.IyzicoStoredCard;
import com.ecommerce.paymentservice.payment.service.PaymentProvisioningFailureRecorder;
import com.ecommerce.paymentservice.payment.service.PaymentService;
import com.ecommerce.paymentservice.payment.strategy.PaymentStrategy;
import com.ecommerce.paymentservice.subscription.constant.TenantSubscriptionStatus;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.service.CardService;
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
    private final CardService cardService;

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

        // Alıcı bilgisini denormalize et (admin transaction listesinde "kim aldı")
        if (context.getBuyer() != null) {
            payment.setBuyerEmail(context.getBuyer().email());
            String fullName = ((context.getBuyer().name() != null ? context.getBuyer().name() : "") + " "
                    + (context.getBuyer().surname() != null ? context.getBuyer().surname() : "")).trim();
            payment.setBuyerName(fullName.isEmpty() ? null : fullName);
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
    public Payment processTokenCharge(Long tenantId, String cardToken, String cardUserKey, BigDecimal amount) {
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

        CreatePaymentRequest iyzicoRequest = strategy.prepareRenewalRequest(payment, cardToken, cardUserKey);

        com.iyzipay.model.Payment iyzicoResponse = callIyzico(payment, iyzicoRequest);

        return handleIyzicoResponse(payment, iyzicoResponse, null);
    }

    @Override
    @Transactional
    public RefundResult processRefund(Long orderId, String transactionId, BigDecimal amount, String kind) {
        // PRODUCT_ORDER'da Payment.orderId NULL'dur (ödeme sipariş persist'inden önce). Önce iyzico txn id ile bul.
        Optional<Payment> opt = (transactionId != null && !transactionId.isBlank())
                ? paymentRepository.findByIyzicoTransactionId(transactionId)
                : paymentRepository.findByOrderId(orderId);
        if (opt.isEmpty()) {
            log.warn("[REFUND] Ödeme kaydı bulunamadı — iade atlandı. OrderID: {}, txnId: {}", orderId, transactionId);
            return new RefundResult(false, "Ödeme kaydı bulunamadı.", null);
        }
        Payment payment = opt.get();
        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("[REFUND] OrderID: {} zaten REFUNDED — idempotent atlandı.", orderId);
            return new RefundResult(true, "Zaten iade edilmiş.", payment.getRefundedAmount());
        }

        BigDecimal refundAmount = amount != null ? amount : payment.getAmount();
        try {
            // İPTAL (aynı gün / settlement öncesi) → Cancel(paymentId). Başarısızsa Refund'a düş.
            if ("CANCEL".equalsIgnoreCase(kind)) {
                if (payment.getIyzicoTransactionId() == null) {
                    return refundViaRefundApi(payment, refundAmount);
                }
                com.iyzipay.model.Cancel cancel = doCancel(payment.getIyzicoTransactionId());
                if (!"success".equalsIgnoreCase(cancel.getStatus())) {
                    log.warn("[REFUND] Cancel başarısız ({}) — Refund'a düşülüyor. OrderID: {}",
                            cancel.getErrorMessage(), orderId);
                    return refundViaRefundApi(payment, refundAmount);
                }
                markRefunded(payment, refundAmount);
                log.info("[REFUND] iyzico Cancel başarılı. OrderID: {}", orderId);
                return new RefundResult(true, "İptal başarılı.", refundAmount);
            }
            // İADE → Refund(paymentTransactionId, tutar)
            return refundViaRefundApi(payment, refundAmount);
        } catch (Exception e) {
            log.error("[REFUND] iyzico iade hatası. OrderID: {}", orderId, e);
            payment.setFailureReason("İade hatası: " + e.getMessage());
            paymentRepository.save(payment);
            return new RefundResult(false, "İade işlemi başarısız: " + e.getMessage(), null);
        }
    }

    // iyzico Refund API ile iade. paymentTransactionId yoksa (eski kayıt) yalnız statü güncellenir.
    private RefundResult refundViaRefundApi(Payment payment, BigDecimal amount) {
        if (payment.getPaymentTransactionId() == null) {
            log.warn("[REFUND] paymentTransactionId yok (eski kayıt) → yalnız statü REFUNDED. OrderID: {}",
                    payment.getOrderId());
            markRefunded(payment, amount);
            return new RefundResult(true, "Eski kayıt — yalnız statü güncellendi.", amount);
        }
        com.iyzipay.model.Refund refund = doRefund(payment.getPaymentTransactionId(), amount, payment.getCurrency());
        if (!"success".equalsIgnoreCase(refund.getStatus())) {
            log.error("[REFUND] iyzico Refund reddetti. OrderID: {}, hata: {}",
                    payment.getOrderId(), refund.getErrorMessage());
            payment.setFailureReason("İade reddedildi: " + refund.getErrorMessage());
            paymentRepository.save(payment);
            return new RefundResult(false, "İade reddedildi: " + refund.getErrorMessage(), null);
        }
        markRefunded(payment, amount);
        log.info("[REFUND] iyzico Refund başarılı. OrderID: {}, tutar: {}", payment.getOrderId(), amount);
        return new RefundResult(true, "İade başarılı.", amount);
    }

    private void markRefunded(Payment payment, BigDecimal amount) {
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(amount);
        paymentRepository.save(payment);
    }

    private com.iyzipay.model.Cancel doCancel(String paymentId) {
        com.iyzipay.request.CreateCancelRequest req = new com.iyzipay.request.CreateCancelRequest();
        req.setLocale(com.iyzipay.model.Locale.TR.getValue());
        req.setConversationId(java.util.UUID.randomUUID().toString());
        req.setPaymentId(paymentId);
        req.setIp("127.0.0.1");
        return com.iyzipay.model.Cancel.create(req, iyzicoOptions);
    }

    private com.iyzipay.model.Refund doRefund(String paymentTransactionId, BigDecimal amount, String currency) {
        com.iyzipay.request.CreateRefundRequest req = new com.iyzipay.request.CreateRefundRequest();
        req.setLocale(com.iyzipay.model.Locale.TR.getValue());
        req.setConversationId(java.util.UUID.randomUUID().toString());
        req.setPaymentTransactionId(paymentTransactionId);
        req.setPrice(amount);
        req.setCurrency(currency != null ? currency : "TRY");
        req.setIp("127.0.0.1");
        return com.iyzipay.model.Refund.create(req, iyzicoOptions);
    }

    // iyzico response'undan ilk paymentItem'ın transaction id'si (Refund API bununla çalışır)
    private String firstPaymentTransactionId(com.iyzipay.model.Payment resp) {
        try {
            if (resp.getPaymentItems() != null && !resp.getPaymentItems().isEmpty()) {
                return resp.getPaymentItems().get(0).getPaymentTransactionId();
            }
        } catch (Exception e) {
            log.warn("paymentTransactionId çıkarılamadı: {}", e.getMessage());
        }
        return null;
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
            // İade için per-item transaction id'yi sakla (iyzico Refund API bununla çalışır)
            payment.setPaymentTransactionId(firstPaymentTransactionId(iyzicoResponse));

            if (context != null && context.getType() == PaymentType.SUBSCRIPTION) {
                try {
                    TenantSubscription sub = tenantSubscriptionService.createActiveSubscription(
                            context.getTenantId(),
                            context.getReferenceId(),
                            iyzicoResponse.getCardToken(),
                            iyzicoResponse.getCardUserKey(),
                            payment.getAmount(),
                            context.getContactEmail()
                    );
                    outboxService.publishSubscriptionActivatedEvent(sub);

                    // Abonelik ödemesinde registerCard ile saklanan kartı vault'a varsayılan olarak ekle.
                    // Best-effort: kart seed'i ödemeyi bozmamalı.
                    try {
                        cardService.seedDefaultCard(
                                context.getTenantId(),
                                context.getCustomerId(),
                                new IyzicoStoredCard(
                                        iyzicoResponse.getCardToken(),
                                        iyzicoResponse.getCardUserKey(),
                                        iyzicoResponse.getLastFourDigits(),
                                        iyzicoResponse.getCardAssociation(),
                                        iyzicoResponse.getCardFamily(),
                                        iyzicoResponse.getBinNumber()
                                )
                        );
                    } catch (Exception cardEx) {
                        log.warn("Abonelik kartı vault'a kaydedilemedi (ödeme etkilenmez): {}", cardEx.getMessage());
                    }
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
