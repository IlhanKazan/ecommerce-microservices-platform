import React, { useMemo, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TablePagination, Chip, TextField, MenuItem, Stack, Button, Avatar, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, InputAdornment, Tooltip,
} from '@mui/material';
import {
    Search as SearchIcon,
    Block as SuspendIcon,
    PlayCircleOutline as ReactivateIcon,
    Visibility as ViewIcon,
    Store as StoreIcon,
    Verified as VerifiedIcon,
    GppMaybe as UnverifiedIcon,
} from '@mui/icons-material';
import { TenantStatus } from '../../../types/enums';
import type { TenantStatus as TenantStatusType, TenantSummary } from '../../../types/tenant';
import {
    useAdminStores, useAdminStoreDetail, useSuspendStore, useReactivateStore,
} from '../../../query/useAdminQueries';
import { useNotification } from '../../../components/shared/NotificationContext';
import EmptyState from '../../../components/shared/EmptyState';

const STATUS_META: Record<string, { label: string; color: 'success' | 'warning' | 'error' | 'default' | 'info' }> = {
    ACTIVE: { label: 'Aktif', color: 'success' },
    PASSIVE: { label: 'Duraklatıldı', color: 'warning' },
    SUSPENDED: { label: 'Askıda', color: 'error' },
    CLOSED: { label: 'Kapalı', color: 'default' },
    PENDING_PAYMENT: { label: 'Ödeme Bekliyor', color: 'info' },
    PAYMENT_FAILED: { label: 'Ödeme Başarısız', color: 'error' },
};

const StatusChip: React.FC<{ status: string }> = ({ status }) => {
    const meta = STATUS_META[status] ?? { label: status, color: 'default' as const };
    return <Chip size="small" label={meta.label} color={meta.color} variant={meta.color === 'default' ? 'outlined' : 'filled'} />;
};

