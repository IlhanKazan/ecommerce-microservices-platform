import React from 'react';
import { Typography, Card, CardMedia, Box, IconButton, Button, Rating, Avatar } from '@mui/material';
import { FavoriteBorder, ShoppingCartOutlined } from '@mui/icons-material';
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
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', minimumFractionDigits: 2 }).format(price);

const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
    const toast = useToastStore();
    const addItem = useCartStore((s) => s.addItem);
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const { mutate: addApiItem, isPending } = useAddToBasket();

    const imageUrl = product.mainImageUrl ?? 'https://placehold.co/400x400/f5f5f5/bdbdbd?text=Resim+Yok';
    const displayPrice = product.discountedPrice ?? product.price;
    const hasDiscount =
        product.discountedPrice != null && product.discountedPrice < product.price;
    const discountPct = hasDiscount
        ? Math.round((1 - product.discountedPrice! / product.price) * 100)
        : 0;
    const hasRating = (product.reviewCount ?? 0) > 0;

    const handleAddToCart = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (!product.inStock || isPending) return;

        if (isAuthenticated) {
            addApiItem(
                { productId: Number(product.id), quantity: 1 },
                {
                    onSuccess: () => toast.success(`"${product.name}" sepete eklendi!`),
                    onError: () => toast.error('Sepete eklenirken hata oluştu.'),
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
                width: '100%',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                borderRadius: 2,
                border: '1px solid',
                borderColor: 'divider',
                overflow: 'hidden',
                bgcolor: 'white',
                transition: 'box-shadow 0.25s ease, border-color 0.25s ease, transform 0.25s ease',
                '&:hover': {
                    boxShadow: '0 10px 28px rgba(26,34,56,0.14)',
                    borderColor: 'transparent',
                    transform: 'translateY(-2px)',
                    '& .card-add-btn': { opacity: 1, transform: 'translateY(0)' },
                    '& .card-img': { transform: 'scale(1.05)' },
                },
            }}
        >
            {/* ── Görsel alanı ── */}
            <Box sx={{ position: 'relative', paddingTop: '100%', bgcolor: 'grey.50', overflow: 'hidden' }}>
                {/* Tıklanabilir görsel */}
                <Box
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    sx={{ position: 'absolute', inset: 0, display: 'block' }}
                >
                    <CardMedia
                        component="img"
                        className="card-img"
                        image={imageUrl}
                        alt={product.name}
                        loading="lazy"
                        sx={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'contain',
                            p: 1.5,
                            opacity: product.inStock ? 1 : 0.45,
                            transition: 'transform 0.3s ease',
                        }}
                    />
                </Box>

                {/* Rozetler — sol üst */}
                <Box sx={{ position: 'absolute', top: 8, left: 8, display: 'flex', flexDirection: 'column', gap: 0.5, zIndex: 2 }}>
                    {!product.inStock && (
                        <Box sx={{
                            px: 1, py: 0.25, borderRadius: 0.75,
                            bgcolor: 'rgba(0,0,0,0.65)', color: 'white',
                            fontSize: '0.65rem', fontWeight: 700, letterSpacing: 0.3,
                        }}>
                            TÜKENDI
                        </Box>
                    )}
                    {hasDiscount && (
                        <Box sx={{
                            px: 1, py: 0.25, borderRadius: 0.75,
                            bgcolor: 'error.main', color: 'white',
                            fontSize: '0.65rem', fontWeight: 700,
                        }}>
                            %{discountPct} İNDİRİM
                        </Box>
                    )}
                </Box>

                {/* Favori — sağ üst */}
                <IconButton
                    size="small"
                    sx={{
                        position: 'absolute', top: 6, right: 6, zIndex: 2,
                        bgcolor: 'rgba(255,255,255,0.85)',
                        width: 28, height: 28,
                        '&:hover': { bgcolor: 'white', color: 'error.main' },
                    }}
                >
                    <FavoriteBorder sx={{ fontSize: 15 }} />
                </IconButton>

                {/* Hover sepet butonu */}
                <Button
                    className="card-add-btn"
                    variant="contained"
                    size="small"
                    disableElevation
                    startIcon={<ShoppingCartOutlined sx={{ fontSize: '0.9rem !important' }} />}
                    onClick={handleAddToCart}
                    disabled={!product.inStock || isPending}
                    sx={{
                        position: 'absolute', bottom: 8, left: 8, right: 8, zIndex: 3,
                        opacity: 0, transform: 'translateY(6px)',
                        transition: 'opacity 0.2s ease, transform 0.2s ease',
                        borderRadius: 1.5, textTransform: 'none',
                        fontWeight: 600, fontSize: '0.78rem', py: 0.6,
                        display: { xs: 'none', sm: 'flex' },
                    }}
                >
                    {product.inStock ? 'Sepete Ekle' : 'Tükendi'}
                </Button>
            </Box>

            {/* ── İçerik alanı ── */}
            <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', px: 1.5, pt: 1.25, pb: 1.75 }}>

                {/* Marka */}
                <Box sx={{ minHeight: '1.3em', mb: 0.25 }}>
                    {product.brand && (
                        <Typography
                            variant="caption"
                            sx={{ color: 'text.disabled', fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.6, fontSize: '0.65rem' }}
                        >
                            {product.brand}
                        </Typography>
                    )}
                </Box>

                {/* Ürün adı */}
                <Typography
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    sx={{
                        fontWeight: 500, fontSize: '0.82rem', lineHeight: 1.35,
                        color: 'text.primary', textDecoration: 'none',
                        display: '-webkit-box', overflow: 'hidden',
                        WebkitBoxOrient: 'vertical', WebkitLineClamp: 2,
                        minHeight: '2.25em',
                        '&:hover': { color: 'primary.main' },
                    }}
                >
                    {product.name}
                </Typography>

                {/* Yıldız — sadece review varsa göster */}
                <Box sx={{ minHeight: '1.5em', mt: 0.5, display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    {hasRating && (
                        <>
                            <Rating
                                value={product.ratingAverage ?? 0}
                                precision={0.5}
                                readOnly
                                size="small"
                                sx={{ fontSize: '0.85rem' }}
                            />
                            <Typography variant="caption" sx={{ color: 'text.disabled', fontSize: '0.7rem' }}>
                                ({product.reviewCount})
                            </Typography>
                        </>
                    )}
                </Box>

                {/* Satıcı */}
                <Box sx={{ minHeight: '1.6em', mt: 0.5 }}>
                    {product.tenantName && (
                        <RouterLink
                            to={`/store/${product.tenantId}`}
                            style={{ textDecoration: 'none' }}
                            onClick={(e) => e.stopPropagation()}
                        >
                            <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, '&:hover': { opacity: 0.7 } }}>
                                <Avatar
                                    src={product.tenantLogoUrl ?? undefined}
                                    alt={product.tenantName}
                                    sx={{ width: 14, height: 14, fontSize: '0.5rem', bgcolor: 'primary.light' }}
                                >
                                    {product.tenantName.charAt(0).toUpperCase()}
                                </Avatar>
                                <Typography noWrap sx={{ color: 'text.secondary', fontSize: '0.68rem', maxWidth: 110 }}>
                                    {product.tenantName}
                                </Typography>
                            </Box>
                        </RouterLink>
                    )}
                </Box>

                {/* Fiyat + mobil sepet */}
                <Box sx={{ mt: 'auto', pt: 1.25, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
                    <Box>
                        {hasDiscount && (
                            <Typography sx={{ fontSize: '0.72rem', color: 'text.disabled', textDecoration: 'line-through', lineHeight: 1.2 }}>
                                {formatPrice(product.price)}
                            </Typography>
                        )}
                        <Typography
                            sx={{
                                fontWeight: 800,
                                fontSize: '1.1rem',
                                lineHeight: 1.2,
                                fontVariantNumeric: 'tabular-nums',
                                color: !product.inStock ? 'text.disabled' : hasDiscount ? 'error.main' : 'text.primary',
                            }}
                        >
                            {formatPrice(displayPrice)}
                        </Typography>
                    </Box>

                    {/* Mobil sepet */}
                    <IconButton
                        size="small"
                        onClick={handleAddToCart}
                        disabled={!product.inStock || isPending}
                        sx={{
                            display: { xs: 'flex', sm: 'none' },
                            width: 32, height: 32,
                            border: '1px solid', borderColor: 'divider',
                            borderRadius: 1,
                            color: 'primary.main',
                            '&:hover': { bgcolor: 'primary.main', color: 'white', borderColor: 'primary.main' },
                        }}
                    >
                        <ShoppingCartOutlined sx={{ fontSize: 16 }} />
                    </IconButton>
                </Box>
            </Box>
        </Card>
    );
};

export default React.memo(ProductCard);
