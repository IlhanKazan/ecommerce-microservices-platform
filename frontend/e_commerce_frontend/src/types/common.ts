export interface ApiErrorResponse {
    message: string;
    path: string;
    statusCode: number;
    timestamp: string;
    errorCode: string;
    details?: Record<string, any>;
}