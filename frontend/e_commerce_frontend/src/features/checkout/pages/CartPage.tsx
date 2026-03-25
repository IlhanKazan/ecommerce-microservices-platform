import React, { useMemo } from 'react';
import {
    Box, Typography, Divider, Button, Paper, IconButton,
    Stack, Grid, Container, LinearProgress, Avatar, CircularProgress, Alert, Snackbar
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { Link as RouterLink } from 'react-router-dom';
import { AppRoutes } from '../../../utils/routes';

import { useCartStore } from '../../../store/useCartStore';
import { useUpdateCartItem, useRemoveFromCart } from '../../../query/useProductQueries';

const SHIPPING_FEE = 39.90;
const FREE_SHIPPING_THRESHOLD = 2000;

const CartPage: React.FC = () => {
    const { items: cart, updateQuantity, removeItem, getTotalPrice } = useCartStore();
    const { mutate: updateApi, isPending: isUpdating } = useUpdateCartItem();
    const { mutate: removeApi, isPending: isRemoving } = useRemoveFromCart();

    const [snackbar, setSnackbar] = React.useState({ open: false, message: '', type: 'error' as 'error' | 'success' });

    const handleQuantityChange = (productId: number, currentQuantity: number, delta: number) => {
        const newQuantity = currentQuantity + delta;
        if (newQuantity < 1) return;

        updateApi(
            { productId, quantity: newQuantity },
            {
                onSuccess: () => updateQuantity(productId, newQuantity),
                onError: (error: any) => {
                    setSnackbar({
                        open: true,
                        message: error.response?.data?.message || 'Stok yetersiz, miktar artırılamadı.',
                        type: 'error'
                    });
                }
            }
        );
    };

    const handleRemoveItem = (productId: number) => {
        removeApi(productId, {
            onSuccess: () => removeItem(productId),
            onError: () => setSnackbar({ open: true, message: 'Ürün silinirken bir hata oluştu.', type: 'error' })
        });
    };

    const totals = useMemo(() => {
        const subtotal = getTotalPrice();
        const shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        const total = subtotal > 0 ? subtotal + shipping : 0;
        const remainingForFreeShipping = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
        const shippingProgress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);

        return { subtotal, total, shipping, remainingForFreeShipping, shippingProgress };
    }, [cart, getTotalPrice]);

    if (cart.length === 0) {
        return (
            <Container maxWidth="md" sx={{ py: 10, textAlign: 'center' }}>
                <Box sx={{ bgcolor: 'grey.100', width: 120, height: 120, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', mx: 'auto', mb: 3 }}>
                    <ShoppingCartIcon sx={{ fontSize: 60, color: 'grey.400' }} />
                </Box>
                <Typography variant="h5" fontWeight="bold" gutterBottom>Sepetiniz şu an boş</Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                    Senin için seçtiğimiz binlerce ürünü keşfetmeye ne dersin?
                </Typography>
                <Button variant="contained" size="large" component={RouterLink} to={AppRoutes.HOME} sx={{ px: 5, borderRadius: 2 }}>
                    Alışverişe Başla
                </Button>
            </Container>
        );
    }

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', py: 4 }}>
            <Container maxWidth="lg">
                <Typography variant="h4" component="h1" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
                    Sepetim ({cart.length} Ürün)
                </Typography>

                <Grid container spacing={4}>
                    <Grid size={{ xs: 12, md: 8 }}>
                        {totals.remainingForFreeShipping > 0 ? (
                            <Paper elevation={0} sx={{ p: 2, mb: 3, border: '1px solid', borderColor: 'warning.light', bgcolor: '#fff8e1' }}>
                                <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                                    <LocalShippingIcon color="warning" fontSize="small" />
                                    <Typography variant="body2" fontWeight="bold" color="text.primary">
                                        {totals.remainingForFreeShipping.toFixed(2)} TL daha ekle, kargo bedava olsun!
                                    </Typography>
                                </Stack>
                                <LinearProgress variant="determinate" value={totals.shippingProgress} color="warning" sx={{ height: 8, borderRadius: 5 }} />
                            </Paper>
                        ) : (
                            <Paper elevation={0} sx={{ p: 2, mb: 3, border: '1px solid', borderColor: 'success.light', bgcolor: '#e8f5e9' }}>
                                <Stack direction="row" alignItems="center" spacing={1}>
                                    <LocalShippingIcon color="success" />
                                    <Typography variant="body2" fontWeight="bold" color="success.main">
                                        Tebrikler! Kargo Bedava fırsatını yakaladın.
                                    </Typography>
                                </Stack>
                            </Paper>
                        )}

                        <Stack spacing={2}>
                            {cart.map(item => (
                                <Paper key={item.productId} elevation={0} sx={{ p: 2, border: '1px solid #e0e0e0', borderRadius: 2, opacity: isUpdating || isRemoving ? 0.7 : 1 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
                                        <Box component={RouterLink} to={`/product/${item.productId}`} sx={{ width: { xs: '100%', sm: 100 }, height: 100, flexShrink: 0, bgcolor: 'grey.50', borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid #eee' }}>
                                            <Avatar src={item.imageUrl} variant="square" sx={{ width: '80%', height: '80%' }} />
                                        </Box>
                                        <Box sx={{ flexGrow: 1, width: '100%', textAlign: { xs: 'center', sm: 'left' } }}>
                                            <Typography variant="subtitle1" fontWeight="bold">{item.name}</Typography>
                                            <Typography variant="caption" color="success.main" fontWeight="bold">Hızlı Teslimat</Typography>
                                        </Box>
                                        <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={3} sx={{ width: { xs: '100%', sm: 'auto' } }}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ddd', borderRadius: 1 }}>
                                                <IconButton size="small" onClick={() => handleQuantityChange(item.productId, item.quantity, -1)} disabled={item.quantity <= 1 || isUpdating}>
                                                    <RemoveIcon fontSize="small" />
                                                </IconButton>
                                                <Typography sx={{ width: 30, textAlign: 'center', fontWeight: 'bold' }}>
                                                    {isUpdating ? '...' : item.quantity}
                                                </Typography>
                                                <IconButton size="small" onClick={() => handleQuantityChange(item.productId, item.quantity, 1)} disabled={isUpdating}>
                                                    <AddIcon fontSize="small" />
                                                </IconButton>
                                            </Box>
                                            <Box sx={{ textAlign: 'right', minWidth: 80 }}>
                                                <Typography variant="h6" color="primary.main" fontWeight="bold">
                                                    {(item.price * item.quantity).toFixed(2)} TL
                                                </Typography>
                                                <IconButton size="small" onClick={() => handleRemoveItem(item.productId)} disabled={isRemoving} sx={{ '&:hover': { color: 'error.main' } }}>
                                                    {isRemoving ? <CircularProgress size={20} /> : <DeleteIcon fontSize="small" />}
                                                </IconButton>
                                            </Box>
                                        </Stack>
                                    </Stack>
                                </Paper>
                            ))}
                        </Stack>
                    </Grid>

                    <Grid size={{ xs: 12, md: 4 }}>
                        <Box sx={{ position: 'sticky', top: 20 }}>
                            <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                                <Typography variant="h6" fontWeight="bold" gutterBottom>Sipariş Özeti</Typography>
                                <Stack spacing={1.5} sx={{ mt: 2 }}>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">Ürünlerin Toplamı</Typography>
                                        <Typography variant="body2" fontWeight="bold">{totals.subtotal.toFixed(2)} TL</Typography>
                                    </Box>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">Kargo Toplam</Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {totals.shipping === 0 ? (
                                                <span style={{ color: 'green', textDecoration: 'line-through' }}>{SHIPPING_FEE} TL</span>
                                            ) : (`${SHIPPING_FEE} TL`)}
                                        </Typography>
                                    </Box>
                                </Stack>
                                <Divider sx={{ my: 2 }} />
                                <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                                    <Typography variant="subtitle1" fontWeight="bold">Toplam</Typography>
                                    <Typography variant="h4" color="primary.main" fontWeight="bold">
                                        {totals.total.toFixed(2)} TL
                                    </Typography>
                                </Box>
                                <Button variant="contained" color="primary" size="large" fullWidth endIcon={<ArrowForwardIcon />} component={RouterLink} to={AppRoutes.CHECKOUT} sx={{ py: 1.5, borderRadius: 2, fontSize: '1.1rem', fontWeight: 'bold' }}>
                                    Sepeti Onayla
                                </Button>
                            </Paper>
                        </Box>
                    </Grid>
                </Grid>
            </Container>

            <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })}>
                <Alert severity={snackbar.type} sx={{ width: '100%' }}>{snackbar.message}</Alert>
            </Snackbar>
        </Box>
    );
};

export default CartPage;