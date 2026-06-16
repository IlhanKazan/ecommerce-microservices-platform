import React, { useMemo, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TablePagination, Chip, TextField, MenuItem, Stack, Button, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions, Divider, Grid, Link,
} from '@mui/material';
import { BarChart } from '@mui/x-charts/BarChart';
import { Link as RouterLink } from 'react-router-dom';
import { Visibility as ViewIcon } from '@mui/icons-material';
import { useAdminOrders, useAdminOrderDetail, useAdminOrderStats } from '../../../query/useAdminQueries';
import { useAdminApproveReturn, useAdminRejectReturn } from '../../../query/useOrderQueries';
import { useToastStore } from '../../../store/useToastStore';
import { formatPrice } from '../../../utils/formatPrice';
import EmptyState from '../../../components/shared/EmptyState';

const STATUS_META: Record<string, { label: string; color: 'success' | 'warning' | 'error' | 'default' | 'info' }> = {
    CONFIRMED: { label: 'Onaylandı', color: 'info' },
    SHIPPED: { label: 'Kargoda', color: 'warning' },
    DELIVERED: { label: 'Teslim Edildi', color: 'success' },
    CANCELLED: { label: 'İptal', color: 'default' },
    REFUNDED: { label: 'İade', color: 'error' },
    RETURN_REQUESTED: { label: 'İade Talebi', color: 'warning' },
    RETURNED: { label: 'İade Edildi', color: 'error' },
    RETURN_REJECTED: { label: 'İade Reddedildi', color: 'default' },
};

const StatusChip: React.FC<{ status: string }> = ({ status }) => {
    const m = STATUS_META[status] ?? { label: status, color: 'default' as const };
    return <Chip size="small" label={m.label} color={m.color} variant={m.color === 'default' ? 'outlined' : 'filled'} />;
};

