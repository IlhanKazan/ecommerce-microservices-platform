import React, { useMemo, useState } from 'react';
import {
    Box, Paper, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    TablePagination, Chip, TextField, MenuItem, Stack, Button, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions, Divider,
} from '@mui/material';
import { useAdminTransactions } from '../../../query/useAdminQueries';
import { formatPrice } from '../../../utils/formatPrice';
import EmptyState from '../../../components/shared/EmptyState';
import type { AdminTransaction } from '../../../types/admin';

const TYPE_META: Record<string, { label: string; color: 'primary' | 'secondary' }> = {
    PRODUCT_ORDER: { label: 'Ürün Siparişi', color: 'primary' },
    SUBSCRIPTION: { label: 'Abonelik', color: 'secondary' },
};

const STATUS_META: Record<string, { label: string; color: 'success' | 'warning' | 'error' | 'default' }> = {
    SUCCESS: { label: 'Başarılı', color: 'success' },
    PENDING: { label: 'Beklemede', color: 'warning' },
    FAILURE: { label: 'Başarısız', color: 'error' },
    REFUNDED: { label: 'İade', color: 'default' },
    PROVISION_FAILED: { label: 'Provizyon Hatası', color: 'error' },
};

const purpose = (t: AdminTransaction): string => {
    if (t.paymentType === 'PRODUCT_ORDER') return t.orderId != null ? `Sipariş #${t.orderId}` : 'Ürün siparişi';
    if (t.paymentType === 'SUBSCRIPTION') return 'Abonelik ödemesi';
    return '—';
};

const buyerLabel = (t: AdminTransaction): string =>
    t.buyerName || t.buyerEmail || (t.paymentType === 'SUBSCRIPTION' ? 'Mağaza sahibi' : '—');

const DetailRow: React.FC<{ label: string; children: React.ReactNode }> = ({ label, children }) => (
    <Stack direction="row" justifyContent="space-between" spacing={2}>
        <Typography variant="body2" color="text.secondary">{label}</Typography>
        <Typography variant="body2" sx={{ textAlign: 'right' }}>{children}</Typography>
    </Stack>
);

