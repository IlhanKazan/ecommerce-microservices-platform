import React, { useMemo } from 'react';
import {
    Box, Typography, Divider, Button, Paper, IconButton,
    Stack, Grid, Container, LinearProgress, Avatar, CircularProgress
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { Link as RouterLink } from 'react-router-dom';
import { AppRoutes } from '../../../utils/routes';
import { useBasket, useRemoveFromBasket, useAddToBasket } from '../../../query/useBasketQueries';
import { useToastStore } from '../../../store/useToastStore';
import { useCartStore } from '../../../store/useCartStore';
import { useAuthStore } from '../../../store/useAuthStore'; // Auth store eklendi

const SHIPPING_FEE = 39.90;
const FREE_SHIPPING_THRESHOLD = 2000;

const formatPrice = (price: number) =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(price);

const CartPage: React.FC = () => {
    // Auth Kontrolü
    const isAuthenticated = useAuthStore(state => state.isAuthenticated);

    // API Hooks
    const { data: basket, isLoading: isApiLoading } = useBasket();
    const { mutate: removeItemApi, isPending: isRemoving, variables: removingId } = useRemoveFromBasket();
    const { mutate: addToBasketApi, isPending: isAdding } = useAddToBasket();

    // Local Store & Toast
    const localCart = useCartStore();
    const toast = useToastStore();

    // Veri Kaynağını Belirle (Çorba olmayı engelleyen kısım)
    const displayItems = isAuthenticated ? (basket?.items ?? []) : localCart.items;
    const isLoading = isAuthenticated ? isApiLoading : false;

    // --- İŞLEM FONKSİYONLARI ---
    const handleIncrease = (productId: number, currentQuantity: number) => {
        if (isAuthenticated) {
            addToBasketApi(
                { productId, quantity: 1 },
                { onError: () => toast.error('Stok yetersiz.') }
            );
        } else {
            localCart.updateQuantity(productId, currentQuantity + 1);
        }
    };

    const handleDecrease = (productId: number, currentQuantity: number) => {
        if (currentQuantity <= 1) {
            handleRemove(productId);
            return;
        }

        if (isAuthenticated) {
            // TODO: Backend PUT endpoint hazır olunca burası güncellenir
            removeItemApi(productId, {
                onSuccess: () => {
                    addToBasketApi({ productId, quantity: currentQuantity - 1 });
                },
                onError: () => toast.error('Miktar azaltılırken hata oluştu.')
            });
        } else {
            localCart.updateQuantity(productId, currentQuantity - 1);
        }
    };

    const handleRemove = (productId: number) => {
        if (isAuthenticated) {
            removeItemApi(productId, {
                onSuccess: () => toast.success('Ürün sepetten kaldırıldı.'),
                onError: () => toast.error('Ürün kaldırılırken hata oluştu.')
            });
        } else {
            localCart.removeItem(productId);
            toast.success('Ürün sepetten kaldırıldı.');
        }
    };

    // Fiyat Hesaplamaları
    const totals = useMemo(() => {
        let subtotal = 0;
        if (isAuthenticated) {
            subtotal = basket?.totalPrice ?? 0;
        } else {
            subtotal = localCart.getTotalPrice();
        }
        const shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        const total = subtotal > 0 ? subtotal + shipping : 0;
        const remainingForFreeShipping = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
        const shippingProgress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);
        return { subtotal, total, shipping, remainingForFreeShipping, shippingProgress };
    }, [basket, localCart.items, isAuthenticated]);

    // RENDER BLOKLARI
    if (isLoading) {
        return (
            <Container maxWidth="lg" sx={{ py: 8, display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
            </Container>
        );
    }

    if (displayItems.length === 0) {
        return (
            <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', py: 4 }}>
                <Container maxWidth="md" sx={{ py: 10, textAlign: 'center' }}>
                    <Box sx={{
                        bgcolor: 'grey.100', width: 120, height: 120, borderRadius: '50%',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        mx: 'auto', mb: 3
                    }}>
                        <ShoppingCartIcon sx={{ fontSize: 60, color: 'grey.400' }} />
                    </Box>
                    <Typography variant="h5" fontWeight="bold" gutterBottom>
                        Sepetiniz şu an boş
                    </Typography>
                    <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                        Binlerce ürünü keşfetmeye ne dersin?
                    </Typography>
                    <Button
                        variant="contained"
                        size="large"
                        component={RouterLink}
                        to={AppRoutes.PRODUCT_LIST}
                        sx={{ px: 5, borderRadius: 2 }}
                    >
                        Alışverişe Başla
                    </Button>
                </Container>
            </Box>
        );
    }

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', py: 4 }}>
            <Container maxWidth="lg">
                <Typography variant="h4" component="h1" fontWeight="bold" gutterBottom sx={{ mb: 3 }}>
                    Sepetim ({displayItems.length} Ürün)
                </Typography>

                <Grid container spacing={4}>
                    <Grid size={{ xs: 12, md: 8 }}>
                        {/* Ücretsiz kargo progress bar */}
                        {totals.remainingForFreeShipping > 0 ? (
                            <Paper elevation={0} sx={{ p: 2, mb: 3, border: '1px solid', borderColor: 'warning.light', bgcolor: '#fff8e1' }}>
                                <Stack direction="row" alignItems="center" spacing={1} mb={1}>
                                    <LocalShippingIcon color="warning" fontSize="small" />
                                    <Typography variant="body2" fontWeight="bold">
                                        {formatPrice(totals.remainingForFreeShipping)} daha ekle, kargo bedava!
                                    </Typography>
                                </Stack>
                                <LinearProgress
                                    variant="determinate"
                                    value={totals.shippingProgress}
                                    color="warning"
                                    sx={{ height: 8, borderRadius: 5 }}
                                />
                            </Paper>
                        ) : (
                            <Paper elevation={0} sx={{ p: 2, mb: 3, border: '1px solid', borderColor: 'success.light', bgcolor: '#e8f5e9' }}>
                                <Stack direction="row" alignItems="center" spacing={1}>
                                    <LocalShippingIcon color="success" />
                                    <Typography variant="body2" fontWeight="bold" color="success.main">
                                        Tebrikler! Kargo bedava fırsatını yakaladın.
                                    </Typography>
                                </Stack>
                            </Paper>
                        )}

                        <Stack spacing={2}>
                            {displayItems.map((item: any) => {
                                const isThisRemoving = isRemoving && removingId === item.productId;
                                // Hem backend (productName/imageUrl) hem local (name/mainImageUrl) tiplerini tolere et
                                const itemName = item.productName || item.name;
                                const itemImage = item.imageUrl || item.mainImageUrl;

                                return (
                                    <Paper
                                        key={item.productId}
                                        elevation={0}
                                        sx={{
                                            p: 2,
                                            border: '1px solid #e0e0e0',
                                            borderRadius: 2,
                                            opacity: isThisRemoving ? 0.5 : 1,
                                            transition: 'opacity 0.2s'
                                        }}
                                    >
                                        <Stack
                                            direction={{ xs: 'column', sm: 'row' }}
                                            spacing={2}
                                            alignItems="center"
                                        >
                                            <Box
                                                component={RouterLink}
                                                to={`/product/${item.productId}`}
                                                sx={{
                                                    width: { xs: '100%', sm: 100 },
                                                    height: 100,
                                                    flexShrink: 0,
                                                    bgcolor: 'grey.50',
                                                    borderRadius: 2,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    border: '1px solid #eee',
                                                    overflow: 'hidden',
                                                }}
                                            >
                                                <Avatar
                                                    src={itemImage ?? undefined}
                                                    variant="square"
                                                    sx={{ width: '80%', height: '80%', objectFit: 'contain' }}
                                                />
                                            </Box>

                                            <Box sx={{ flexGrow: 1, width: '100%', textAlign: { xs: 'center', sm: 'left' } }}>
                                                <Typography variant="subtitle1" fontWeight="bold">
                                                    {itemName}
                                                </Typography>
                                                <Typography variant="caption" color="success.main" fontWeight="bold">
                                                    Hızlı Teslimat
                                                </Typography>
                                            </Box>

                                            <Stack
                                                direction="row"
                                                alignItems="center"
                                                justifyContent="space-between"
                                                spacing={2}
                                                sx={{ width: { xs: '100%', sm: 'auto' } }}
                                            >
                                                <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ddd', borderRadius: 1 }}>
                                                    <IconButton
                                                        size="small"
                                                        onClick={() => handleDecrease(item.productId, item.quantity)}
                                                        disabled={isAdding || isRemoving}
                                                    >
                                                        <RemoveIcon fontSize="small" />
                                                    </IconButton>
                                                    <Typography sx={{ width: 30, textAlign: 'center', fontWeight: 'bold' }}>
                                                        {item.quantity}
                                                    </Typography>
                                                    <IconButton
                                                        size="small"
                                                        onClick={() => handleIncrease(item.productId, item.quantity)}
                                                        disabled={isAdding || isRemoving}
                                                    >
                                                        <AddIcon fontSize="small" />
                                                    </IconButton>
                                                </Box>

                                                <Box sx={{ textAlign: 'right', minWidth: 90 }}>
                                                    <Typography variant="h6" color="primary.main" fontWeight="bold">
                                                        {formatPrice(item.price * item.quantity)}
                                                    </Typography>
                                                    <IconButton
                                                        size="small"
                                                        onClick={() => handleRemove(item.productId)}
                                                        disabled={isThisRemoving}
                                                        sx={{ '&:hover': { color: 'error.main' } }}
                                                    >
                                                        {isThisRemoving
                                                            ? <CircularProgress size={18} />
                                                            : <DeleteIcon fontSize="small" />
                                                        }
                                                    </IconButton>
                                                </Box>
                                            </Stack>
                                        </Stack>
                                    </Paper>
                                );
                            })}
                        </Stack>
                    </Grid>

                    <Grid size={{ xs: 12, md: 4 }}>
                        <Box sx={{ position: 'sticky', top: 20 }}>
                            <Paper elevation={0} sx={{ p: 3, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                                <Typography variant="h6" fontWeight="bold" gutterBottom>
                                    Sipariş Özeti
                                </Typography>
                                <Stack spacing={1.5} sx={{ mt: 2 }}>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">
                                            Ara Toplam
                                        </Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {formatPrice(totals.subtotal)}
                                        </Typography>
                                    </Box>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">
                                            Kargo
                                        </Typography>
                                        <Typography
                                            variant="body2"
                                            fontWeight="bold"
                                            color={totals.shipping === 0 ? 'success.main' : 'text.primary'}
                                        >
                                            {totals.shipping === 0
                                                ? 'Ücretsiz'
                                                : formatPrice(SHIPPING_FEE)
                                            }
                                        </Typography>
                                    </Box>
                                </Stack>
                                <Divider sx={{ my: 2 }} />
                                <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                                    <Typography variant="subtitle1" fontWeight="bold">Toplam</Typography>
                                    <Typography variant="h5" color="primary.main" fontWeight="bold">
                                        {formatPrice(totals.total)}
                                    </Typography>
                                </Box>
                                <Button
                                    variant="contained"
                                    color="primary"
                                    size="large"
                                    fullWidth
                                    endIcon={<ArrowForwardIcon />}
                                    component={RouterLink}
                                    to={AppRoutes.CHECKOUT}
                                    sx={{ py: 1.5, borderRadius: 2, fontSize: '1rem', fontWeight: 'bold' }}
                                >
                                    Siparişi Tamamla
                                </Button>
                            </Paper>
                        </Box>
                    </Grid>
                </Grid>
            </Container>
        </Box>
    );
};

export default CartPage;