const AdminOrdersPage: React.FC = () => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);
    const [status, setStatus] = useState('');
    const [tenantInput, setTenantInput] = useState('');
    const [tenantId, setTenantId] = useState<number | null>(null);
    const [detailId, setDetailId] = useState<number | null>(null);

    const query = useMemo(
        () => ({ page, size: rowsPerPage, status: status || null, tenantId }),
        [page, rowsPerPage, status, tenantId],
    );
    const { data, isLoading, isError } = useAdminOrders(query);
    const stats = useAdminOrderStats();
    const detail = useAdminOrderDetail(detailId);
    const approveReturn = useAdminApproveReturn();
    const rejectReturn = useAdminRejectReturn();
    const toast = useToastStore();

    const resolvingReturn = approveReturn.isPending || rejectReturn.isPending;
    const handleAdminResolve = (mode: 'approve' | 'reject') => {
        if (detailId == null) return;
        const fn = mode === 'approve' ? approveReturn : rejectReturn;
        fn.mutate({ orderId: detailId }, {
            onSuccess: () => {
                toast.success(mode === 'approve' ? 'İade onaylandı, para iadesi yapıldı.' : 'İade reddedildi.');
                setDetailId(null);
            },
            onError: (err: unknown) => {
                const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
                toast.error(msg || 'İşlem başarısız oldu.');
            },
        });
    };

    const applyTenant = () => {
        setPage(0);
        const n = parseInt(tenantInput, 10);
        setTenantId(Number.isFinite(n) ? n : null);
    };

    const orders = data?.content ?? [];

    const chart = useMemo(() => {
        const by = stats.data?.byStatus ?? {};
        const keys = Object.keys(STATUS_META).filter((k) => by[k] != null);
        return {
            labels: keys.map((k) => STATUS_META[k].label),
            values: keys.map((k) => by[k] ?? 0),
        };
    }, [stats.data]);

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Sipariş Yönetimi</Typography>

            {/* Özet kartları + grafik */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, md: 4 }}>
                    <Stack spacing={2}>
                        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                            <Typography variant="caption" color="text.secondary">Toplam Sipariş</Typography>
                            <Typography variant="h4" fontWeight="bold">{stats.data?.totalOrders ?? '—'}</Typography>
                        </Paper>
                        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                            <Typography variant="caption" color="text.secondary">Toplam Ciro (GMV)</Typography>
                            <Typography variant="h5" fontWeight="bold" color="primary.main">
                                {stats.data ? formatPrice(stats.data.totalGmv) : '—'}
                            </Typography>
                        </Paper>
                    </Stack>
                </Grid>
                <Grid size={{ xs: 12, md: 8 }}>
                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
                        <Typography variant="subtitle2" sx={{ mb: 1 }}>Duruma Göre Sipariş Dağılımı</Typography>
                        {stats.isLoading ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                        ) : chart.labels.length === 0 ? (
                            <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>Veri yok.</Typography>
                        ) : (
                            <BarChart
                                height={220}
                                xAxis={[{ scaleType: 'band', data: chart.labels }]}
                                series={[{ data: chart.values, label: 'Sipariş', color: '#F27A1A' }]}
                            />
                        )}
                    </Paper>
                </Grid>
            </Grid>

            {/* Filtreler */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                    <TextField
                        size="small" select label="Durum" value={status}
                        onChange={(e) => { setPage(0); setStatus(e.target.value); }}
                        sx={{ minWidth: 180 }}
                    >
                        <MenuItem value="">Tümü</MenuItem>
                        {Object.keys(STATUS_META).map((s) => (
                            <MenuItem key={s} value={s}>{STATUS_META[s]?.label ?? s}</MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        size="small" label="Mağaza ID" value={tenantInput} type="number"
                        onChange={(e) => setTenantInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applyTenant(); }}
                        sx={{ maxWidth: 160 }}
                    />
                    <Button variant="contained" onClick={applyTenant}>Filtrele</Button>
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Siparişler yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : orders.length === 0 ? (
                    <EmptyState title="Sipariş bulunamadı" description="Filtrelere uyan sipariş yok." />
                ) : (
                    <>
                        <TableContainer>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>#</TableCell>
                                        <TableCell>Mağaza</TableCell>
                                        <TableCell>Müşteri</TableCell>
                                        <TableCell>Tutar</TableCell>
                                        <TableCell>Durum</TableCell>
                                        <TableCell>Tarih</TableCell>
                                        <TableCell align="right">İşlem</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {orders.map((o) => (
                                        <TableRow key={o.orderId} hover>
                                            <TableCell>#{o.orderId}</TableCell>
                                            <TableCell>#{o.tenantId}</TableCell>
                                            <TableCell>{o.buyerEmail ?? '—'}</TableCell>
                                            <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                                            <TableCell><StatusChip status={o.status} /></TableCell>
                                            <TableCell>{new Date(o.createdAt).toLocaleDateString('tr-TR')}</TableCell>
                                            <TableCell align="right">
                                                <Button size="small" startIcon={<ViewIcon />} onClick={() => setDetailId(o.orderId)}>Detay</Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
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
                <DialogTitle>Sipariş #{detailId} Detayı</DialogTitle>
                <DialogContent dividers>
                    {detail.isLoading ? (
                        <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                    ) : detail.data ? (
                        <Stack spacing={1.5}>
                            <Stack direction="row" justifyContent="space-between">
                                <Typography variant="body2" color="text.secondary">Mağaza</Typography>
                                <Typography variant="body2">#{detail.data.tenantId}</Typography>
                            </Stack>
                            <Stack direction="row" justifyContent="space-between">
                                <Typography variant="body2" color="text.secondary">Müşteri</Typography>
                                <Typography variant="body2">{detail.data.buyerEmail ?? '—'}</Typography>
                            </Stack>
                            <Stack direction="row" justifyContent="space-between">
                                <Typography variant="body2" color="text.secondary">Durum</Typography>
                                <StatusChip status={detail.data.status} />
                            </Stack>
                            <Stack direction="row" justifyContent="space-between">
                                <Typography variant="body2" color="text.secondary">Toplam</Typography>
                                <Typography variant="body2" fontWeight={600}>{formatPrice(detail.data.totalAmount)}</Typography>
                            </Stack>
                            <Divider sx={{ my: 1 }} />
                            <Typography variant="subtitle2">Ürünler</Typography>
                            {detail.data.items.map((it, i) => (
                                <Stack key={i} direction="row" justifyContent="space-between" alignItems="center">
                                    <Link
                                        component={RouterLink}
                                        to={`/product/${it.productId}`}
                                        target="_blank"
                                        rel="noopener"
                                        variant="body2"
                                        underline="hover"
                                        color="text.primary"
                                        sx={{ flex: 1, '&:hover': { color: 'primary.main' } }}
                                    >
                                        {it.productName}
                                    </Link>
                                    <Typography variant="caption" color="text.secondary" sx={{ mx: 2 }}>x{it.quantity}</Typography>
                                    <Typography variant="body2">{formatPrice(it.unitPrice)}</Typography>
                                </Stack>
                            ))}
                        </Stack>
                    ) : (
                        <Typography color="text.secondary">Detay yüklenemedi.</Typography>
                    )}
                </DialogContent>
                <DialogActions>
                    {detail.data?.status === 'RETURN_REQUESTED' && (
                        <>
                            <Button color="error" onClick={() => handleAdminResolve('reject')} disabled={resolvingReturn}>
                                İadeyi Reddet
                            </Button>
                            <Button color="success" variant="contained" onClick={() => handleAdminResolve('approve')} disabled={resolvingReturn}>
                                İadeyi Onayla
                            </Button>
                        </>
                    )}
                    <Button onClick={() => setDetailId(null)}>Kapat</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminOrdersPage;