const AdminTransactionsPage: React.FC = () => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(20);
    const [type, setType] = useState('');
    const [status, setStatus] = useState('');
    const [tenantInput, setTenantInput] = useState('');
    const [tenantId, setTenantId] = useState<number | null>(null);
    const [detail, setDetail] = useState<AdminTransaction | null>(null);

    const query = useMemo(
        () => ({ page, size: rowsPerPage, type: type || null, status: status || null, tenantId }),
        [page, rowsPerPage, type, status, tenantId],
    );
    const { data, isLoading, isError } = useAdminTransactions(query);

    const applyTenant = () => {
        setPage(0);
        const n = parseInt(tenantInput, 10);
        setTenantId(Number.isFinite(n) ? n : null);
    };

    const rows = data?.content ?? [];

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Ödemeler</Typography>

            {/* Filtreler */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} flexWrap="wrap">
                    <TextField size="small" select label="Tür" value={type}
                        onChange={(e) => { setPage(0); setType(e.target.value); }} sx={{ minWidth: 180 }}>
                        <MenuItem value="">Tümü</MenuItem>
                        {Object.entries(TYPE_META).map(([k, v]) => <MenuItem key={k} value={k}>{v.label}</MenuItem>)}
                    </TextField>
                    <TextField size="small" select label="Durum" value={status}
                        onChange={(e) => { setPage(0); setStatus(e.target.value); }} sx={{ minWidth: 180 }}>
                        <MenuItem value="">Tümü</MenuItem>
                        {Object.entries(STATUS_META).map(([k, v]) => <MenuItem key={k} value={k}>{v.label}</MenuItem>)}
                    </TextField>
                    <TextField size="small" label="Mağaza ID" value={tenantInput} type="number"
                        onChange={(e) => setTenantInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') applyTenant(); }} sx={{ maxWidth: 160 }} />
                    <Button variant="contained" onClick={applyTenant}>Filtrele</Button>
                </Stack>
            </Paper>

            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Ödemeler yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : rows.length === 0 ? (
                    <EmptyState title="Ödeme bulunamadı" description="Filtrelere uyan ödeme yok." />
                ) : (
                    <>
                        <TableContainer>
                            <Table>
                                <TableHead>
                                    <TableRow>
                                        <TableCell>#</TableCell>
                                        <TableCell>Tür</TableCell>
                                        <TableCell>Alıcı</TableCell>
                                        <TableCell>Mağaza</TableCell>
                                        <TableCell>Açıklama</TableCell>
                                        <TableCell>Tutar</TableCell>
                                        <TableCell>Durum</TableCell>
                                        <TableCell>Tarih</TableCell>
                                        <TableCell align="right">İşlem</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {rows.map((p) => {
                                        const tm = TYPE_META[p.paymentType] ?? { label: p.paymentType, color: 'primary' as const };
                                        const sm = STATUS_META[p.paymentStatus] ?? { label: p.paymentStatus, color: 'default' as const };
                                        return (
                                            <TableRow key={p.id} hover sx={{ cursor: 'pointer' }} onClick={() => setDetail(p)}>
                                                <TableCell>#{p.id}</TableCell>
                                                <TableCell><Chip size="small" label={tm.label} color={tm.color} variant="outlined" /></TableCell>
                                                <TableCell>{buyerLabel(p)}</TableCell>
                                                <TableCell>{p.tenantName ?? (p.tenantId != null ? `#${p.tenantId}` : '—')}</TableCell>
                                                <TableCell>{purpose(p)}</TableCell>
                                                <TableCell>{formatPrice(p.amount)}</TableCell>
                                                <TableCell>
                                                    <Chip size="small" label={sm.label} color={sm.color}
                                                        variant={sm.color === 'default' ? 'outlined' : 'filled'} />
                                                </TableCell>
                                                <TableCell>{new Date(p.paidAt ?? p.createdAt).toLocaleString('tr-TR')}</TableCell>
                                                <TableCell align="right">
                                                    <Button size="small" onClick={(e) => { e.stopPropagation(); setDetail(p); }}>Detay</Button>
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

            {/* Detay modal */}
            <Dialog open={detail != null} onClose={() => setDetail(null)} maxWidth="sm" fullWidth>
                <DialogTitle>Ödeme #{detail?.id} Detayı</DialogTitle>
                <DialogContent dividers>
                    {detail && (
                        <Stack spacing={1.5}>
                            <DetailRow label="Tür">{TYPE_META[detail.paymentType]?.label ?? detail.paymentType}</DetailRow>
                            <DetailRow label="Açıklama">{purpose(detail)}</DetailRow>
                            <DetailRow label="Alıcı">{buyerLabel(detail)}</DetailRow>
                            {detail.buyerEmail && <DetailRow label="E-posta">{detail.buyerEmail}</DetailRow>}
                            <DetailRow label="Mağaza">{detail.tenantName ?? (detail.tenantId != null ? `#${detail.tenantId}` : '—')}</DetailRow>
                            <Divider sx={{ my: 1 }} />
                            <DetailRow label="Tutar">{formatPrice(detail.amount)}</DetailRow>
                            {detail.commissionAmount != null && (
                                <DetailRow label={`Komisyon${detail.commissionRate != null ? ` (%${detail.commissionRate})` : ''}`}>
                                    {formatPrice(detail.commissionAmount)}
                                </DetailRow>
                            )}
                            {detail.netAmount != null && <DetailRow label="Net (mağaza)">{formatPrice(detail.netAmount)}</DetailRow>}
                            {detail.refundedAmount != null && Number(detail.refundedAmount) > 0 && (
                                <DetailRow label="İade Edilen">{formatPrice(detail.refundedAmount)}</DetailRow>
                            )}
                            <Divider sx={{ my: 1 }} />
                            <DetailRow label="Durum">{STATUS_META[detail.paymentStatus]?.label ?? detail.paymentStatus}</DetailRow>
                            <DetailRow label="Ödeme Yöntemi">{detail.paymentMethod ?? '—'}</DetailRow>
                            <DetailRow label="iyzico Referans">{detail.iyzicoTransactionId ?? '—'}</DetailRow>
                            {detail.orderId != null && <DetailRow label="Sipariş">#{detail.orderId}</DetailRow>}
                            {detail.subscriptionId != null && <DetailRow label="Abonelik">#{detail.subscriptionId}</DetailRow>}
                            <DetailRow label="Tarih">{new Date(detail.paidAt ?? detail.createdAt).toLocaleString('tr-TR')}</DetailRow>
                        </Stack>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDetail(null)}>Kapat</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminTransactionsPage;
