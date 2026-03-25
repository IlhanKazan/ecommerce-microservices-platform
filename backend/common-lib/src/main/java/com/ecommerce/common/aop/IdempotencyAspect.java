package com.ecommerce.common.aop;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return joinPoint.proceed();

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (!StringUtils.hasText(idempotencyKey)) {
            log.warn("Idempotency-Key eksik! Header bulunamadı.");
            throw new BusinessException("Sistem güvenliği için Idempotency-Key header'ı eksik!", "MISSING_IDEMPOTENCY_KEY");
        }

        String redisKey = idempotent.cachePrefix() + idempotencyKey;

        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofSeconds(idempotent.ttlSeconds()));

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.warn("Idempotency İhlali! Key: {}", idempotencyKey);
            throw new BusinessException("Bu işlem zaten gerçekleştirildi veya işleniyor. Lütfen bekleyin.", "DUPLICATE_REQUEST");
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }
}