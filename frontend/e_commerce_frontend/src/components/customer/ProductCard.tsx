import React from 'react';
import {
    Typography, Card, CardContent, CardMedia, Box,
    IconButton, Button, Stack, Rating,
} from '@mui/material';
import {
    FavoriteBorder,
    ShoppingCartOutlined,
    Inventory2Outlined,
} from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';
import type { ProductSummary } from '../../types';
import { useToastStore } from '../../store/useToastStore';
import { useCartStore } from '../../store/useCartStore';
import { useAuthStore } from '../../store/useAuthStore';
import { useAddToBasket } from '../../query/useBasketQueries';

interface ProductCardProps {
    product: ProductSummary;
}

const formatPrice = (price: number): string =>
    new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency: 'TRY',
        minimumFractionDigits: 2,
    }).format(price);

const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
    const toast = useToastStore();
    const addItem = useCartStore((state) => state.addItem);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const { mutate: addApiItem, isPending } = useAddToBasket();

    const imageUrl =
        product.mainImageUrl ?? 'https://via.placeholder.com/300x300?text=Resim+Yok';

    // discountedPrice varsa onu, yoksa normal fiyatı göster
    const displayPrice = product.discountedPrice ?? product.price;
    const hasDiscount =
        product.discountedPrice !== null &&
        product.discountedPrice !== undefined &&
        product.discountedPrice < product.price;

    const handleAddToCart = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        if (!product.inStock || isPending) return;

        if (isAuthenticated) {
            addApiItem(
                { productId: Number(product.id), quantity: 1 },
                {
                    onSuccess: () => toast.success(`"${product.name}" sepete eklendi!`),
                    onError: () => toast.error('Ürün sepete eklenirken hata oluştu.'),
                },
            );
        } else {
            addItem({
                productId: Number(product.id),
                name: product.name,
                price: displayPrice,
                quantity: 1,
                mainImageUrl: product.mainImageUrl ?? undefined,
            });
            toast.success(`"${product.name}" sepete eklendi!`);
        }
    };

    return (
        <Card
            elevation={0}
            sx={{
                bgcolor: 'white',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                border: '1px solid #eee',
                borderRadius: 3,
                position: 'relative',
                overflow: 'hidden',
                transition: 'all 0.25s ease',
                '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
                    borderColor: 'transparent',
                    '& .quick-add': { opacity: 1, transform: 'translateY(0)' },
                },
            }}
        >
            {/* Favori */}
            <IconButton
                size="small"
                sx={{
                    position: 'absolute', top: 8, right: 8, zIndex: 2,
                    bgcolor: 'rgba(255,255,255,0.9)',
                    '&:hover': { bgcolor: 'white', color: 'error.main' },
                }}
            >
                <FavoriteBorder fontSize="small" />
            </IconButton>

            {/* Stok rozeti */}
            {!product.inStock && (
                <Box sx={{
                    position: 'absolute', top: 8, left: 8, zIndex: 2,
                    bgcolor: 'grey.700', color: 'white',
                    px: 1, py: 0.25, borderRadius: 1,
                    fontSize: '0.7rem', fontWeight: 'bold',
                }}>
                    STOKTA YOK
                </Box>
            )}

            {/* İndirim rozeti */}
            {hasDiscount && (
                <Box sx={{
                    position: 'absolute', top: product.inStock ? 8 : 32, left: 8, zIndex: 2,
                    bgcolor: 'error.main', color: 'white',
                    px: 1, py: 0.25, borderRadius: 1,
                    fontSize: '0.7rem', fontWeight: 'bold',
                }}>
                    {Math.round((1 - product.discountedPrice! / product.price) * 100)}% İNDİRİM
                </Box>
            )}

            {/* Görsel */}
            <Box sx={{ position: 'relative', paddingTop: '100%', overflow: 'hidden', bgcolor: '#f8f8f8' }}>
                <Box
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    sx={{ position: 'absolute', inset: 0, display: 'block', zIndex: 1 }}
                >
                    <CardMedia
                        component="img"
                        image={imageUrl}
                        alt={product.name}
                        loading="lazy"
                        sx={{
                            width: '100%', height: '100%',
                            objectFit: 'contain', p: 2,
                            opacity: product.inStock ? 1 : 0.5,
                        }}
                    />
                </Box>

                {/* Quick-add (hover) */}
                <Button
                    className="quick-add"
                    variant="contained"
                    size="small"
                    startIcon={
                        isPending
                            ? undefined
                            : product.inStock
                                ? <ShoppingCartOutlined fontSize="small" />
                                : <Inventory2Outlined fontSize="small" />
                    }
                    onClick={handleAddToCart}
                    disabled={!product.inStock || isPending}
                    sx={{
                        position: 'absolute', bottom: 8, left: 8, right: 8, zIndex: 3,
                        opacity: 0, transform: 'translateY(8px)',
                        transition: 'all 0.2s ease',
                        borderRadius: 2, textTransform: 'none',
                        fontWeight: 'bold', fontSize: '0.8rem',
                        display: { xs: 'none', sm: 'flex' },
                    }}
                >
                    {product.inStock ? 'Sepete Ekle' : 'Stokta Yok'}
                </Button>
            </Box>

            {/* İçerik */}
            <CardContent sx={{ flexGrow: 1, p: 1.5, display: 'flex', flexDirection: 'column' }}>
                {product.brand && (
                    <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.5 }}
                    >
                        {product.brand}
                    </Typography>
                )}

                <Typography
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    variant="body2"
                    sx={{
                        fontWeight: 500, mt: 0.25,
                        textDecoration: 'none', color: 'text.primary',
                        display: '-webkit-box', overflow: 'hidden',
                        WebkitBoxOrient: 'vertical', WebkitLineClamp: 2,
                        minHeight: '2.6em', lineHeight: 1.3,
                        '&:hover': { color: 'primary.main' },
                    }}
                >
                    {product.name}
                </Typography>

                {/* Puan & yorum sayısı — her ikisi de artık ProductSummary'de mevcut */}
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 0.5, mb: 1, gap: 0.5 }}>
                    <Rating
                        value={product.ratingAverage ?? 0}
                        precision={0.5}
                        readOnly
                        size="small"
                        sx={{ fontSize: '1rem' }}
                    />
                    <Typography variant="caption" color="text.secondary">
                        ({product.reviewCount ?? 0})
                    </Typography>
                </Box>

                {/* Fiyat */}
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 'auto' }}>
                    <Box>
                        {hasDiscount && (
                            <Typography
                                variant="caption"
                                color="text.disabled"
                                sx={{ textDecoration: 'line-through', display: 'block', lineHeight: 1 }}
                            >
                                {formatPrice(product.price)}
                            </Typography>
                        )}
                        <Typography
                            variant="subtitle1"
                            color={product.inStock ? (hasDiscount ? 'error.main' : 'primary.main') : 'text.disabled'}
                            fontWeight="bold"
                            sx={{ fontSize: '1rem' }}
                        >
                            {formatPrice(displayPrice)}
                        </Typography>
                    </Box>

                    {/* Mobil sepet butonu */}
                    <IconButton
                        color="primary"
                        size="small"
                        onClick={handleAddToCart}
                        disabled={!product.inStock || isPending}
                        sx={{
                            display: { xs: 'flex', sm: 'none' },
                            border: '1px solid', borderColor: 'divider',
                            borderRadius: 1.5,
                            '&:hover': { bgcolor: 'primary.main', color: 'white' },
                        }}
                    >
                        <ShoppingCartOutlined fontSize="small" />
                    </IconButton>
                </Stack>
            </CardContent>
        </Card>
    );
};

export default ProductCard;