import React, { useState } from 'react';
import {
    Box, Paper, Typography, Stack, CircularProgress, Alert, Chip, Button,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, TextField,
} from '@mui/material';
import { CheckCircle as ApproveIcon, Cancel as RejectIcon, Search as SearchIcon } from '@mui/icons-material';
import { InputAdornment } from '@mui/material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import {
    useGetTenantReturns, useApproveReturn, useRejectReturn,
} from '../../../query/useOrderQueries';
import { useToastStore } from '../../../store/useToastStore';
import { formatPrice } from '../../../utils/formatPrice';
import { formatDateTime } from '../../../utils/formatDate';
import { returnReasonLabel } from '../../../utils/returnReasons';
import EmptyState from '../../../components/shared/EmptyState';
import type { OrderReturn } from '../../../types/order';

type Resolve = { mode: 'approve' | 'reject'; ret: OrderReturn } | null;

const MerchantReturnsPage: React.FC = () => {
    const activeTenant = useMerchantStore((s) => s.activeTenant);
    const tenantId = activeTenant?.id ?? null;

    const { data, isLoading, isError } = useGetTenantReturns(tenantId);
    const approve = useApproveReturn(tenantId ?? 0);
    const reject = useRejectReturn(tenantId ?? 0);
    const toast = useToastStore();

    const [resolve, setResolve] = useState<Resolve>(null);
    const [note, setNote] = useState('');
    const [search, setSearch] = useState('');

    const busy = approve.isPending || reject.isPending;

    const handleConfirm = () => {
        if (!resolve) return;
        const { mode, ret } = resolve;
        const payload = { orderId: ret.orderId, note: note || undefined };
        const opts = {
            onSuccess: () => {
                toast.success(mode === 'approve' ? 'İade onaylandı, para iadesi yapıldı.' : 'İade reddedildi.');
                setResolve(null);
                setNote('');
            },
            onError: (err: unknown) => {
                const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
                toast.error(msg || 'İşlem başarısız oldu.');
            },
        };
        if (mode === 'approve') approve.mutate(payload, opts);
        else reject.mutate(payload, opts);
    };

    if (!activeTenant) {
        return <Alert severity="info">Önce bir mağaza seçin.</Alert>;
    }
    if (isLoading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}><CircularProgress /></Box>;
    }
    if (isError) {
        return <Alert severity="error">İade talepleri yüklenirken bir hata oluştu.</Alert>;
    }

    const returns = data ?? [];
    const q = search.trim().toLowerCase();
    const filteredReturns = q
        ? returns.filter((r) =>
            String(r.orderId).includes(q) || (r.buyerEmail ?? '').toLowerCase().includes(q))
        : returns;

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" spacing={2} sx={{ mb: 3 }}>
                <Typography variant="h5" fontWeight="bold">İade Talepleri</Typography>
                <Stack direction="row" alignItems="center" spacing={2}>
                    {returns.length > 0 && (
                        <TextField
                            placeholder="Sipariş no veya alıcı ara..."
                            size="small"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            sx={{ width: { xs: '100%', sm: 280 } }}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon fontSize="small" color="action" />
                                    </InputAdornment>
                                ),
                            }}
                        />
                    )}
                    <Chip label={`${returns.length} bekleyen`} variant="outlined" />
                </Stack>
            </Stack>

            {returns.length === 0 ? (
                <EmptyState title="Bekleyen iade yok" description="Müşteriler iade talebi açtığında burada görünür ve onaylayabilirsiniz." />
            ) : (
                <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Sipariş</TableCell>
                                    <TableCell>Alıcı</TableCell>
                                    <TableCell align="right">Tutar</TableCell>
                                    <TableCell>Sebep</TableCell>
                                    <TableCell>Tarih</TableCell>
                                    <TableCell align="right">İşlem</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {filteredReturns.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                                            Aramayla eşleşen iade talebi yok.
                                        </TableCell>
                                    </TableRow>
                                ) : filteredReturns.map((r) => (
                                    <TableRow key={r.returnId} hover>
                                        <TableCell>#{r.orderId}</TableCell>
                                        <TableCell>{r.buyerEmail ?? '—'}</TableCell>
                                        <TableCell align="right">{r.orderTotal != null ? formatPrice(r.orderTotal) : '—'}</TableCell>
                                        <TableCell sx={{ maxWidth: 240 }}>
                                            <Typography variant="body2" fontWeight={600}>{returnReasonLabel(r.reasonCode)}</Typography>
                                            {r.reason && (
                                                <Typography variant="caption" color="text.secondary" noWrap display="block" title={r.reason}>
                                                    {r.reason}
                                                </Typography>
                                            )}
                                        </TableCell>
                                        <TableCell>{formatDateTime(r.createdAt)}</TableCell>
                                        <TableCell align="right">
                                            <Button size="small" color="success" startIcon={<ApproveIcon />}
                                                onClick={() => { setResolve({ mode: 'approve', ret: r }); setNote(''); }}>
                                                Onayla
                                            </Button>
                                            <Button size="small" color="error" startIcon={<RejectIcon />}
                                                onClick={() => { setResolve({ mode: 'reject', ret: r }); setNote(''); }}>
                                                Reddet
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Paper>
            )}

            <Dialog open={resolve != null} onClose={() => setResolve(null)} maxWidth="xs" fullWidth>
                <DialogTitle>{resolve?.mode === 'approve' ? 'İadeyi Onayla' : 'İadeyi Reddet'}</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        {resolve?.mode === 'approve'
                            ? `#${resolve?.ret.orderId} siparişinin iadesini onaylıyorsunuz. Müşteriye para iadesi yapılacak ve stok geri eklenecek.`
                            : `#${resolve?.ret.orderId} siparişinin iade talebini reddediyorsunuz. Sipariş "Teslim Edildi" durumuna döner.`}
                    </DialogContentText>
                    <TextField
                        label="Not (opsiyonel)"
                        fullWidth multiline rows={2}
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setResolve(null)} disabled={busy}>Vazgeç</Button>
                    <Button
                        variant="contained"
                        color={resolve?.mode === 'approve' ? 'success' : 'error'}
                        onClick={handleConfirm}
                        disabled={busy}
                    >
                        {busy ? <CircularProgress size={18} color="inherit" /> : (resolve?.mode === 'approve' ? 'Onayla' : 'Reddet')}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default MerchantReturnsPage;
