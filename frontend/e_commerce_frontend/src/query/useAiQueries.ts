import { useMutation, useQuery } from '@tanstack/react-query';
import { aiService } from '../features/ai/api/aiService';
import type {
    ChatRequest,
    StockInsight,
    TagSuggestRequest,
} from '../features/ai/types';

export const AI_REVIEW_SUMMARY_KEY = (productId: number) =>
    ['ai', 'review-summary', productId] as const;

/** Ürün AI yorum özeti — ProductDetailPage kartı için. */
export function useReviewSummary(productId: number | null, enabled = true) {
    return useQuery({
        queryKey: AI_REVIEW_SUMMARY_KEY(productId ?? 0),
        queryFn: () => aiService.getReviewSummary(productId as number),
        enabled: enabled && !!productId,
        staleTime: 1000 * 60 * 30, // 30 dk — backend ayrıca Redis'te cache'liyor
        retry: 1,
    });
}

/** Etiket önerisi (merchant). */
export function useSuggestTags() {
    return useMutation({
        mutationFn: (payload: TagSuggestRequest) => aiService.suggestTags(payload),
    });
}

/** Chatbot mesajı gönder. */
export function useChat() {
    return useMutation({
        mutationFn: (payload: ChatRequest) => aiService.chat(payload),
    });
}

/** Merchant stok insight. */
export function useStockInsights(tenantId: number | null, enabled = true) {
    return useQuery<StockInsight>({
        queryKey: ['ai', 'stock-insights', tenantId],
        queryFn: () => aiService.getStockInsights(tenantId as number),
        enabled: enabled && !!tenantId,
        staleTime: 1000 * 60 * 10,
        retry: 1,
    });
}
