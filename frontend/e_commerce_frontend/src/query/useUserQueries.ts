import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from '../features/user/api/userService.ts';
import type { User as AppUser } from '../types/user';

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

/**
 * Invalidate user profile cache when needed (after profile updates, login, etc)
 */
export const useInvalidateMe = () => {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: USER_QUERY_KEYS.me });
    };
};
