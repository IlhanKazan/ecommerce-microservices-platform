import React, { useRef, useState } from 'react';
import {
    Paper, Typography, Box, Button, Stack, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, TextField,
} from '@mui/material';
import {
    PauseCircleOutline as PauseIcon,
    PlayCircleOutline as ResumeIcon,
    Block as CloseStoreIcon,
    WarningAmber as WarningIcon,
} from '@mui/icons-material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { tenantService } from '../api/tenantService';
import { useNotification } from '../../../components/shared/NotificationContext';
import { TENANT_QUERY_KEYS } from '../../../query/useTenantQueries';
import { generateIdempotencyKey } from '../../../utils/idempotencyUtils';
import { TenantStatus } from '../../../types/enums';
import type { TenantStatus as TenantStatusType } from '../../../types/tenant';

interface Props {
    tenantId: number;
    status: TenantStatusType;
}

const CONFIRM_WORD = 'KAPAT';

const StoreLifecycleSection: React.FC<Props> = ({ tenantId, status }) => {
    const queryClient = useQueryClient();
    const { notify } = useNotification();

    const [closeOpen, setCloseOpen] = useState(false);
    const [confirmText, setConfirmText] = useState('');
    const closeKeyRef = useRef<string>(generateIdempotencyKey());

    const refresh = () => {
        queryClient.invalidateQueries({ queryKey: ['tenant', tenantId] });
        queryClient.invalidateQueries({ queryKey: TENANT_QUERY_KEYS.myTenants });
    };

    const onError = (error: unknown, fallback: string) => {
        const e = error as { response?: { data?: { message?: string } }; message?: string };
        notify(e?.response?.data?.message || e?.message || fallback, 'error');
    };

    const pauseMutation = useMutation({
        mutationFn: () => tenantService.pauseTenant(tenantId),
        onSuccess: () => { refresh(); notify('Mağaza duraklatıldı. Ürünleriniz satıştan kaldırıldı.', 'success'); },
        onError: (e) => onError(e, 'Mağaza duraklatılamadı.'),
    });

    const resumeMutation = useMutation({
        mutationFn: () => tenantService.resumeTenant(tenantId),
        onSuccess: () => { refresh(); notify('Mağaza yeniden açıldı. Ürünleriniz tekrar satışta.', 'success'); },
        onError: (e) => onError(e, 'Mağaza açılamadı.'),
    });

    const closeMutation = useMutation({
        mutationFn: () => tenantService.closeTenant(tenantId, closeKeyRef.current),
        onSuccess: () => {
            closeKeyRef.current = generateIdempotencyKey();
            setCloseOpen(false);
            setConfirmText('');
            refresh();
            notify('Mağaza kalıcı olarak kapatıldı.', 'success');
        },
        onError: (e) => { onError(e, 'Mağaza kapatılamadı.'); },
    });

    const isActive = status === TenantStatus.ACTIVE;
    const isPaused = status === TenantStatus.PASSIVE;
    const busy = pauseMutation.isPending || resumeMutation.isPending;

    return (
        <Paper sx={{ p: 3, borderRadius: 4, borderColor: '#fca5a5' }} variant="outlined">
            <Typography variant="h6" sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1, color: 'error.main' }}>
                <WarningIcon /> Tehlikeli Bölge
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Mağazayı duraklatmak ürünlerinizi geçici olarak satıştan kaldırır; kapatmak ise kalıcıdır ve geri alınamaz.
            </Typography>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                {isActive && (
                    <Button
                        variant="outlined" color="warning" startIcon={<PauseIcon />}
                        disabled={busy} onClick={() => pauseMutation.mutate()}
                    >
                        Mağazayı Duraklat
                    </Button>
                )}
                {isPaused && (
                    <Button
                        variant="outlined" color="success" startIcon={<ResumeIcon />}
                        disabled={busy} onClick={() => resumeMutation.mutate()}
                    >
                        Mağazayı Yeniden Aç
                    </Button>
                )}
                {(isActive || isPaused) && (
                    <Button
                        variant="contained" color="error" startIcon={<CloseStoreIcon />}
                        disabled={busy} onClick={() => setCloseOpen(true)}
                    >
                        Mağazayı Kalıcı Kapat
                    </Button>
                )}
            </Stack>

            <Dialog open={closeOpen} onClose={() => !closeMutation.isPending && setCloseOpen(false)} maxWidth="xs" fullWidth>
                <DialogTitle sx={{ color: 'error.main', display: 'flex', alignItems: 'center', gap: 1 }}>
                    <WarningIcon /> Mağazayı Kalıcı Kapat
                </DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        Bu işlem <strong>geri alınamaz</strong>. Tüm ürünleriniz satıştan kaldırılacak ve mağaza panelinize
                        erişiminiz sonlanacak. Onaylamak için <strong>{CONFIRM_WORD}</strong> yazın.
                    </DialogContentText>
                    <TextField
                        autoFocus fullWidth size="small" value={confirmText}
                        onChange={(e) => setConfirmText(e.target.value)}
                        placeholder={CONFIRM_WORD}
                    />
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button color="inherit" onClick={() => setCloseOpen(false)} disabled={closeMutation.isPending}>
                        Vazgeç
                    </Button>
                    <Button
                        variant="contained" color="error"
                        disabled={confirmText !== CONFIRM_WORD || closeMutation.isPending}
                        startIcon={closeMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <CloseStoreIcon />}
                        onClick={() => closeMutation.mutate()}
                    >
                        Mağazayı Kapat
                    </Button>
                </DialogActions>
            </Dialog>
        </Paper>
    );
};

export default StoreLifecycleSection;
