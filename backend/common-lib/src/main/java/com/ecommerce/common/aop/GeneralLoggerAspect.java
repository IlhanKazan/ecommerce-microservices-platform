package com.ecommerce.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class GeneralLoggerAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // tüm mikroservislerin service katmanını dinleyecek
    @Around("execution(* com.ecommerce..*.service..*(..)) && !within(com.ecommerce.paymentservice..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Start -> Method: {}, Args: {}", methodName, args);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            log.error("Error -> Method: {}, Message: {}", methodName, e.getMessage());
            throw e;
        }

        long executionTime = System.currentTimeMillis() - start;
        log.info("End -> Method: {}, ResultType: {}, Time: {} ms",
                methodName,
                result != null ? result.getClass().getSimpleName() : "null",
                executionTime);

        return result;
    }
}