import { api } from '../../../lib/axios';
import type {
    ReviewSummary,
    TagSuggestRequest,
    TagSuggestResponse,
    ChatRequest,
    ChatResponse,
    StockInsight,
} from '../types';

// Tüm yollar gateway baseURL'ine (/api/v1) görelidir.
export const aiService = {
    /** Ürün yorumlarının AI özeti (public). refresh=true cache atlar. */
    async getReviewSummary(productId: number, refresh = false): Promise<ReviewSummary> {
        const { data } = await api.get<ReviewSummary>(
            `/public/ai/reviews/${productId}/summary`,
            { params: refresh ? { refresh: true } : undefined },
        );
        return data;
    },

    /** Ürün etiketi önerisi (merchant, auth). */
    async suggestTags(payload: TagSuggestRequest): Promise<TagSuggestResponse> {
        const { data } = await api.post<TagSuggestResponse>(
            '/ai/products/suggest-tags',
            payload,
        );
        return data;
    },

    /** Alışveriş asistanı chatbot (auth). */
    async chat(payload: ChatRequest): Promise<ChatResponse> {
        const { data } = await api.post<ChatResponse>('/ai/chat', payload);
        return data;
    },

    /** Merchant stok insight (auth, tenant üyesi). */
    async getStockInsights(tenantId: number): Promise<StockInsight> {
        const { data } = await api.get<StockInsight>(
            `/ai/merchants/${tenantId}/stock-insights`,
        );
        return data;
    },

    /** Ürün görüntüleme takibi (best-effort, auth opsiyonel). */
    async trackView(productId: number, tenantId: number): Promise<void> {
        try {
            await api.post('/public/ai/track/view', { productId, tenantId });
        } catch {
            // sessizce yut — tracking kritik değil
        }
    },
};