const AdminStoresPage: React.FC = () => {
    const { notify } = useNotification();

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [statusFilter, setStatusFilter] = useState<TenantStatusType | ''>('');
    const [verifiedFilter, setVerifiedFilter] = useState<'' | 'true' | 'false'>('');
    const [searchInput, setSearchInput] = useState('');
    const [appliedQ, setAppliedQ] = useState('');

    const [detailId, setDetailId] = useState<number | null>(null);
    const [confirm, setConfirm] = useState<{ action: 'suspend' | 'reactivate'; store: TenantSummary } | null>(null);

    const query = useMemo(
        () => ({
            page, size: rowsPerPage,
            status: statusFilter || null,
            verified: verifiedFilter === '' ? null : verifiedFilter === 'true',
            q: appliedQ || null,
        }),
        [page, rowsPerPage, statusFilter, verifiedFilter, appliedQ],
    );
    const { data, isLoading, isError } = useAdminStores(query);
    const detail = useAdminStoreDetail(detailId);

    const suspend = useSuspendStore();
    const reactivate = useReactivateStore();
    const busy = suspend.isPending || reactivate.isPending;

    const applySearch = () => { setPage(0); setAppliedQ(searchInput.trim()); };

    const onError = (e: unknown, fallback: string) => {
        const err = e as { response?: { data?: { message?: string } }; message?: string };
        notify(err?.response?.data?.message || err?.message || fallback, 'error');
    };

    const runConfirm = () => {
        if (!confirm) return;
        const id = confirm.store.id;
        if (confirm.action === 'suspend') {
            suspend.mutate(id, {
                onSuccess: () => { notify('Mağaza askıya alındı.', 'success'); setConfirm(null); },
                onError: (e) => onError(e, 'Mağaza askıya alınamadı.'),
            });
        } else {
            reactivate.mutate(id, {
                onSuccess: () => { notify('Mağaza yeniden aktive edildi.', 'success'); setConfirm(null); },
                onError: (e) => onError(e, 'Mağaza aktive edilemedi.'),
            });
        }
    };

    const stores = data?.content ?? [];

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Mağaza Yönetimi</Typography>

            <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                    <TextField
                        size="small" label="Mağaza / işletme ara" value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applySearch(); }}
                        sx={{ flex: 1 }}
                        InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
                    />
                    <TextField
                        size="small" select label="Durum" value={statusFilter}
                        onChange={(e) => { setPage(0); setStatusFilter(e.target.value as TenantStatusType | ''); }}
                        sx={{ minWidth: 180 }}
                    >
                        <MenuItem value="">Tümü</MenuItem>
                        {Object.keys(STATUS_META).map((s) => (
                            <MenuItem key={s} value={s}>{STATUS_META[s].label}</MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        size="small" select label="Doğrulama" value={verifiedFilter}
                        onChange={(e) => { setPage(0); setVerifiedFilter(e.target.value as '' | 'true' | 'false'); }}
                        sx={{ minWidth: 180 }}
                    >
                        <MenuItem value="">Tümü</MenuItem>
                        <MenuItem value="true">Doğrulanmış</MenuItem>
                        <MenuItem value="false">Doğrulanmamış</MenuItem>
                    </TextField>
                    <Button variant="contained" onClick={applySearch}>Ara</Button>
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Mağazalar yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : stores.length === 0 ? (
                    <EmptyState title="Mağaza bulunamadı" description="Filtrelere uyan mağaza yok." />
                ) : (
                    <>
                        <TableContainer>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Mağaza</TableCell>
                                        <TableCell>İşletme Adı</TableCell>
                                        <TableCell>Durum</TableCell>
                                        <TableCell>Doğrulama</TableCell>
                                        <TableCell align="right">İşlemler</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {stores.map((store) => {
                                        const canSuspend = store.status === TenantStatus.ACTIVE || store.status === TenantStatus.PASSIVE;
                                        const canReactivate = store.status === TenantStatus.SUSPENDED;
                                        return (
                                            <TableRow key={store.id} hover>
                                                <TableCell>
                                                    <Stack direction="row" spacing={1.5} alignItems="center">
                                                        <Avatar variant="rounded" src={store.logoUrl ?? undefined} sx={{ width: 36, height: 36, bgcolor: 'grey.200' }}>
                                                            <StoreIcon fontSize="small" />
                                                        </Avatar>
                                                        <Box>
                                                            <Typography variant="body2" fontWeight={600}>{store.name}</Typography>
                                                            <Typography variant="caption" color="text.secondary">#{store.id}</Typography>
                                                        </Box>
                                                    </Stack>
                                                </TableCell>
                                                <TableCell>{store.businessName}</TableCell>
                                                <TableCell><StatusChip status={store.status} /></TableCell>
                                                <TableCell>
                                                    {store.isVerified ? (
                                                        <Chip size="small" color="success" variant="outlined"
                                                              icon={<VerifiedIcon />} label="Doğrulanmış" />
                                                    ) : (
                                                        <Chip size="small" color="default" variant="outlined"
                                                              icon={<UnverifiedIcon />} label="Doğrulanmamış" />
                                                    )}
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                                                        <Tooltip title="Detay">
                                                            <Button size="small" startIcon={<ViewIcon />} onClick={() => setDetailId(store.id)}>Detay</Button>
                                                        </Tooltip>
                                                        {canSuspend && (
                                                            <Button
                                                                size="small" color="error" variant="outlined" startIcon={<SuspendIcon />}
                                                                disabled={busy} onClick={() => setConfirm({ action: 'suspend', store })}
                                                            >Askıya Al</Button>
                                                        )}
                                                        {canReactivate && (
                                                            <Button
                                                                size="small" color="success" variant="outlined" startIcon={<ReactivateIcon />}
                                                                disabled={busy} onClick={() => setConfirm({ action: 'reactivate', store })}
                                                            >Reaktive Et</Button>
                                                        )}
                                                    </Stack>
                                                </TableCell>
                                            </TableRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <TablePagination
                            component="div"
                            count={data?.totalElements ?? 0}
                            page={page}
                            onPageChange={(_, p) => setPage(p)}
                            rowsPerPage={rowsPerPage}
                            onRowsPerPageChange={(e) => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
                            rowsPerPageOptions={[10, 25, 50]}
                            labelRowsPerPage="Sayfa başına"
                        />
                    </>
                )}
            </Paper>

            {/* Detay modal */}
            <Dialog open={detailId != null} onClose={() => setDetailId(null)} maxWidth="sm" fullWidth>
                <DialogTitle>Mağaza Detayı</DialogTitle>
                <DialogContent dividers>
                    {detail.isLoading ? (
                        <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                    ) : detail.data ? (
                        <Stack spacing={1.5}>
                            <Row label="Mağaza Adı" value={detail.data.name} />
                            <Row label="İşletme Adı" value={detail.data.businessName} />
                            <Row label="Durum" value={<StatusChip status={detail.data.status} />} />
                            <Row label="E-posta" value={detail.data.contactEmail ?? '—'} />
                            <Row label="Telefon" value={detail.data.contactPhone ?? '—'} />
                            <Row label="Doğrulanmış" value={detail.data.isVerified ? 'Evet' : 'Hayır'} />
                            <Row label="Açıklama" value={detail.data.description ?? '—'} />
                            <Row label="Üye sayısı" value={String(detail.data.members?.length ?? 0)} />
                        </Stack>
                    ) : (
                        <Typography color="text.secondary">Detay yüklenemedi.</Typography>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDetailId(null)}>Kapat</Button>
                </DialogActions>
            </Dialog>

            {/* Onay modal */}
            <Dialog open={confirm != null} onClose={() => !busy && setConfirm(null)} maxWidth="xs" fullWidth>
                <DialogTitle>
                    {confirm?.action === 'suspend' ? 'Mağazayı Askıya Al' : 'Mağazayı Reaktive Et'}
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        {confirm?.action === 'suspend'
                            ? <><strong>{confirm?.store.name}</strong> askıya alınacak. Ürünleri aramadan ve vitrinden kaldırılacak.</>
                            : <><strong>{confirm?.store.name}</strong> yeniden aktive edilecek. Ürünleri tekrar satışa dönecek.</>}
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setConfirm(null)} disabled={busy} color="inherit">Vazgeç</Button>
                    <Button
                        variant="contained"
                        color={confirm?.action === 'suspend' ? 'error' : 'success'}
                        disabled={busy}
                        startIcon={busy ? <CircularProgress size={18} color="inherit" /> : undefined}
                        onClick={runConfirm}
                    >
                        {confirm?.action === 'suspend' ? 'Askıya Al' : 'Reaktive Et'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

const Row: React.FC<{ label: string; value: React.ReactNode }> = ({ label, value }) => (
    <Stack direction="row" spacing={2}>
        <Typography variant="body2" color="text.secondary" sx={{ minWidth: 120 }}>{label}</Typography>
        <Box sx={{ flex: 1 }}><Typography variant="body2" component="div">{value}</Typography></Box>
    </Stack>
);

export default AdminStoresPage;
