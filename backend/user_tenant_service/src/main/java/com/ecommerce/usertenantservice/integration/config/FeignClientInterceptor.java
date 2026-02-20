package com.ecommerce.usertenantservice.integration.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j; // Log için
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j // Logları aç
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Durum Kontrolü
        if (authentication == null) {
            log.warn("FEIGN INTERCEPTOR: Authentication is NULL! Token eklenemiyor.");
            return;
        }

        // 2. Nesne Tipi Kontrolü
        log.info("FEIGN INTERCEPTOR: Auth Class Type: {}", authentication.getClass().getName());

        // 3. Token Ekleme
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            String tokenValue = jwtToken.getToken().getTokenValue();

            template.header(AUTHORIZATION_HEADER, String.format("%s %s", TOKEN_TYPE, tokenValue));
            log.info("FEIGN INTERCEPTOR: Token eklendi! (Başlangıç: {}...)", tokenValue.substring(0, 10));
        } else {
            // Eğer buraya düşerse, senin Auth nesnen JwtAuthenticationToken değil demektir!
            log.error("FEIGN INTERCEPTOR: Authentication nesnesi JwtAuthenticationToken tipinde değil! Token eklenmedi.");
        }
    }
}