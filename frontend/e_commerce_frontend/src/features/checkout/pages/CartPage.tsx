import React, { useState, useMemo } from 'react';
import {
    Box, Typography, Divider, Button, Paper, IconButton,
    Stack, Grid, Container, LinearProgress, Avatar
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { Link as RouterLink } from 'react-router-dom';
import { AppRoutes } from '../../../utils/routes';
import photo from '../../../components/customer/react.svg';

interface CartItem {
    id: string;
    name: string;
    price: number;
    quantity: number;
    imageUrl: string;
    selectedAttributes?: string;
}

const initialCart: CartItem[] = [
    { id: 'PRD001', name: 'Oversize Basic Tişört', price: 299.99, quantity: 2, imageUrl: '', selectedAttributes: 'Beden: L' },
    { id: 'PRD002', name: 'Kablosuz Noise Cancelling Kulaklık', price: 1499.00, quantity: 1, imageUrl: '', selectedAttributes: 'Renk: Siyah' },
];

const SHIPPING_FEE = 39.90;
const FREE_SHIPPING_THRESHOLD = 2000;

const CartPage: React.FC = () => {
    const [cart, setCart] = useState<CartItem[]>(initialCart);

    const handleQuantityChange = (itemId: string, delta: number) => {
        setCart(prevCart =>
            prevCart.map(item =>
                item.id === itemId
                    ? { ...item, quantity: Math.max(1, item.quantity + delta) }
                    : item
            )
        );
    };

    const handleRemoveItem = (itemId: string) => {
        setCart(prevCart => prevCart.filter(item => item.id !== itemId));
    };

    const totals = useMemo(() => {
        const subtotal = cart.reduce((acc, item) => acc + item.price * item.quantity, 0);
        const shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        const total = subtotal > 0 ? subtotal + shipping : 0;

        const remainingForFreeShipping = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
        const shippingProgress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);

        return { subtotal, total, shipping, remainingForFreeShipping, shippingProgress };
    }, [cart]);

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
                                <LinearProgress
                                    variant="determinate"
                                    value={totals.shippingProgress}
                                    color="warning"
                                    sx={{ height: 8, borderRadius: 5, bgcolor: 'rgba(0,0,0,0.05)' }}
                                />
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
                                <Paper key={item.id} elevation={0} sx={{ p: 2, border: '1px solid #e0e0e0', borderRadius: 2 }}>
                                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">

                                        <Box
                                            component={RouterLink}
                                            to={`/product/${item.id}`}
                                            sx={{ width: { xs: '100%', sm: 100 }, height: 100, flexShrink: 0, bgcolor: 'grey.50', borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1px solid #eee' }}
                                        >
                                            <Avatar src={photo} variant="square" sx={{ width: '80%', height: '80%' }} />
                                        </Box>

                                        <Box sx={{ flexGrow: 1, width: '100%', textAlign: { xs: 'center', sm: 'left' } }}>
                                            <Typography variant="subtitle1" fontWeight="bold">
                                                {item.name}
                                            </Typography>
                                            {item.selectedAttributes && (
                                                <Typography variant="caption" color="text.secondary" display="block">
                                                    {item.selectedAttributes}
                                                </Typography>
                                            )}
                                            <Typography variant="caption" color="success.main" fontWeight="bold">
                                                Hızlı Teslimat
                                            </Typography>
                                        </Box>

                                        <Stack
                                            direction="row"
                                            alignItems="center"
                                            justifyContent="space-between"
                                            spacing={3}
                                            sx={{ width: { xs: '100%', sm: 'auto' } }}
                                        >
                                            <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ddd', borderRadius: 1 }}>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => handleQuantityChange(item.id, -1)}
                                                    disabled={item.quantity === 1}
                                                >
                                                    <RemoveIcon fontSize="small" />
                                                </IconButton>
                                                <Typography sx={{ width: 30, textAlign: 'center', fontWeight: 'bold' }}>{item.quantity}</Typography>
                                                <IconButton size="small" onClick={() => handleQuantityChange(item.id, 1)}>
                                                    <AddIcon fontSize="small" />
                                                </IconButton>
                                            </Box>

                                            <Box sx={{ textAlign: 'right', minWidth: 80 }}>
                                                <Typography variant="h6" color="primary.main" fontWeight="bold">
                                                    {(item.price * item.quantity).toFixed(2)} TL
                                                </Typography>
                                                <IconButton
                                                    size="small"
                                                    color="text"
                                                    onClick={() => handleRemoveItem(item.id)}
                                                    sx={{ '&:hover': { color: 'error.main' } }}
                                                >
                                                    <DeleteIcon fontSize="small" />
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
                                        <Typography variant="body2" color="text.secondary">Ürünün Toplamı</Typography>
                                        <Typography variant="body2" fontWeight="bold">{totals.subtotal.toFixed(2)} TL</Typography>
                                    </Box>
                                    <Box display="flex" justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">Kargo Toplam</Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {totals.shipping === 0 ? (
                                                <span style={{ color: 'green', textDecoration: 'line-through' }}>{SHIPPING_FEE} TL</span>
                                            ) : (
                                                `${SHIPPING_FEE} TL`
                                            )}
                                        </Typography>
                                    </Box>
                                    {totals.shipping === 0 && (
                                        <Typography variant="caption" color="success.main" align="right" display="block">
                                            Kargo Bedava
                                        </Typography>
                                    )}
                                </Stack>

                                <Divider sx={{ my: 2 }} />

                                <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                                    <Typography variant="subtitle1" fontWeight="bold">Toplam</Typography>
                                    <Typography variant="h4" color="primary.main" fontWeight="bold">
                                        {totals.total.toFixed(2)} TL
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
                                    sx={{ py: 1.5, borderRadius: 2, fontSize: '1.1rem', fontWeight: 'bold' }}
                                >
                                    Sepeti Onayla
                                </Button>

                                <Box sx={{ mt: 2, textAlign: 'center' }}>
                                    <Typography variant="caption" color="text.secondary">
                                        Güvenli Alışveriş & İade Garantisi
                                    </Typography>
                                </Box>
                            </Paper>
                        </Box>
                    </Grid>
                </Grid>
            </Container>
        </Box>
    );
};

export default CartPage;