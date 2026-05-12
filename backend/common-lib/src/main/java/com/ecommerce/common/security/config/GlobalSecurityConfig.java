package com.ecommerce.common.security.config;

import com.ecommerce.common.security.converter.JwtAuthConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class GlobalSecurityConfig {

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    // Dev: sadece issuer-uri yeterli (OIDC discovery yapar).
    // Prod/container: jwk-set-uri + issuer-uri birlikte kullanılır —
    //   jwk-set-uri container DNS'inden JWK'ları çeker (discovery atlanır),
    //   issuer-uri dışa açık public URL olarak iss claim doğrulamasında kullanılır.
    @Value("${security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/categories/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthConverter)
                                .decoder(jwtDecoder())
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            if (issuerUri != null && !issuerUri.isBlank()) {
                // iss claim + exp/nbf validation — imza doğrulaması NimbusJwtDecoder içinde zaten var
                decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            }
            return decoder;
        }
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @PostConstruct
    public void enableSecurityContextPropagation() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

}
