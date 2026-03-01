import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/apiEndpoints';
import { useAuthStore } from '../store/useAuthStore';

const addAuthInterceptor = (instance: AxiosInstance) => {
    instance.interceptors.request.use(
        (config: InternalAxiosRequestConfig) => {
            const token = useAuthStore.getState().token;
            if (token && config.headers) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        },
        (error) => Promise.reject(error)
    );

    instance.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response?.status === 401) {
                console.error("Yetkisiz erişim! Token patlamış olabilir.");
                useAuthStore.getState().clearAuth();
            }
            return Promise.reject(error);
        }
    );
};

export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

addAuthInterceptor(api);