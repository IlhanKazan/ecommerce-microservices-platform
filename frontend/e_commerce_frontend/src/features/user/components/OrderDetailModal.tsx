import React, { useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
    Stack, Box, Avatar, Divider, Chip, Grid, TextField,
    CircularProgress, DialogContentText,
} from '@mui/material';
import type { OrderDetail } from '../../../types/order';
import { ORDER_STATUS_CONFIG } from '../../../utils/orderUtils';
import { useCancelOrder } from '../../../query/useOrderQueries';
import { useToastStore } from '../../../store/useToastStore';
import { formatPrice } from '../../../utils/formatPrice';
import { formatDateTime } from '../../../utils/formatDate';

interface Props {
    order: OrderDetail;
    open: boolean;
    onClose: () => void;
}

const OrderDetailModal: React.FC<Props> = ({ order, open, onClose }) => {
    const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
    const [cancelReason, setCancelReason] = useState('');
    const { mutate: cancelOrder, isPending: isCancelling } = useCancelOrder();
    const toast = useToastStore();

    let parsedAddress: Record<string, string> = {};
    try {
        parsedAddress = JSON.parse(order.shippingAddressJson);
    } catch {
        // Geçersiz JSON sessizce geçilir
    }

    const cfg = ORDER_STATUS_CONFIG[order.status];

    const handleCancel = () => {
        cancelOrder(
            { orderId: order.orderId, reason: cancelReason || undefined },
            {
                onSuccess: () => {
                    toast.success('Sipariş iptal edildi.');
                    setCancelDialogOpen(false);
                    onClose();
                },
                onError: () => toast.error('İptal işlemi başarısız oldu.'),
            },
        );
    };

    return (
        <>
            <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
                <DialogTitle>
                    <Stack direction="row" alignItems="center" spacing={1.5}>
                        <Typography fontWeight="bold">Sipariş #{order.orderId}</Typography>
                        <Chip label={cfg.label} color={cfg.color} size="small" />
                    </Stack>
                </DialogTitle>

                <DialogContent dividers>
                    <Grid container spacing={3}>
                        {/* Teslimat adresi */}
                        <Grid item xs={12} sm={5}>
                            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                                Teslimat Adresi
                            </Typography>
                            <Typography variant="body2">{parsedAddress.contactName}</Typography>
                            <Typography variant="body2">{parsedAddress.fullAddress}</Typography>
                            <Typography variant="body2">
                                {parsedAddress.city}, {parsedAddress.zipCode}
                            </Typography>
                            <Typography variant="body2">{parsedAddress.country}</Typography>
                            <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{ mt: 1, display: 'block' }}
                            >
                                {formatDateTime(order.createdAt)}
                            </Typography>
                        </Grid>

                        {/* Ürünler */}
                        <Grid item xs={12} sm={7}>
                            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                                Ürünler
                            </Typography>
                            <Stack spacing={1.5}>
                                {order.items.map((item) => (
                                    <Stack
                                        key={item.productId}
                                        direction="row"
                                        alignItems="center"
                                        spacing={1.5}
                                    >
                                        <Avatar
                                            src={item.productImageUrl ?? undefined}
                                            variant="rounded"
                                            sx={{ width: 44, height: 44, bgcolor: 'grey.100' }}
                                        />
                                        <Box sx={{ flexGrow: 1 }}>
                                            <Typography variant="body2" fontWeight="bold">
                                                {item.productName}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                {item.sku}
                                            </Typography>
                                        </Box>
                                        <Box sx={{ textAlign: 'right', minWidth: 80 }}>
                                            <Typography variant="body2">
                                                {item.quantity} × {formatPrice(item.unitPrice)}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                {formatPrice(item.unitPrice * item.quantity)}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                ))}
                                <Divider />
                                <Stack direction="row" justifyContent="space-between">
                                    <Typography fontWeight="bold">Toplam</Typography>
                                    <Typography fontWeight="bold" color="primary.main">
                                        {formatPrice(order.totalAmount, order.currency)}
                                    </Typography>
                                </Stack>
                            </Stack>
                        </Grid>
                    </Grid>
                </DialogContent>

                <DialogActions>
                    {order.status === 'CONFIRMED' && (
                        <Button color="error" onClick={() => setCancelDialogOpen(true)}>
                            İptal Et
                        </Button>
                    )}
                    <Button onClick={onClose}>Kapat</Button>
                </DialogActions>
            </Dialog>

            {/* İptal onay dialogu */}
            <Dialog
                open={cancelDialogOpen}
                onClose={() => setCancelDialogOpen(false)}
                maxWidth="xs"
                fullWidth
            >
                <DialogTitle>Siparişi İptal Et</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        Bu siparişi iptal etmek istediğinizden emin misiniz? İade işlemi
                        başlatılacaktır.
                    </DialogContentText>
                    <TextField
                        label="İptal Sebebi (opsiyonel)"
                        fullWidth
                        multiline
                        rows={2}
                        value={cancelReason}
                        onChange={(e) => setCancelReason(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => setCancelDialogOpen(false)}
                        disabled={isCancelling}
                    >
                        Vazgeç
                    </Button>
                    <Button
                        color="error"
                        variant="contained"
                        onClick={handleCancel}
                        disabled={isCancelling}
                    >
                        {isCancelling ? (
                            <CircularProgress size={18} color="inherit" />
                        ) : (
                            'İptal Et'
                        )}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
};

export default OrderDetailModal;
