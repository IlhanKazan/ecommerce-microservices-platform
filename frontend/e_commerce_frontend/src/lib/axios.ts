import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { USER_SERVICE_BASE_URL, PAYMENT_SERVICE_BASE_URL } from '../config/apiEndpoints';
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

export const userApi = axios.create({
    baseURL: USER_SERVICE_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});
addAuthInterceptor(userApi);


export const paymentApi = axios.create({
    baseURL: PAYMENT_SERVICE_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});
addAuthInterceptor(paymentApi);