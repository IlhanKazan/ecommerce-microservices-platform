import React, { useMemo, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TablePagination, Chip, TextField, MenuItem, Stack, Button, CircularProgress, Avatar,
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Tooltip, IconButton,
} from '@mui/material';
import { Block as DeactivateIcon, DeleteForever as DeleteIcon, BarChart as MetricsIcon } from '@mui/icons-material';
import { useAdminProducts, useDeactivateProduct, useRemoveProduct } from '../../../query/useAdminQueries';
import { useAdminProductMetrics } from '../../../query/useOrderQueries';
import { formatPrice } from '../../../utils/formatPrice';
import { useToastStore } from '../../../store/useToastStore';
import EmptyState from '../../../components/shared/EmptyState';
import ProductMetricsModal from '../../../components/shared/ProductMetricsModal';
import type { AdminProduct } from '../../../types/admin';

const STATUS_META: Record<string, { label: string; color: 'success' | 'default' | 'error' }> = {
    ACTIVE: { label: 'Aktif', color: 'success' },
    INACTIVE: { label: 'Pasif', color: 'default' },
    DELETED: { label: 'Silinmiş', color: 'error' },
};

const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE'];

type Pending = { type: 'deactivate' | 'delete'; product: AdminProduct } | null;

const AdminProductsPage: React.FC = () => {
    const toast = useToastStore();
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(20);
    const [status, setStatus] = useState('');
    const [searchInput, setSearchInput] = useState('');
    const [q, setQ] = useState('');
    const [tenantInput, setTenantInput] = useState('');
    const [tenantId, setTenantId] = useState<number | null>(null);
    const [pending, setPending] = useState<Pending>(null);
    const [metricsTarget, setMetricsTarget] = useState<AdminProduct | null>(null);

    const query = useMemo(
        () => ({ page, size: rowsPerPage, q: q || null, tenantId, status: status || null }),
        [page, rowsPerPage, q, tenantId, status],
    );
    const { data, isLoading, isError } = useAdminProducts(query);
    const adminMetrics = useAdminProductMetrics(metricsTarget?.id ?? null, !!metricsTarget);
    const deactivate = useDeactivateProduct();
    const remove = useRemoveProduct();

    const applyFilters = () => {
        setPage(0);
        setQ(searchInput.trim());
        const n = parseInt(tenantInput, 10);
        setTenantId(Number.isFinite(n) ? n : null);
    };

    const products = data?.content ?? [];

    const confirmAction = () => {
        if (!pending) return;
        const { type, product } = pending;
        const mutation = type === 'deactivate' ? deactivate : remove;
        mutation.mutate(product.id, {
            onSuccess: () => {
                toast.success(type === 'deactivate' ? 'Ürün satıştan kaldırıldı.' : 'Ürün silindi.');
                setPending(null);
            },
            onError: () => toast.error('İşlem başarısız oldu.'),
        });
    };

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Ürün Yönetimi</Typography>

            {/* Filtreler */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} flexWrap="wrap">
                    <TextField size="small" label="Ürün adı / SKU" value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applyFilters(); }} sx={{ minWidth: 220 }} />
                    <TextField size="small" select label="Durum" value={status}
                        onChange={(e) => { setPage(0); setStatus(e.target.value); }} sx={{ minWidth: 160 }}>
                        <MenuItem value="">Tümü</MenuItem>
                        {STATUS_OPTIONS.map((s) => <MenuItem key={s} value={s}>{STATUS_META[s].label}</MenuItem>)}
                    </TextField>
                    <TextField size="small" label="Mağaza ID" value={tenantInput} type="number"
                        onChange={(e) => setTenantInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applyFilters(); }} sx={{ maxWidth: 160 }} />
                    <Button variant="contained" onClick={applyFilters}>Filtrele</Button>
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Ürünler yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : products.length === 0 ? (
                    <EmptyState title="Ürün bulunamadı" description="Filtrelere uyan ürün yok." />
                ) : (
                    <>
                        <TableContainer>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Ürün</TableCell>
                                        <TableCell>SKU</TableCell>
                                        <TableCell>Mağaza</TableCell>
                                        <TableCell>Fiyat</TableCell>
                                        <TableCell>Durum</TableCell>
                                        <TableCell align="right">İşlem</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {products.map((p) => {
                                        const sm = STATUS_META[p.status] ?? { label: p.status, color: 'default' as const };
                                        return (
                                            <TableRow key={p.id} hover>
                                                <TableCell>
                                                    <Stack direction="row" alignItems="center" spacing={1.5}>
                                                        <Avatar variant="rounded" src={p.mainImageUrl ?? undefined} sx={{ width: 40, height: 40 }}>
                                                            {p.name.charAt(0)}
                                                        </Avatar>
                                                        <Typography variant="body2" sx={{ maxWidth: 260 }} noWrap title={p.name}>
                                                            {p.name}
                                                        </Typography>
                                                    </Stack>
                                                </TableCell>
                                                <TableCell>{p.sku}</TableCell>
                                                <TableCell>#{p.tenantId}</TableCell>
                                                <TableCell>{formatPrice(p.price)}</TableCell>
                                                <TableCell>
                                                    <Chip size="small" label={sm.label} color={sm.color}
                                                        variant={sm.color === 'default' ? 'outlined' : 'filled'} />
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Tooltip title="Satış Metrikleri">
                                                        <IconButton size="small" color="info"
                                                            onClick={() => setMetricsTarget(p)}>
                                                            <MetricsIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                    <Tooltip title="Satıştan kaldır (Pasif)">
                                                        <span>
                                                            <IconButton size="small" color="warning"
                                                                disabled={p.status === 'INACTIVE'}
                                                                onClick={() => setPending({ type: 'deactivate', product: p })}>
                                                                <DeactivateIcon fontSize="small" />
                                                            </IconButton>
                                                        </span>
                                                    </Tooltip>
                                                    <Tooltip title="Sil">
                                                        <IconButton size="small" color="error"
                                                            onClick={() => setPending({ type: 'delete', product: p })}>
                                                            <DeleteIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
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
                            rowsPerPageOptions={[20, 50, 100]}
                            labelRowsPerPage="Sayfa başına"
                        />
                    </>
                )}
            </Paper>

            {/* Onay dialogu */}
            <Dialog open={pending != null} onClose={() => setPending(null)} maxWidth="xs" fullWidth>
                <DialogTitle>{pending?.type === 'delete' ? 'Ürünü Sil' : 'Satıştan Kaldır'}</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        <strong>{pending?.product.name}</strong>{' '}
                        {pending?.type === 'delete'
                            ? 'ürününü kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.'
                            : 'ürününü satıştan kaldırmak (pasifleştirmek) istediğinize emin misiniz?'}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setPending(null)}>Vazgeç</Button>
                    <Button
                        variant="contained"
                        color={pending?.type === 'delete' ? 'error' : 'warning'}
                        onClick={confirmAction}
                        disabled={deactivate.isPending || remove.isPending}
                    >
                        {pending?.type === 'delete' ? 'Sil' : 'Kaldır'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Satış metrikleri dialog */}
            <ProductMetricsModal
                open={!!metricsTarget}
                onClose={() => setMetricsTarget(null)}
                productName={metricsTarget?.name ?? ''}
                data={adminMetrics.data}
                isLoading={adminMetrics.isLoading}
                isError={adminMetrics.isError}
            />
        </Box>
    );
};

export default AdminProductsPage;
