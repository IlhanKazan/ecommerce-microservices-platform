import { create } from 'zustand';

type ToastSeverity = 'success' | 'error' | 'warning' | 'info';

interface Toast {
    id: number;
    message: string;
    severity: ToastSeverity;
}

interface ToastState {
    toasts: Toast[];
    show: (message: string, severity?: ToastSeverity) => void;
    success: (message: string) => void;
    error: (message: string) => void;
    warning: (message: string) => void;
    dismiss: (id: number) => void;
}

let toastId = 0;

export const useToastStore = create<ToastState>((set) => ({
    toasts: [],

    show: (message, severity = 'info') => {
        const id = ++toastId;
        set((state) => ({
            toasts: [...state.toasts, { id, message, severity }]
        }));
        setTimeout(() => { 
            set((state) => ({ toasts: state.toasts.filter(t => t.id !== id) }));
        }, 4000);
    },

    success: (message) => useToastStore.getState().show(message, 'success'),
    error: (message) => useToastStore.getState().show(message, 'error'),
    warning: (message) => useToastStore.getState().show(message, 'warning'),

    dismiss: (id) => set((state) => ({
        toasts: state.toasts.filter(t => t.id !== id)
    })),
}));