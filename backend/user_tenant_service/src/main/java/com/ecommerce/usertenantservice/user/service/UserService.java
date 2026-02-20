package com.ecommerce.usertenantservice.user.service;

import com.ecommerce.usertenantservice.integration.payment.PaymentServiceClientAdapter;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.user.entity.User;
import com.ecommerce.usertenantservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PaymentServiceClientAdapter paymentServiceClientAdapter;

    public UserService(UserRepository userRepository, PaymentServiceClientAdapter paymentServiceClientAdapter) {
        this.userRepository = userRepository;
        this.paymentServiceClientAdapter = paymentServiceClientAdapter;
    }

    @Transactional
    public User syncUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User syncUserUpdate(User user, UUID keycloakId) {
        User oldUser = userRepository.findByKeycloakId(keycloakId);
        if (user != null) {
            oldUser.setEmail(user.getEmail());
            oldUser.setFirstName(user.getFirstName());
            oldUser.setLastName(user.getLastName());
            userRepository.save(oldUser);
            return user;
        }else  {
            return null;
        }
    }

    @Transactional
    public User deleteUser(UUID keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId);
        user.setIsActive(false);
        userRepository.save(user);
        return user;
    }

    public User getExistingUser(UUID keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    public Optional<User> getByEmail(String email){
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateUser(User user) {
        try{
            userRepository.save(user);
            return user;
        }catch (Exception e){
            log.error("Error while saving user {}", user, e);
            return null;
        }
    }

    public Page<PaymentHistoryResponse> getUserPaymentHistory(UUID keycloakId, Pageable pageable){
        User user = userRepository.findByKeycloakId(keycloakId);
        return paymentServiceClientAdapter.getUserPaymentHistory(user.getId(), pageable);
    }

}
