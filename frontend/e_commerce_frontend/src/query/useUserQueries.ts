import { useQuery, useQueryClient } from '@tanstack/react-query';
import { userService } from '../features/user/api/userService.ts';

export const USER_QUERY_KEYS = {
    me: ['user', 'me'] as const,
    all: ['user'] as const,
};

/**
 * Fetch authenticated user profile data
 * Only runs if the user is authenticated (caller must check)
 * Uses TanStack Query for caching and auto-refetching
 */
export const useMe = (enabled: boolean = true) => {
    return useQuery({
        queryKey: USER_QUERY_KEYS.me,
        queryFn: () => userService.getMe(),
        enabled: enabled,
        staleTime: 1000 * 60 * 5, // 5 minutes
        retry: 1,
    });
};

// Son gezilen ürün id'leri (giriş yapmış kullanıcı). "Son Gezdiklerin" rail'i için.
export const useRecentlyViewed = (enabled: boolean, limit = 12) => {
    return useQuery({
        queryKey: ['recently-viewed', limit],
        queryFn: () => userService.getRecentlyViewed(limit),
        enabled,
        staleTime: 1000 * 30,
    });
};

// Son aramalar (giriş yapmış kullanıcı). Arama çubuğu odaklanınca gösterilir.
export const useRecentSearches = (enabled: boolean, limit = 8) => {
    return useQuery({
        queryKey: ['recent-searches', limit],
        queryFn: () => userService.getRecentSearches(limit),
        enabled,
        staleTime: 1000 * 30,
    });
};

/**
 * Invalidate user profile cache when needed (after profile updates, login, etc)
 */
export const useInvalidateMe = () => {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: USER_QUERY_KEYS.me });
    };
};
