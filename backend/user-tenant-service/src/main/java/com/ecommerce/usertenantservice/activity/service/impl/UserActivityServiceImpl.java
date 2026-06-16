package com.ecommerce.usertenantservice.activity.service.impl;

import com.ecommerce.usertenantservice.activity.constant.ActivityType;
import com.ecommerce.usertenantservice.activity.entity.UserActivity;
import com.ecommerce.usertenantservice.activity.repository.UserActivityRepository;
import com.ecommerce.usertenantservice.activity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityServiceImpl implements UserActivityService {

    private static final int MAX_RECENT = 50;

    private final UserActivityRepository userActivityRepository;

    @Override
    @Transactional
    public void recordProductView(UUID userId, Long productId, Long tenantId) {
        if (productId == null) {
            return;
        }
        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .activityType(ActivityType.PRODUCT_VIEW)
                .productId(productId)
                .tenantId(tenantId)
                .build();
        userActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getRecentlyViewedProductIds(UUID userId, int limit) {
        int safeLimit = (limit <= 0 || limit > MAX_RECENT) ? 10 : limit;
        return userActivityRepository.findRecentlyViewedProductIds(
                userId, ActivityType.PRODUCT_VIEW, PageRequest.of(0, safeLimit));
    }

    @Override
    @Transactional
    public void recordSearch(UUID userId, String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        String trimmed = term.trim();
        if (trimmed.length() > 255) {
            trimmed = trimmed.substring(0, 255);
        }
        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .activityType(ActivityType.SEARCH)
                .searchTerm(trimmed)
                .build();
        userActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRecentSearches(UUID userId, int limit) {
        int safeLimit = (limit <= 0 || limit > MAX_RECENT) ? 10 : limit;
        return userActivityRepository.findRecentSearchTerms(
                userId, ActivityType.SEARCH, PageRequest.of(0, safeLimit));
    }
}
