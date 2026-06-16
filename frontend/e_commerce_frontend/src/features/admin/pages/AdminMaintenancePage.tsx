import React, { useState } from 'react';
import {
    Box, Typography, Paper, Button, Stack, Alert, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Chip,
} from '@mui/material';
import {
    Refresh as RefreshIcon,
    Sync as SyncIcon,
    RestartAlt as RestartIcon,
    CleaningServices as CacheIcon,
} from '@mui/icons-material';
import {
    useReindexProducts, useResyncStocks, useReconcileReservations, useClearCaches,
} from '../../../query/useAdminQueries';
import { useNotification } from '../../../components/shared/NotificationContext';

interface MaintenanceAction {
    key: string;
    title: string;
    description: string;
    warning?: string;
    icon: React.ReactNode;
    buttonLabel: string;
    run: (cb: { onSuccess: (msg: string) => void; onError: (e: unknown) => void }) => void;
    isPending: boolean;
}

const AdminMaintenancePage: React.FC = () => {
    const { notify } = useNotification();
    const [confirmKey, setConfirmKey] = useState<string | null>(null);

    const reindex = useReindexProducts();
    const resync = useResyncStocks();
    const reconcile = useReconcileReservations();
    const clearCaches = useClearCaches();

    const errMsg = (e: unknown, fb: string) =>
        (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (e as { message?: string })?.message || fb;

    const actions: MaintenanceAction[] = [
        {
            key: 'reindex',
            title: 'Ürün Reindex',
            description: 'Tüm aktif ürünleri Elasticsearch arama indeksine yeniden yayar. Mağaza adı/logo veya arama verisi eksik/bozuksa kullan.',
            icon: <RefreshIcon />,
            buttonLabel: 'Reindex Et',
            run: (cb) => reindex.mutate(undefined, { onSuccess: cb.onSuccess, onError: cb.onError }),
            isPending: reindex.isPending,
        },
        {
            key: 'resync',
            title: 'Stok Resync',
            description: 'Elasticsearch’teki "inStock" bayraklarını veritabanındaki gerçek stokla yeniden senkronlar. Seed/elle stok girişi sonrası kartlarda yanlış "Tükendi" görünüyorsa kullan.',
            icon: <SyncIcon />,
            buttonLabel: 'Resync Et',
            run: (cb) => resync.mutate(undefined, { onSuccess: cb.onSuccess, onError: cb.onError }),
            isPending: resync.isPending,
        },
        {
            key: 'reconcile',
            title: 'Rezervasyon Reconcile',
            description: 'Yarıda kalan/sızmış stok rezervasyonlarını serbest bırakır (rezerve kalıp available’a dönmeyen miktarlar).',
            warning: 'Devam eden bir checkout (sipariş alımı) yokken çalıştırın — aksi halde işlenmekte olan rezervasyon da geri alınabilir.',
            icon: <RestartIcon />,
            buttonLabel: 'Reconcile Et',
            run: (cb) => reconcile.mutate(undefined, { onSuccess: cb.onSuccess, onError: cb.onError }),
            isPending: reconcile.isPending,
        },
        {
            key: 'clear-cache',
            title: 'Cache Temizle',
            description: 'Ürün ve mağaza servislerindeki tüm Redis cache’lerini temizler. Veritabanından elle veri değiştirdikten sonra (ör. mağaza doğrulama) eski/stale veri görünüyorsa kullan.',
            icon: <CacheIcon />,
            buttonLabel: 'Cache’i Temizle',
            run: (cb) => clearCaches.mutate(undefined, { onSuccess: cb.onSuccess, onError: cb.onError }),
            isPending: clearCaches.isPending,
        },
    ];

    const confirmAction = actions.find((a) => a.key === confirmKey);

    const handleConfirm = () => {
        if (!confirmAction) return;
        confirmAction.run({
            onSuccess: (msg) => notify(msg || 'İşlem tamamlandı.', 'success'),
            onError: (e) => notify(errMsg(e, 'İşlem başarısız oldu.'), 'error'),
        });
        setConfirmKey(null);
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight={700} gutterBottom>
                Sistem / Bakım
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Platform geneli manuel bakım operasyonları. Bu işlemler tüm mağazaları etkiler — dikkatli kullanın.
            </Typography>

            <Stack spacing={2} sx={{ maxWidth: 760 }}>
                {actions.map((a) => (
                    <Paper key={a.key} variant="outlined" sx={{ p: 2.5, borderRadius: 2 }}>
                        <Stack direction="row" spacing={2} alignItems="flex-start">
                            <Box sx={{ color: 'primary.main', mt: 0.5 }}>{a.icon}</Box>
                            <Box sx={{ flex: 1 }}>
                                <Typography fontWeight={700}>{a.title}</Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                                    {a.description}
                                </Typography>
                                {a.warning && (
                                    <Alert severity="warning" sx={{ mt: 1.5, py: 0.5 }}>
                                        {a.warning}
                                    </Alert>
                                )}
                            </Box>
                            <Button
                                variant="contained"
                                onClick={() => setConfirmKey(a.key)}
                                disabled={a.isPending}
                                sx={{ minWidth: 140, whiteSpace: 'nowrap' }}
                            >
                                {a.isPending
                                    ? <CircularProgress size={22} color="inherit" />
                                    : a.buttonLabel}
                            </Button>
                        </Stack>
                    </Paper>
                ))}
            </Stack>

            <Dialog open={!!confirmAction} onClose={() => setConfirmKey(null)} maxWidth="xs" fullWidth>
                <DialogTitle>
                    {confirmAction?.title}
                    <Chip label="Platform geneli" size="small" color="warning" sx={{ ml: 1 }} />
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>{confirmAction?.description}</DialogContentText>
                    {confirmAction?.warning && (
                        <Alert severity="warning" sx={{ mt: 2 }}>{confirmAction.warning}</Alert>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmKey(null)} color="inherit">İptal</Button>
                    <Button onClick={handleConfirm} variant="contained">Çalıştır</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminMaintenancePage;
