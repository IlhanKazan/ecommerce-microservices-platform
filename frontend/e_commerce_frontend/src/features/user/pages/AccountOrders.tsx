import React, { useState } from 'react';
import {
    Box, Typography, Paper, Stack, Chip, Avatar, CircularProgress, Alert,
    TablePagination,
} from '@mui/material';
import type { OrderDetail } from '../../../types/order';
import { ORDER_STATUS_CONFIG } from '../../../utils/orderUtils';
import { useGetMyOrders } from '../../../query/useOrderQueries';
import { formatPrice } from '../../../utils/formatPrice';
import { formatDate } from '../../../utils/formatDate';
import OrderDetailModal from '../components/OrderDetailModal';

const AccountOrders: React.FC = () => {
    const [page, setPage] = useState(0);
    const [selectedOrder, setSelectedOrder] = useState<OrderDetail | null>(null);
    const { data, isLoading, isError } = useGetMyOrders(page, 10);

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (isError) {
        return <Alert severity="error">Siparişler yüklenirken hata oluştu.</Alert>;
    }

    const orders = data?.content ?? [];

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>
                Siparişlerim
            </Typography>

            {orders.length === 0 ? (
                <Alert severity="info">Henüz hiç siparişiniz bulunmamaktadır.</Alert>
            ) : (
                <Stack spacing={2}>
                    {orders.map((order) => {
                        const cfg = ORDER_STATUS_CONFIG[order.status];
                        const firstItem = order.items[0];
                        const moreCount = order.items.length - 1;

                        return (
                            <Paper
                                key={order.orderId}
                                variant="outlined"
                                sx={{
                                    p: 2,
                                    borderRadius: 2,
                                    cursor: 'pointer',
                                    '&:hover': { borderColor: 'primary.main' },
                                }}
                                onClick={() => setSelectedOrder(order)}
                            >
                                <Stack
                                    direction={{ xs: 'column', sm: 'row' }}
                                    alignItems={{ sm: 'center' }}
                                    justifyContent="space-between"
                                    spacing={2}
                                >
                                    <Stack direction="row" alignItems="center" spacing={2}>
                                        <Avatar
                                            src={firstItem?.productImageUrl ?? undefined}
                                            variant="rounded"
                                            sx={{ width: 56, height: 56, bgcolor: 'grey.100' }}
                                        />
                                        <Box>
                                            <Typography
                                                variant="caption"
                                                color="text.secondary"
                                                sx={{ fontFamily: 'monospace' }}
                                            >
                                                #{order.orderId}
                                            </Typography>
                                            <Typography variant="body2" fontWeight="bold">
                                                {firstItem?.productName ?? '—'}
                                                {moreCount > 0 && (
                                                    <span style={{ color: '#666', fontWeight: 400 }}>
                                                        {' '}ve {moreCount} ürün daha
                                                    </span>
                                                )}
                                            </Typography>
                                            <Typography
                                                variant="caption"
                                                color="text.secondary"
                                            >
                                                {formatDate(order.createdAt)}
                                            </Typography>
                                        </Box>
                                    </Stack>

                                    <Stack direction="row" alignItems="center" spacing={2}>
                                        <Typography
                                            variant="subtitle1"
                                            fontWeight="bold"
                                            color="primary.main"
                                        >
                                            {formatPrice(order.totalAmount, order.currency)}
                                        </Typography>
                                        <Chip label={cfg.label} color={cfg.color} size="small" />
                                    </Stack>
                                </Stack>
                            </Paper>
                        );
                    })}
                </Stack>
            )}

            {(data?.totalElements ?? 0) > 10 && (
                <TablePagination
                    component="div"
                    count={data?.totalElements ?? 0}
                    page={page}
                    onPageChange={(_, p) => setPage(p)}
                    rowsPerPage={10}
                    rowsPerPageOptions={[10]}
                    labelDisplayedRows={({ from, to, count }) => `${from}–${to} / ${count}`}
                    sx={{ mt: 2 }}
                />
            )}

            {selectedOrder && (
                <OrderDetailModal
                    order={selectedOrder}
                    open={!!selectedOrder}
                    onClose={() => setSelectedOrder(null)}
                />
            )}
        </Box>
    );
};

export default AccountOrders;
