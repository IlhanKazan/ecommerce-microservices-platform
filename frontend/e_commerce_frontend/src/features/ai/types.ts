// AI Service tipleri — backend-ai (FastAPI) yanıtlarıyla eşleşir.

export type Sentiment = 'POSITIVE' | 'NEGATIVE' | 'MIXED' | 'NEUTRAL';

export interface ReviewSummary {
    productId: number;
    reviewCount: number;
    averageRating: number | null;
    overallSentiment: Sentiment | null;
    summary: string | null;
    pros: string[];
    cons: string[];
    keywords: string[];
    cached: boolean;
}

export interface TagSuggestRequest {
    title: string;
    description?: string;
    category?: string;
}

export interface TagSuggestResponse {
    tags: string[];
}

export interface ChatRequest {
    message: string;
    sessionId?: string | null;
}

export interface ChatResponse {
    sessionId: string;
    message: string;
    usedTools: string[];
}

export interface ProductStockSignal {
    productId: number;
    productName: string | null;
    unitsSold: number;
    availableQuantity: number | null;
    inStock: boolean | null;
    suggestion: string | null;
}

export interface StockInsight {
    tenantId: number;
    narrative: string | null;
    signals: ProductStockSignal[];
}

// Chat UI mesajı (client-side)
export interface ChatUiMessage {
    role: 'user' | 'assistant';
    content: string;
}
