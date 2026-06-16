package com.ecommerce.usertenantservice.tenant.controller.internal;

import com.ecommerce.usertenantservice.tenant.entity.Tenant;
import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servis-içi tenant lookup. Diğer servisler (ör. payment admin transaction listesi)
 * tenant id → ad eşlemesini tek çağrıda alır. Gateway'e açık değil; Feign ile çağrılır.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/internal/tenants")
@RequiredArgsConstructor
public class InternalTenantController {

    private final TenantRepository tenantRepository;

    @GetMapping("/names")
    public ResponseEntity<Map<Long, String>> getTenantNames(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }
        Map<Long, String> names = tenantRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        return ResponseEntity.ok(names);
    }
}
