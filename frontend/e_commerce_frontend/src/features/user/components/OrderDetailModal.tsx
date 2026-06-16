import React, { useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography,
    Stack, Box, Avatar, Divider, Chip, Grid, TextField, MenuItem,
    CircularProgress, DialogContentText, Link,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import type { OrderDetail } from '../../../types/order';
import { ORDER_STATUS_CONFIG } from '../../../utils/orderUtils';
import { RETURN_REASONS, RETURN_WINDOW_DAYS, isWithinReturnWindow } from '../../../utils/returnReasons';
import { useCancelOrder, useRequestReturn } from '../../../query/useOrderQueries';
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
    const [returnDialogOpen, setReturnDialogOpen] = useState(false);
    const [returnReasonCode, setReturnReasonCode] = useState('');
    const [returnNote, setReturnNote] = useState('');
    const { mutate: cancelOrder, isPending: isCancelling } = useCancelOrder();
    const { mutate: requestReturn, isPending: isReturning } = useRequestReturn();
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

    const handleReturn = () => {
        if (!returnReasonCode) {
            toast.error('Lütfen bir iade sebebi seçin.');
            return;
        }
        if (returnReasonCode === 'OTHER' && !returnNote.trim()) {
            toast.error('"Diğer" için lütfen kısa bir açıklama yazın.');
            return;
        }
        requestReturn(
            { orderId: order.orderId, reasonCode: returnReasonCode, note: returnNote || undefined },
            {
                onSuccess: () => {
                    toast.success('İade talebiniz alındı. Mağaza onayını bekliyor.');
                    setReturnDialogOpen(false);
                    onClose();
                },
                onError: (err: unknown) => {
                    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
                    toast.error(msg || 'İade talebi oluşturulamadı.');
                },
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
                                            <Link
                                                component={RouterLink}
                                                to={`/product/${item.productId}`}
                                                onClick={onClose}
                                                variant="body2"
                                                fontWeight="bold"
                                                underline="hover"
                                                color="text.primary"
                                                sx={{ '&:hover': { color: 'primary.main' } }}
                                            >
                                                {item.productName}
                                            </Link>
                                            <Typography variant="caption" color="text.secondary" display="block">
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
                    {order.status === 'DELIVERED' && isWithinReturnWindow(order.deliveredAt) && (
                        <Button color="warning" onClick={() => setReturnDialogOpen(true)}>
                            İade Talebi
                        </Button>
                    )}
                    {order.status === 'DELIVERED' && !isWithinReturnWindow(order.deliveredAt) && (
                        <Typography variant="body2" color="text.secondary" sx={{ mr: 'auto', ml: 1 }}>
                            İade süresi ({RETURN_WINDOW_DAYS} gün) doldu.
                        </Typography>
                    )}
                    {order.status === 'RETURN_REQUESTED' && (
                        <Typography variant="body2" color="warning.main" sx={{ mr: 'auto', ml: 1 }}>
                            İade talebiniz mağaza onayını bekliyor.
                        </Typography>
                    )}
                    {order.status === 'RETURN_REJECTED' && (
                        <Typography variant="body2" color="text.secondary" sx={{ mr: 'auto', ml: 1 }}>
                            İade talebiniz reddedildi.
                        </Typography>
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

            {/* İade talebi dialogu */}
            <Dialog
                open={returnDialogOpen}
                onClose={() => setReturnDialogOpen(false)}
                maxWidth="xs"
                fullWidth
            >
                <DialogTitle>İade Talebi Oluştur</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        Talebiniz mağaza tarafından incelenecek; onaylanırsa ödemeniz iade edilir.
                        Teslimden itibaren {RETURN_WINDOW_DAYS} gün içinde iade talep edebilirsiniz.
                    </DialogContentText>
                    <TextField
                        select
                        label="İade Sebebi"
                        fullWidth
                        required
                        value={returnReasonCode}
                        onChange={(e) => setReturnReasonCode(e.target.value)}
                        sx={{ mb: 2 }}
                    >
                        {RETURN_REASONS.map((r) => (
                            <MenuItem key={r.code} value={r.code}>{r.label}</MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label={returnReasonCode === 'OTHER' ? 'Açıklama (zorunlu)' : 'Açıklama (opsiyonel)'}
                        fullWidth
                        multiline
                        rows={2}
                        required={returnReasonCode === 'OTHER'}
                        value={returnNote}
                        onChange={(e) => setReturnNote(e.target.value)}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setReturnDialogOpen(false)} disabled={isReturning}>
                        Vazgeç
                    </Button>
                    <Button
                        color="warning"
                        variant="contained"
                        onClick={handleReturn}
                        disabled={isReturning}
                    >
                        {isReturning ? (
                            <CircularProgress size={18} color="inherit" />
                        ) : (
                            'İade Talebi Gönder'
                        )}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
};

export default OrderDetailModal;
