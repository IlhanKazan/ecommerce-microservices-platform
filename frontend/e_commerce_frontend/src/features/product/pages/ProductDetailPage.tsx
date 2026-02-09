import React, { useState } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import {
    Box, Typography, CircularProgress, Alert,
    Button, Stack, Rating, Divider, Container, Grid,
    Paper, IconButton, Breadcrumbs, Link, Tabs, Tab, Avatar
} from '@mui/material';
import {
    ShoppingCart as ShoppingCartIcon,
    FavoriteBorder,
    Share,
    LocalShipping,
    VerifiedUser,
    Cached,
    Add,
    Remove
} from '@mui/icons-material';
import { useProductDetail } from '../../../query/useProductQueries';
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

// TODO [08.02.2026 22:49]: Baştan sona elden geçecek. Örnek teşkil etmesi için hardcoded sayfa
const ProductDetailPage: React.FC = () => {
    const { productId } = useParams<{ productId: string }>();
    const id = parseInt(productId || '0', 10);
    const { data: product, isLoading, isError } = useProductDetail(id);

    const [quantity, setQuantity] = useState(1);
    const [tabValue, setTabValue] = useState(0);

    const handleQuantityChange = (type: 'increase' | 'decrease') => {
        if (type === 'decrease' && quantity > 1) setQuantity(prev => prev - 1);
        if (type === 'increase') setQuantity(prev => prev + 1);
    };

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setTabValue(newValue);
    };

    const handleAddToCart = () => {
        alert(`${quantity} adet ${product?.name} sepete eklendi.`);
    };

    if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', py: 20 }}><CircularProgress /></Box>;
    if (isError || !product) return <Alert severity="error" sx={{ mt: 5 }}>Ürün bulunamadı.</Alert>;

    const formattedPrice = new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(product.price);

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 8 }}>

            <Box sx={{ bgcolor: 'white', borderBottom: '1px solid', borderColor: 'divider', py: 2 }}>
                <Container maxWidth="lg">
                    <Breadcrumbs aria-label="breadcrumb" sx={{ fontSize: '0.9rem' }}>
                        <Link component={RouterLink} to="/" underline="hover" color="inherit">Anasayfa</Link>
                        <Link component={RouterLink} to="/productlist" underline="hover" color="inherit">Ürünler</Link>
                        <Link component={RouterLink} to="#" underline="hover" color="inherit">{product.category || 'Genel'}</Link>
                        <Typography color="text.primary">{product.name}</Typography>
                    </Breadcrumbs>
                </Container>
            </Box>

            <Container maxWidth="lg" sx={{ mt: 4 }}>
                <Grid container spacing={6}>

                    <Grid size={{ xs: 12, md: 6 }}>
                        <Paper
                            elevation={0}
                            sx={{
                                border: '1px solid #e0e0e0',
                                borderRadius: 4,
                                overflow: 'hidden',
                                position: 'relative',
                                bgcolor: 'white'
                            }}
                        >
                            <IconButton
                                sx={{ position: 'absolute', top: 15, right: 15, bgcolor: 'white', boxShadow: 1, '&:hover': { color: 'red' } }}
                            >
                                <FavoriteBorder />
                            </IconButton>

                            <Box sx={{ p: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
                                <img
                                    src={photo}
                                    alt={product.name}
                                    style={{ maxWidth: '100%', maxHeight: 400, objectFit: 'contain' }}
                                />
                            </Box>
                        </Paper>

                        <Stack direction="row" spacing={2} mt={2} justifyContent="center">
                            {[1, 2, 3].map((item) => (
                                <Box
                                    key={item}
                                    sx={{
                                        width: 70, height: 70, border: '1px solid #ddd', borderRadius: 2,
                                        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
                                        '&:hover': { borderColor: 'primary.main' }
                                    }}
                                >
                                    <img src={photo} alt="thumb" style={{ width: '80%', height: '80%', objectFit: 'contain' }} />
                                </Box>
                            ))}
                        </Stack>
                    </Grid>

                    <Grid size={{ xs: 12, md: 6 }}>
                        <Box>
                            <Typography variant="h4" component="h1" fontWeight="bold" color="text.primary" gutterBottom>
                                {product.name}
                            </Typography>

                            <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                                <Rating value={product.rating} precision={0.5} readOnly size="small" />
                                <Typography variant="body2" color="primary.main" sx={{ cursor: 'pointer', textDecoration: 'underline' }}>
                                    (128 Değerlendirme)
                                </Typography>
                                <Divider orientation="vertical" flexItem sx={{ height: 15, alignSelf: 'center' }} />
                                <Typography variant="caption" color="text.secondary">Ürün Kodu: #ILHAN-{product.id}</Typography>
                            </Stack>

                            <Box sx={{ my: 3, p: 2, bgcolor: 'rgba(125, 85, 37, 0.05)', borderRadius: 2, border: '1px dashed', borderColor: 'primary.light' }}>
                                <Typography variant="h3" fontWeight="bold" color="primary.main">
                                    {formattedPrice}
                                </Typography>
                                <Typography variant="caption" color="success.main" fontWeight="bold">
                                    %15 İndirimli Fiyat • Kargo Bedava
                                </Typography>
                            </Box>

                            <Stack direction="row" spacing={2} alignItems="center" mb={4}>
                                <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ccc', borderRadius: 1 }}>
                                    <IconButton onClick={() => handleQuantityChange('decrease')} disabled={quantity <= 1}>
                                        <Remove fontSize="small" />
                                    </IconButton>
                                    <Typography sx={{ px: 2, fontWeight: 'bold' }}>{quantity}</Typography>
                                    <IconButton onClick={() => handleQuantityChange('increase')}>
                                        <Add fontSize="small" />
                                    </IconButton>
                                </Box>

                                <Button
                                    variant="contained"
                                    size="large"
                                    fullWidth
                                    startIcon={<ShoppingCartIcon />}
                                    onClick={handleAddToCart}
                                    sx={{
                                        py: 1.5,
                                        fontSize: '1.1rem',
                                        borderRadius: 2,
                                        boxShadow: 2
                                    }}
                                >
                                    Sepete Ekle
                                </Button>

                                <IconButton sx={{ border: '1px solid #ddd', borderRadius: 2, p: 1.5 }}>
                                    <Share />
                                </IconButton>
                            </Stack>

                            <Grid container spacing={2}>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}>
                                        <LocalShipping color="action" fontSize="large" />
                                        <Typography variant="caption" align="center">Hızlı Kargo</Typography>
                                    </Stack>
                                </Grid>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}>
                                        <VerifiedUser color="action" fontSize="large" />
                                        <Typography variant="caption" align="center">%100 Orijinal</Typography>
                                    </Stack>
                                </Grid>
                                <Grid size={4}>
                                    <Stack alignItems="center" spacing={1}>
                                        <Cached color="action" fontSize="large" />
                                        <Typography variant="caption" align="center">Kolay İade</Typography>
                                    </Stack>
                                </Grid>
                            </Grid>

                        </Box>
                    </Grid>
                </Grid>

                <Paper sx={{ mt: 6, borderRadius: 4, overflow: 'hidden' }} elevation={1}>
                    <Tabs
                        value={tabValue}
                        onChange={handleTabChange}
                        variant="fullWidth"
                        indicatorColor="primary"
                        textColor="primary"
                        sx={{ bgcolor: 'grey.50', borderBottom: '1px solid #e0e0e0' }}
                    >
                        <Tab label="Ürün Açıklaması" sx={{ fontWeight: 'bold' }} />
                        <Tab label="Değerlendirmeler (128)" sx={{ fontWeight: 'bold' }} />
                        <Tab label="Taksit Seçenekleri" sx={{ fontWeight: 'bold' }} />
                    </Tabs>

                    <CustomTabPanel value={tabValue} index={0}>
                        <Container maxWidth="md">
                            <Typography paragraph>
                                {product.description}
                            </Typography>
                            <Typography paragraph>
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum.
                                Cras venenatis euismod malesuada. Nulla facilisi. Ut enim ad minim veniam, quis nostrud exercitation ullamco.
                            </Typography>
                            <Typography variant="h6" gutterBottom>Ürün Özellikleri:</Typography>
                            <ul>
                                <li>Yüksek kaliteli malzeme</li>
                                <li>Ergonomik tasarım</li>
                                <li>Uzun ömürlü kullanım</li>
                                <li>2 Yıl garanti</li>
                            </ul>
                        </Container>
                    </CustomTabPanel>

                    <CustomTabPanel value={tabValue} index={1}>
                        <Container maxWidth="md">
                            <Stack spacing={3}>
                                <Box>
                                    <Stack direction="row" spacing={2} alignItems="center" mb={1}>
                                        <Avatar sx={{ width: 30, height: 30, fontSize: '0.9rem' }}>A</Avatar>
                                        <Typography variant="subtitle2">Ahmet Y.</Typography>
                                        <Rating value={5} size="small" readOnly />
                                    </Stack>
                                    <Typography variant="body2" color="text.secondary">
                                        Ürün harika, kargo çok hızlıydı. Kesinlikle tavsiye ederim. Fiyat/Performans ürünü.
                                    </Typography>
                                    <Divider sx={{ mt: 2 }} />
                                </Box>
                                <Box>
                                    <Stack direction="row" spacing={2} alignItems="center" mb={1}>
                                        <Avatar sx={{ width: 30, height: 30, fontSize: '0.9rem', bgcolor: 'secondary.main' }}>M</Avatar>
                                        <Typography variant="subtitle2">Mehmet K.</Typography>
                                        <Rating value={4} size="small" readOnly />
                                    </Stack>
                                    <Typography variant="body2" color="text.secondary">
                                        Kalitesi güzel ama beklediğimden biraz daha küçük geldi. Yine de iş görür.
                                    </Typography>
                                </Box>
                            </Stack>
                        </Container>
                    </CustomTabPanel>

                    <CustomTabPanel value={tabValue} index={2}>
                        <Container maxWidth="md">
                            <Typography>Anlaşmalı banka kartlarına peşin fiyatına 3 taksit imkanı.</Typography>
                        </Container>
                    </CustomTabPanel>
                </Paper>

            </Container>
        </Box>
    );
};

export default ProductDetailPage;