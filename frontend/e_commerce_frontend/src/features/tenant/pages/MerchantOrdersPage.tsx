import React, { useEffect, useState } from 'react';
import {
    Box, Paper, Typography, Stack, Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, TablePagination, Chip, IconButton, Select, MenuItem,
    FormControl, InputLabel, CircularProgress, Alert, Tooltip,
    Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField,
    InputAdornment,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import SearchIcon from '@mui/icons-material/Search';
import { useDebounce } from '../../../hooks/useDebounce';
import type { OrderDetail, OrderStatus } from '../../../types/order';
import { ORDER_STATUS_CONFIG } from '../../../utils/orderUtils';
import { useGetTenantOrders, useUpdateOrderStatus } from '../../../query/useOrderQueries';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useToastStore } from '../../../store/useToastStore';
import { formatPrice } from '../../../utils/formatPrice';
import { formatDate } from '../../../utils/formatDate';
import MerchantOrderDetailModal from '../components/MerchantOrderDetailModal';

const MerchantOrdersPage: React.FC = () => {
    const activeTenant = useMerchantStore((s) => s.activeTenant);
    const tenantId = activeTenant?.id ?? null;
    const toast = useToastStore();

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(20);
    const [filterStatus, setFilterStatus] = useState<OrderStatus | 'ALL'>('ALL');
    const [searchInput, setSearchInput] = useState('');
    const [selectedOrder, setSelectedOrder] = useState<OrderDetail | null>(null);
    const [shippingOrderId, setShippingOrderId] = useState<number | null>(null);
    const [trackingNumber, setTrackingNumber] = useState('');

    const debouncedSearch = useDebounce(searchInput, 300);
    const serverStatus = filterStatus === 'ALL' ? '' : filterStatus;

    // Arama/filtre değişince ilk sayfaya dön
    useEffect(() => {
        setPage(0);
    }, [debouncedSearch, filterStatus]);

    const { data, isLoading, isError } = useGetTenantOrders(
        tenantId, page, rowsPerPage, serverStatus, debouncedSearch);
    const { mutate: updateStatus, isPending: isUpdating } = useUpdateOrderStatus();

    // Filtre + arama artık server-side; gelen sayfa doğrudan listelenir.
    const orders = data?.content ?? [];

    const handleShipOrder = (order: OrderDetail) => {
        updateStatus(
            {
                tenantId: tenantId!,
                orderId: order.orderId,
                status: 'SHIPPED',
                trackingNumber: trackingNumber || undefined,
            },
            {
                onSuccess: () => {
                    toast.success('Sipariş kargoya verildi.');
                    setShippingOrderId(null);
                    setTrackingNumber('');
                },
                onError: () => toast.error('İşlem başarısız oldu.'),
            },
        );
    };

    const handleDeliverOrder = (order: OrderDetail) => {
        updateStatus(
            { tenantId: tenantId!, orderId: order.orderId, status: 'DELIVERED' },
            {
                onSuccess: () => toast.success('Sipariş teslim edildi olarak işaretlendi.'),
                onError: () => toast.error('İşlem başarısız oldu.'),
            },
        );
    };

    if (!tenantId) return <Alert severity="warning">Aktif mağaza bulunamadı.</Alert>;
    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}>
                <CircularProgress />
            </Box>
        );
    }
    if (isError) return <Alert severity="error">Siparişler yüklenirken hata oluştu.</Alert>;

    const shippingOrder = orders.find((o) => o.orderId === shippingOrderId) ?? null;

    return (
        <Paper variant="outlined" sx={{ p: 3, borderRadius: 3 }}>
            {/* Başlık + filtre */}
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                justifyContent="space-between"
                alignItems={{ sm: 'center' }}
                spacing={2}
                sx={{ mb: 3 }}
            >
                <Typography variant="h6" fontWeight="bold">Siparişler</Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ flex: 1, justifyContent: 'flex-end' }}>
                <TextField
                    placeholder="Sipariş no veya alıcı e-postası ara..."
                    size="small"
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    sx={{ flex: 1, maxWidth: 360 }}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <SearchIcon fontSize="small" color="action" />
                            </InputAdornment>
                        ),
                    }}
                />
                <FormControl size="small" sx={{ minWidth: 180 }}>
                    <InputLabel>Durum Filtresi</InputLabel>
                    <Select
                        value={filterStatus}
                        label="Durum Filtresi"
                        onChange={(e) => {
                            setFilterStatus(e.target.value as OrderStatus | 'ALL');
                            setPage(0);
                        }}
                    >
                        <MenuItem value="ALL">Tümü</MenuItem>
                        <MenuItem value="CONFIRMED">Onaylandı</MenuItem>
                        <MenuItem value="SHIPPED">Kargoda</MenuItem>
                        <MenuItem value="DELIVERED">Teslim Edildi</MenuItem>
                        <MenuItem value="CANCELLED">İptal Edildi</MenuItem>
                        <MenuItem value="REFUNDED">İade Edildi</MenuItem>
                    </Select>
                </FormControl>
                </Stack>
            </Stack>

            {/* Tablo */}
            <TableContainer>
                <Table size="small">
                    <TableHead>
                        <TableRow sx={{ '& th': { fontWeight: 'bold', bgcolor: 'grey.50' } }}>
                            <TableCell>Sipariş No</TableCell>
                            <TableCell>Tarih</TableCell>
                            <TableCell>Ürünler</TableCell>
                            <TableCell align="right">Tutar</TableCell>
                            <TableCell align="center">Durum</TableCell>
                            <TableCell align="center">İşlemler</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {orders.length === 0 ? (
                            <TableRow>
                                <TableCell
                                    colSpan={6}
                                    align="center"
                                    sx={{ py: 4, color: 'text.secondary' }}
                                >
                                    Sipariş bulunamadı.
                                </TableCell>
                            </TableRow>
                        ) : (
                            orders.map((order) => {
                                const cfg = ORDER_STATUS_CONFIG[order.status];
                                const firstItem = order.items[0];
                                const moreCount = order.items.length - 1;

                                return (
                                    <TableRow key={order.orderId} hover>
                                        <TableCell sx={{ fontFamily: 'monospace' }}>
                                            #{order.orderId}
                                        </TableCell>
                                        <TableCell>{formatDate(order.createdAt)}</TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {firstItem?.productName ?? '—'}
                                                {moreCount > 0 && (
                                                    <span style={{ color: '#666' }}>
                                                        {' '}+{moreCount}
                                                    </span>
                                                )}
                                            </Typography>
                                        </TableCell>
                                        <TableCell align="right">
                                            {formatPrice(order.totalAmount, order.currency)}
                                        </TableCell>
                                        <TableCell align="center">
                                            <Chip
                                                label={cfg.label}
                                                color={cfg.color}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell align="center">
                                            <Stack
                                                direction="row"
                                                justifyContent="center"
                                                spacing={0.5}
                                            >
                                                <Tooltip title="Görüntüle">
                                                    <IconButton
                                                        size="small"
                                                        onClick={() => setSelectedOrder(order)}
                                                    >
                                                        <VisibilityIcon fontSize="small" />
                                                    </IconButton>
                                                </Tooltip>
                                                {order.status === 'CONFIRMED' && (
                                                    <Tooltip title="Kargoya Ver">
                                                        <IconButton
                                                            size="small"
                                                            color="info"
                                                            onClick={() =>
                                                                setShippingOrderId(order.orderId)
                                                            }
                                                            disabled={isUpdating}
                                                        >
                                                            <LocalShippingIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                                {order.status === 'SHIPPED' && (
                                                    <Tooltip title="Teslim Edildi">
                                                        <IconButton
                                                            size="small"
                                                            color="success"
                                                            onClick={() => handleDeliverOrder(order)}
                                                            disabled={isUpdating}
                                                        >
                                                            <CheckCircleIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                )}
                                            </Stack>
                                        </TableCell>
                                    </TableRow>
                                );
                            })
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <TablePagination
                component="div"
                count={data?.totalElements ?? 0}
                page={page}
                onPageChange={(_, p) => setPage(p)}
                rowsPerPage={rowsPerPage}
                onRowsPerPageChange={(e) => {
                    setRowsPerPage(parseInt(e.target.value, 10));
                    setPage(0);
                }}
                rowsPerPageOptions={[10, 20, 50]}
                labelRowsPerPage="Sayfa başına:"
                labelDisplayedRows={({ from, to, count }) => `${from}–${to} / ${count}`}
            />

            {/* Detay Modal */}
            {selectedOrder && (
                <MerchantOrderDetailModal
                    order={selectedOrder}
                    open={!!selectedOrder}
                    tenantId={tenantId}
                    onClose={() => setSelectedOrder(null)}
                />
            )}

            {/* Kargo Dialog */}
            {shippingOrder && (
                <Dialog
                    open={!!shippingOrderId}
                    onClose={() => {
                        setShippingOrderId(null);
                        setTrackingNumber('');
                    }}
                    maxWidth="xs"
                    fullWidth
                >
                    <DialogTitle>Kargoya Ver — #{shippingOrderId}</DialogTitle>
                    <DialogContent>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                            Takip numarası opsiyoneldir.
                        </Typography>
                        <TextField
                            label="Kargo Takip Numarası (opsiyonel)"
                            fullWidth
                            value={trackingNumber}
                            onChange={(e) => setTrackingNumber(e.target.value)}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button
                            onClick={() => {
                                setShippingOrderId(null);
                                setTrackingNumber('');
                            }}
                        >
                            İptal
                        </Button>
                        <Button
                            variant="contained"
                            color="info"
                            onClick={() => handleShipOrder(shippingOrder)}
                            disabled={isUpdating}
                        >
                            {isUpdating ? (
                                <CircularProgress size={18} color="inherit" />
                            ) : (
                                'Kargoya Ver'
                            )}
                        </Button>
                    </DialogActions>
                </Dialog>
            )}
        </Paper>
    );
};

export default MerchantOrdersPage;
