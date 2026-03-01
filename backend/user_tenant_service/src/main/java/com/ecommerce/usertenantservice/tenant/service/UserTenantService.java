package com.ecommerce.usertenantservice.tenant.service;

import com.ecommerce.usertenantservice.tenant.entity.UserTenant;
import com.ecommerce.usertenantservice.tenant.repository.UserTenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserTenantService {

    private final UserTenantRepository userTenantRepository;

    public UserTenantService(UserTenantRepository userTenantRepository) {
        this.userTenantRepository = userTenantRepository;
    }

    @Transactional
    public void save(UserTenant userTenant){
        userTenantRepository.save(userTenant);
    }

    @Transactional
    public void delete(UserTenant userTenant){
        userTenantRepository.delete(userTenant);
    }

    public Optional<UserTenant> findByUserIdAndIsActiveTrue(Long id){
        return userTenantRepository.findByUserIdAndIsActiveTrue(id);
    }

    public boolean existsByUserIdAndIsActiveTrue(Long id){
        return userTenantRepository.existsByUserIdAndIsActiveTrue(id);
    }

    public List<UserTenant> findAllMyMemberships(UUID keycloakId){
        return userTenantRepository.findAllMyMemberships(keycloakId);
    }

    public boolean existsByUserIdAndTenantId(Long userId, Long tenantId){
        return userTenantRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    public Page<UserTenant> findByTenantId(Long tenantId, Pageable pageable){
        return userTenantRepository.findByTenantId(tenantId, pageable);
    }

    public Optional<UserTenant> findById(Long memberId){
        return userTenantRepository.findById(memberId);
    }

}
