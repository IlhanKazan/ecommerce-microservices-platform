import React, { useState } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import {
    Box, Typography, CircularProgress, Alert,
    Button, Stack, Rating, Divider, Container, Grid,
    Paper, IconButton, Breadcrumbs, Link, Tabs, Tab,
    Snackbar
} from '@mui/material';
import {
    ShoppingCart as ShoppingCartIcon,
    FavoriteBorder,
    LocalShipping,
    VerifiedUser,
    Cached,
    Add,
    Remove
} from '@mui/icons-material';

import { useGetProductDetail, useAddToCart } from '../../../query/useProductQueries';
import { useCartStore } from '../../../store/useCartStore';
import photo from '../../../components/customer/react.svg';

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

function CustomTabPanel(props: TabPanelProps) {
    const { children, value, index, ...other } = props;
    return (
        <div role="tabpanel" hidden={value !== index} {...other}>
            {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
        </div>
    );
}

const ProductDetailPage: React.FC = () => {
    const { productId } = useParams<{ productId: string }>();
    const id = parseInt(productId || '0', 10);

    const { data: product, isLoading, isError } = useGetProductDetail(id);

    const { mutate: addToCartApi, isPending: isAddingToCart } = useAddToCart();
    const addItemToStore = useCartStore(state => state.addItem);

    const [quantity, setQuantity] = useState(1);
    const [tabValue, setTabValue] = useState(0);

    const [snackbar, setSnackbar] = useState({
        open: false,
        message: '',
        severity: 'success' as 'success' | 'error' | 'warning' | 'info'
    });

    const handleQuantityChange = (type: 'increase' | 'decrease') => {
        if (type === 'decrease' && quantity > 1) setQuantity(prev => prev - 1);
        if (type === 'increase') setQuantity(prev => prev + 1);
    };

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    const handleCloseSnackbar = () => {
        setSnackbar(prev => ({ ...prev, open: false }));
    };

    const handleAddToCart = () => {
        if (!product) return;

        addToCartApi(
            { productId: product.id, quantity },
            {
                onSuccess: () => {
                    addItemToStore({
                        productId: product.id,
                        name: product.name,
                        price: product.price,
                        quantity: quantity,
                        imageUrl: product.imageUrl
                    });

                    setSnackbar({
                        open: true,
                        message: `${quantity} adet ürün sepetinize eklendi!`,
                        severity: 'success'
                    });
                },
                onError: (error: any) => {
                    const errorMessage = error.response?.data?.message || 'Ürün sepete eklenirken bir hata oluştu (Stok yetersiz olabilir).';
                    setSnackbar({
                        open: true,
                        message: errorMessage,
                        severity: 'error'
                    });
                }
            }
        );
    };

    if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 20 }}><CircularProgress /></Box>;
    if (isError || !product) return <Container><Alert severity="error" sx={{ mt: 5 }}>Ürün bulunamadı veya yayından kaldırılmış olabilir.</Alert></Container>;

    const formattedPrice = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(product.price);

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 8 }}>
            <Box sx={{ bgcolor: 'white', borderBottom: '1px solid', borderColor: 'divider', py: 2 }}>
                <Container maxWidth="lg">
                    <Breadcrumbs aria-label="breadcrumb" sx={{ fontSize: '0.9rem' }}>
                        <Link component={RouterLink} to="/" underline="hover" color="inherit">Anasayfa</Link>
                        <Link component={RouterLink} to="/productlist" underline="hover" color="inherit">Ürünler</Link>
                        <Typography color="text.primary">{product.category || 'Genel'}</Typography>
                        <Typography color="text.primary">{product.name}</Typography>
                    </Breadcrumbs>
                </Container>
            </Box>

            <Container maxWidth="lg" sx={{ mt: 4 }}>
                <Grid container spacing={6}>
                    <Grid size={{ xs: 12, md: 6 }}>
                        <Paper elevation={0} sx={{ border: '1px solid #e0e0e0', borderRadius: 4, overflow: 'hidden', position: 'relative', bgcolor: 'white' }}>
                            <IconButton sx={{ position: 'absolute', top: 15, right: 15, bgcolor: 'white', boxShadow: 1, '&:hover': { color: 'red' } }}>
                                <FavoriteBorder />
                            </IconButton>
                            <Box sx={{ p: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
                                <img src={product.imageUrl || photo} alt={product.name} style={{ maxWidth: '100%', maxHeight: 400, objectFit: 'contain' }} />
                            </Box>
                        </Paper>
                    </Grid>

                    <Grid size={{ xs: 12, md: 6 }}>
                        <Box>
                            <Typography variant="h4" component="h1" fontWeight="bold" color="text.primary" gutterBottom>
                                {product.name}
                            </Typography>

                            <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                                <Rating value={product.rating || 0} precision={0.5} readOnly size="small" />
                                <Divider orientation="vertical" flexItem sx={{ height: 15, alignSelf: 'center' }} />
                                <Typography variant="caption" color="text.secondary">Ürün Kodu: #{product.id}</Typography>
                            </Stack>

                            <Box sx={{ my: 3, p: 2, bgcolor: 'rgba(125, 85, 37, 0.05)', borderRadius: 2, border: '1px dashed', borderColor: 'primary.light' }}>
                                <Typography variant="h3" fontWeight="bold" color="primary.main">
                                    {formattedPrice}
                                </Typography>
                            </Box>

                            <Stack direction="row" spacing={2} alignItems="center" mb={4}>
                                <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ccc', borderRadius: 1 }}>
                                    <IconButton onClick={() => handleQuantityChange('decrease')} disabled={quantity <= 1 || isAddingToCart}>
                                        <Remove fontSize="small" />
                                    </IconButton>
                                    <Typography sx={{ px: 2, fontWeight: 'bold' }}>{quantity}</Typography>
                                    <IconButton onClick={() => handleQuantityChange('increase')} disabled={isAddingToCart}>
                                        <Add fontSize="small" />
                                    </IconButton>
                                </Box>

                                <Button
                                    variant="contained"
                                    size="large"
                                    fullWidth
                                    startIcon={isAddingToCart ? <CircularProgress size={20} color="inherit" /> : <ShoppingCartIcon />}
                                    onClick={handleAddToCart}
                                    disabled={isAddingToCart}
                                    sx={{ py: 1.5, fontSize: '1.1rem', borderRadius: 2, boxShadow: 2 }}
                                >
                                    {isAddingToCart ? 'Ekleniyor...' : 'Sepete Ekle'}
                                </Button>
                            </Stack>

                            <Grid container spacing={2}>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}><LocalShipping color="action" fontSize="large" /><Typography variant="caption" align="center">Hızlı Kargo</Typography></Stack>
                                </Grid>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}><VerifiedUser color="action" fontSize="large" /><Typography variant="caption" align="center">%100 Orijinal</Typography></Stack>
                                </Grid>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}><Cached color="action" fontSize="large" /><Typography variant="caption" align="center">Kolay İade</Typography></Stack>
                                </Grid>
                            </Grid>
                        </Box>
                    </Grid>
                </Grid>

                <Paper sx={{ mt: 6, borderRadius: 4, overflow: 'hidden' }} elevation={1}>
                    <Tabs value={tabValue} onChange={handleTabChange} variant="fullWidth" indicatorColor="primary" textColor="primary" sx={{ bgcolor: 'grey.50', borderBottom: '1px solid #e0e0e0' }}>
                        <Tab label="Ürün Açıklaması" sx={{ fontWeight: 'bold' }} />
                        <Tab label="Özellikler" sx={{ fontWeight: 'bold' }} />
                    </Tabs>
                    <CustomTabPanel value={tabValue} index={0}>
                        <Container maxWidth="md">
                            <Typography paragraph>{product.description || 'Bu ürün için henüz bir açıklama girilmemiş.'}</Typography>
                        </Container>
                    </CustomTabPanel>
                    <CustomTabPanel value={tabValue} index={1}>
                        <Container maxWidth="md">
                            <Typography color="text.secondary">Detaylı özellikler yakında eklenecektir.</Typography>
                        </Container>
                    </CustomTabPanel>
                </Paper>
            </Container>

            <Snackbar
                open={snackbar.open}
                autoHideDuration={4000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%', borderRadius: 2, boxShadow: 3 }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default ProductDetailPage;