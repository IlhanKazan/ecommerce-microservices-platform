import React, { useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
    Stack, Box, Avatar, Chip, Table, TableBody, TableCell,
    TableHead, TableRow, TextField, CircularProgress,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import type { OrderDetail } from '../../../types/order';
import { ORDER_STATUS_CONFIG } from '../../../utils/orderUtils';
import { useUpdateOrderStatus } from '../../../query/useOrderQueries';
import { useToastStore } from '../../../store/useToastStore';
import { formatPrice } from '../../../utils/formatPrice';
import { formatDateTime } from '../../../utils/formatDate';

interface Props {
    order: OrderDetail;
    open: boolean;
    tenantId: number;
    onClose: () => void;
}

const MerchantOrderDetailModal: React.FC<Props> = ({ order, open, tenantId, onClose }) => {
    const [trackingNumber, setTrackingNumber] = useState('');
    const [showTrackingInput, setShowTrackingInput] = useState(false);
    const { mutate: updateStatus, isPending } = useUpdateOrderStatus();
    const toast = useToastStore();

    let parsedAddress: Record<string, string> = {};
    try {
        parsedAddress = JSON.parse(order.shippingAddressJson);
    } catch {
        // Geçersiz JSON sessizce geçilir
    }

    const cfg = ORDER_STATUS_CONFIG[order.status];

    const handleShip = () => {
        updateStatus(
            {
                tenantId,
                orderId: order.orderId,
                status: 'SHIPPED',
                trackingNumber: trackingNumber || undefined,
            },
            {
                onSuccess: () => {
                    toast.success('Sipariş kargoya verildi.');
                    setShowTrackingInput(false);
                    onClose();
                },
                onError: () => toast.error('İşlem başarısız oldu.'),
            },
        );
    };

    const handleDeliver = () => {
        updateStatus(
            { tenantId, orderId: order.orderId, status: 'DELIVERED' },
            {
                onSuccess: () => {
                    toast.success('Sipariş teslim edildi.');
                    onClose();
                },
                onError: () => toast.error('İşlem başarısız oldu.'),
            },
        );
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                <Stack direction="row" alignItems="center" spacing={1.5}>
                    <Typography fontWeight="bold">#{order.orderId} — Sipariş Detayı</Typography>
                    <Chip label={cfg.label} color={cfg.color} size="small" />
                </Stack>
            </DialogTitle>

            <DialogContent dividers>
                <Stack spacing={3}>
                    {/* Teslimat adresi */}
                    <Box>
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            Teslimat Adresi
                        </Typography>
                        <Typography variant="body2">{parsedAddress.contactName}</Typography>
                        <Typography variant="body2">{parsedAddress.fullAddress}</Typography>
                        <Typography variant="body2">
                            {parsedAddress.city}, {parsedAddress.zipCode}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {formatDateTime(order.createdAt)}
                        </Typography>
                    </Box>

                    {/* Ürünler tablosu */}
                    <Box>
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            Ürünler
                        </Typography>
                        <Table size="small">
                            <TableHead>
                                <TableRow sx={{ '& th': { fontWeight: 'bold' } }}>
                                    <TableCell>SKU</TableCell>
                                    <TableCell>Ürün</TableCell>
                                    <TableCell align="center">Adet</TableCell>
                                    <TableCell align="right">Birim Fiyat</TableCell>
                                    <TableCell align="right">Toplam</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {order.items.map((item) => (
                                    <TableRow key={item.productId}>
                                        <TableCell
                                            sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}
                                        >
                                            {item.sku}
                                        </TableCell>
                                        <TableCell>
                                            <Stack direction="row" alignItems="center" spacing={1}>
                                                <Avatar
                                                    src={item.productImageUrl ?? undefined}
                                                    variant="rounded"
                                                    sx={{ width: 32, height: 32 }}
                                                />
                                                <Typography variant="body2">
                                                    {item.productName}
                                                </Typography>
                                            </Stack>
                                        </TableCell>
                                        <TableCell align="center">{item.quantity}</TableCell>
                                        <TableCell align="right">
                                            {formatPrice(item.unitPrice)}
                                        </TableCell>
                                        <TableCell align="right">
                                            {formatPrice(item.unitPrice * item.quantity)}
                                        </TableCell>
                                    </TableRow>
                                ))}
                                <TableRow>
                                    <TableCell colSpan={3} />
                                    <TableCell align="right">
                                        <Typography fontWeight="bold">Genel Toplam</Typography>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography fontWeight="bold" color="primary.main">
                                            {formatPrice(order.totalAmount, order.currency)}
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </Box>

                    {/* Kargo durumu bilgisi */}
                    {(order.status === 'SHIPPED' || order.status === 'DELIVERED') && (
                        <Box>
                            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                                Kargo Bilgisi
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Kargo durumu: {cfg.label}
                            </Typography>
                        </Box>
                    )}

                    {/* Kargoya ver input (CONFIRMED ise göster) */}
                    {order.status === 'CONFIRMED' && showTrackingInput && (
                        <Box>
                            <TextField
                                label="Kargo Takip Numarası (opsiyonel)"
                                fullWidth
                                value={trackingNumber}
                                onChange={(e) => setTrackingNumber(e.target.value)}
                            />
                        </Box>
                    )}
                </Stack>
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose}>Kapat</Button>
                {order.status === 'CONFIRMED' && !showTrackingInput && (
                    <Button
                        variant="outlined"
                        color="info"
                        startIcon={<LocalShippingIcon />}
                        onClick={() => setShowTrackingInput(true)}
                    >
                        Kargoya Ver
                    </Button>
                )}
                {order.status === 'CONFIRMED' && showTrackingInput && (
                    <Button
                        variant="contained"
                        color="info"
                        onClick={handleShip}
                        disabled={isPending}
                    >
                        {isPending ? (
                            <CircularProgress size={18} color="inherit" />
                        ) : (
                            'Kargoya Gönder'
                        )}
                    </Button>
                )}
                {order.status === 'SHIPPED' && (
                    <Button
                        variant="contained"
                        color="success"
                        onClick={handleDeliver}
                        disabled={isPending}
                    >
                        {isPending ? (
                            <CircularProgress size={18} color="inherit" />
                        ) : (
                            'Teslim Edildi'
                        )}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
};

export default MerchantOrderDetailModal;
