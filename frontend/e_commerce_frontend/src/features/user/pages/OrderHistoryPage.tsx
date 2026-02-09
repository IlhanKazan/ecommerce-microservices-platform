import React, { useEffect, useState } from 'react';
import {
    Box,
    Typography,
    Divider,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Alert,
    Button
} from '@mui/material';
import LoadingSpinner from "../../../components/shared/LoadingSpinner.tsx";
import type {Order} from '../../../types';
import {mockOrders} from '../../../data/mockOrders.ts';
import { OrderStatus } from '../../../types/enums';

const OrderHistoryPage: React.FC = () => {
    const [orders, setOrders] = useState<Order[] | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [hasError, setHasError] = useState(false);

    const getStatusColor = (status: Order['status']): "primary" | "secondary" | "success" | "error" | "info" | "warning" | "inherit" => {
        switch (status) {
            case OrderStatus.COMPLETED: return 'success';
            case OrderStatus.SHIPPED: return 'info';
            case OrderStatus.PROCESSING: return 'warning';
            case OrderStatus.CANCELLED: return 'error';
            case OrderStatus.PENDING: return 'secondary';
            default: return 'inherit';
        }
    };

    const statusLabelMap: Record<Order['status'], string> = {
        [OrderStatus.COMPLETED]: 'Tamamlandı',
        [OrderStatus.SHIPPED]: 'Kargoda',
        [OrderStatus.PROCESSING]: 'Hazırlanıyor',
        [OrderStatus.CANCELLED]: 'İptal Edildi',
        [OrderStatus.PENDING]: 'Beklemede',
    } as const;

    useEffect(() => {
        const fetchOrders = async () => {
            setIsLoading(true);
            setHasError(false);
            try {

                await new Promise(resolve => setTimeout(resolve, 2000));
                setOrders(mockOrders);

            } catch (error) {

                setHasError(true);
                setOrders(null);
                console.log(error);

            } finally {

                setIsLoading(false);

            }
        };

        fetchOrders();
    }, []);

    if (isLoading) {
        return (
            <LoadingSpinner/>
        );
    }

    if (hasError) {
        return (
            <Box sx={{ py: 5 }}>
                <Alert severity="error">
                    Sipariş geçmişiniz yüklenirken bir sorun oluştu. Lütfen daha sonra tekrar deneyin.
                </Alert>
            </Box>
        );
    }

    if (orders && orders.length === 0) {
        return (
            <Box sx={{ py: 5, textAlign: 'center' }}>
                <Alert severity="info">
                    Henüz tamamlanmış bir siparişiniz bulunmamaktadır. Alışverişe başlayın!
                </Alert>
                <Button variant="contained" sx={{ mt: 3 }}>Alışverişe Başla</Button>
            </Box>
        );
    }

    return (
        <Box sx={{ py: { xs: 4, md: 8 } }}>
            <Typography variant="h4" component="h1" gutterBottom>
                Sipariş Geçmişi ({orders?.length || 0} Sipariş)
            </Typography>
            <Divider sx={{ mb: 4 }} />

            <TableContainer component={Paper} elevation={3}>
                <Table sx={{ minWidth: 650 }} aria-label="Sipariş Geçmişi Tablosu">
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Sipariş ID</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Tarih</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Toplam Tutar</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold' }}>Durum</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold' }}>Detay</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {orders?.map((order) => (
                            <TableRow
                                key={order.id}
                                sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                            >
                                <TableCell component="th" scope="row">
                                    {order.id}
                                </TableCell>
                                <TableCell align="right">{order.date}</TableCell>
                                <TableCell align="right">{order.total.toFixed(2)} TL</TableCell>
                                <TableCell align="center">
                                    <Button size="small" variant="contained" color={getStatusColor(order.status)} >
                                        {statusLabelMap[order.status] || String(order.status)}
                                    </Button>
                                </TableCell>
                                <TableCell align="center">
                                    <Button variant="outlined" size="small">
                                        Görüntüle
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default OrderHistoryPage;