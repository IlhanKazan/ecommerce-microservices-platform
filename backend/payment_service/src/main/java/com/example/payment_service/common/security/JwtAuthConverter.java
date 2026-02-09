package com.example.payment_service.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtAuthConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final KeycloakConfig properties;

    @Value("${client_name}")
    private String clientName;

    public JwtAuthConverter(KeycloakConfig properties){
        this.properties = properties;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.addAll(defaultAuthoritiesConverter.convert(jwt));

        authorities.addAll(extractClientRoles(jwt));

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                resolvePrincipalClaimName(jwt)
        );
    }

    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        try {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) return Collections.emptyList();

            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientName);
            if (clientAccess == null) return Collections.emptyList();

            List<String> clientRoles = (List<String>) clientAccess.get("roles");
            if (clientRoles == null) return Collections.emptyList();

            System.out.println(clientRoles);

            return clientRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

        } catch (ClassCastException e) {
            return Collections.emptyList();
        }
    }

    private String resolvePrincipalClaimName(Jwt jwt) {
        String claimName = properties.getPrincipalAttribute() != null ?
                properties.getPrincipalAttribute() : JwtClaimNames.SUB;
        return jwt.getClaim(claimName);
    }
}

