package com.ecommerce.usertenantservice.integration.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_TYPE = "Bearer";

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("FEIGN INTERCEPTOR: Authentication is NULL! Token eklenemiyor.");
            return;
        }

        log.info("FEIGN INTERCEPTOR: Auth Class Type: {}", authentication.getClass().getName());

        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            String tokenValue = jwtToken.getToken().getTokenValue();

            template.header(AUTHORIZATION_HEADER, String.format("%s %s", TOKEN_TYPE, tokenValue));
            log.info("FEIGN INTERCEPTOR: Token eklendi! (Başlangıç: {}...)", tokenValue.substring(0, 10));
        } else {
            log.error("FEIGN INTERCEPTOR: Authentication nesnesi JwtAuthenticationToken tipinde değil! Token eklenmedi.");
        }
    }
}