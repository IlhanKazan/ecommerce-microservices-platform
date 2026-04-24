import React from 'react';
import { Snackbar, Alert, Stack } from '@mui/material';
import { useToastStore } from '../../store/useToastStore';

const ToastContainer: React.FC = () => {
    const { toasts, dismiss } = useToastStore();

    return (
        <Stack
            spacing={1}
            sx={{
                position: 'fixed',
                bottom: 24,
                right: 24,
                zIndex: 9999,
                maxWidth: 400,
            }}
        >
            {toasts.map((toast) => (
                <Snackbar
                    key={toast.id}
                    open={true}
                    onClose={() => dismiss(toast.id)}
                    anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                    sx={{ position: 'static', transform: 'none' }}
                >
                    <Alert
                        severity={toast.severity}
                        onClose={() => dismiss(toast.id)}
                        variant="filled"
                        sx={{
                            width: '100%',
                            boxShadow: 3,
                            borderRadius: 2,
                        }}
                    >
                        {toast.message}
                    </Alert>
                </Snackbar>
            ))}
        </Stack>
    );
};

export default ToastContainer;