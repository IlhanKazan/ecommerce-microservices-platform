import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import { userService } from '../../user/api/userService';
import {
    Box, Typography, CircularProgress, Alert,
    Button, Stack, Rating, Divider, Container, Grid,
    Paper, IconButton, Breadcrumbs, Link, Tabs, Tab,
    Snackbar, Table, TableBody, TableCell, TableContainer, TableRow,
    Avatar, Chip, TextField, Dialog, DialogTitle, DialogContent,
    DialogActions, Tooltip
} from '@mui/material';
import {
    ShoppingCart as ShoppingCartIcon,
    Favorite,
    FavoriteBorder,
    LocalShipping,
    VerifiedUser,
    Cached,
    Add,
    Remove,
    ThumbUpOutlined,
    ThumbDownOutlined,
    CheckCircle,
    RateReview,
    Delete as DeleteIcon,
    Close as CloseIcon,
    ChevronLeft,
    ChevronRight,
} from '@mui/icons-material';

import { useAuth } from 'react-oidc-context';
import { useAddToBasket } from '../../../query/useBasketQueries';
import { useCartStore } from '../../../store/useCartStore';
import photo from '../../../components/customer/react.svg';
import { useAuthStore } from '../../../store/useAuthStore';
import { useFavoriteStore } from '../../../store/useFavoriteStore';
import {
    useGetProductDetail,
    useProductAvailability,
    useVariantStock,
    useGetProductReviews,
    useCreateReview,
    useMarkReviewHelpful,
    useDeleteReview,
    useSimilarProducts,
    useGetProductsByIds,
} from '../../../query/useProductQueries';
import { useFrequentlyBoughtWith } from '../../../query/useOrderQueries';
import ProductRail from '../../../components/customer/ProductRail';
import type { ReviewCreateRequest, VariantSummary, VariantStock } from '../../../types/product';
import { MultiImageUpload } from '../../../components/shared/ImageUploadField';
import type { ImagePreview } from '../../../utils/imageUploadUtils';
import { productService } from '../api/productService';
import AiReviewSummaryCard from '../../ai/components/AiReviewSummaryCard';

interface TabPanelProps {
    children?: React.ReactNode;
    index: number;
    value: number;
}

function CustomTabPanel({ children, value, index, ...other }: TabPanelProps) {
    return (
        <div role="tabpanel" hidden={value !== index} {...other}>
            {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
        </div>
    );
}

// ─── Review Form ─────────────────────────────────────────────────────────────

interface ReviewFormState {
    rating: number;
    title: string;
    reviewText: string;
    imageUrls: ImagePreview[];
}

const EMPTY_FORM: ReviewFormState = { rating: 5, title: '', reviewText: '', imageUrls: [] };

// ─── Component ───────────────────────────────────────────────────────────────

// Varyant eksenlerini (ör. "Renk" → [Siyah, Beyaz], "Numara" → [42, 43]) child'ların attributes'undan türet.
function buildVariantAxes(variants?: VariantSummary[]): Record<string, string[]> {
    const axes: Record<string, string[]> = {};
    (variants ?? []).forEach((v) => {
        Object.entries(v.attributes ?? {}).forEach(([key, val]) => {
            if (!axes[key]) axes[key] = [];
            if (!axes[key].includes(val)) axes[key].push(val);
        });
    });
    return axes;
}

// Tüm eksenler seçilmişse eşleşen varyantı döner; aksi halde undefined.
function resolveVariant(
    variants: VariantSummary[] | undefined,
    selected: Record<string, string>,
): VariantSummary | undefined {
    if (!variants || variants.length === 0) return undefined;
    const keys = Object.keys(buildVariantAxes(variants));
    if (keys.length === 0 || !keys.every((k) => selected[k])) return undefined;
    return variants.find((v) => keys.every((k) => v.attributes?.[k] === selected[k]));
}

const ProductDetailPage: React.FC = () => {
    const { productId } = useParams<{ productId: string }>();
    const id = parseInt(productId || '0', 10);

    const { data: product, isLoading, isError } = useGetProductDetail(id);
    const { data: availability } = useProductAvailability(id);
    const { data: similarProducts, isLoading: similarLoading } = useSimilarProducts(id);

    // Varyant-bazlı canlı stok — stoksuz varyantı çarpılı göstermek + "Son X adet" için.
    // product yokken boş liste → query disabled; varyantlar gelince çalışır.
    const variantIds = (product?.variants ?? []).map((v) => v.id);
    const { data: variantStockList } = useVariantStock(variantIds);

    const auth = useAuth();
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const currentUser = useAuthStore((state) => state.oidcProfile);
    // Birlikte sıkça alınanlar (public) — co-purchase id'leri → ürün kartları
    const { data: fbtIds } = useFrequentlyBoughtWith(id, true);
    const { data: fbtProducts, isLoading: fbtLoading } = useGetProductsByIds(fbtIds ?? []);
    const { mutate: addToCartApi, isPending: isAddingToCart } = useAddToBasket();
    const localAddItem = useCartStore((state) => state.addItem);
    const isFavorite = useFavoriteStore((s) => s.ids.has(id));
    const toggleFavorite = useFavoriteStore((s) => s.toggle);

    const handleFavorite = () => {
        if (!isAuthenticated) {
            auth.signinRedirect();
            return;
        }
        toggleFavorite(id);
    };

    const [quantity, setQuantity] = useState(1);
    const [selectedAttrs, setSelectedAttrs] = useState<Record<string, string>>({});
    const [tabValue, setTabValue] = useState(0);
    const [activeImage, setActiveImage] = useState<string | null>(null);
    const [lightboxOpen, setLightboxOpen] = useState(false);

    // Yorumlar — sayfalama
    const [reviewPage] = useState(0);
    const { data: reviewsPage, isLoading: isReviewsLoading } = useGetProductReviews(
        id,
        reviewPage,
        10
    );
    const reviews = reviewsPage?.content ?? [];

    // Yorum yazma dialog
    const [reviewDialogOpen, setReviewDialogOpen] = useState(false);
    const [reviewForm, setReviewForm] = useState<ReviewFormState>(EMPTY_FORM);

    const { mutate: createReview, isPending: isSubmittingReview } = useCreateReview(id);
    const { mutate: markHelpful } = useMarkReviewHelpful(id);
    const { mutate: deleteReview } = useDeleteReview(id);
    const [votedReviews, setVotedReviews] = useState<Set<number>>(new Set());

    const [snackbar, setSnackbar] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({ open: false, message: '', severity: 'success' });

    useEffect(() => {
        if (product?.mainImageUrl) setActiveImage(product.mainImageUrl);
    }, [product]);

    // Tam varyant seçilince ve varyantın kendi görseli varsa galeriyi ona getir.
    useEffect(() => {
        const v = resolveVariant(product?.variants, selectedAttrs);
        if (v?.mainImageUrl) setActiveImage(v.mainImageUrl);
    }, [product, selectedAttrs]);

    // Varyant seçimi değişince miktarı sıfırla (yeni varyantın stok tavanını aşmayı önler).
    useEffect(() => {
        setQuantity(1);
    }, [selectedAttrs]);

    // Giriş yapmış kullanıcının gezinme geçmişine kaydet (best-effort, ürün başına bir kez).
    const recordedViewRef = useRef<number | null>(null);
    useEffect(() => {
        if (isAuthenticated && product?.id && recordedViewRef.current !== product.id) {
            recordedViewRef.current = product.id;
            userService.recordProductView(product.id, product.tenantId);
        }
    }, [isAuthenticated, product?.id, product?.tenantId]);

    const handleQuantityChange = (type: 'increase' | 'decrease') => {
        if (type === 'decrease' && quantity > 1) setQuantity((prev) => prev - 1);
        if (type === 'increase') setQuantity((prev) => prev + 1);
    };

    const handleAddToCart = () => {
        if (!product) return;

        // Varyantlı üründe seçilen varyant (child) sepete eklenir; varyantsızda ürünün kendisi.
        const hasVariants = (product.variants?.length ?? 0) > 0;
        const variant = resolveVariant(product.variants, selectedAttrs);
        if (hasVariants && !variant) return; // buton zaten disabled

        const buyId = variant ? variant.id : product.id;
        const buyName = variant ? variant.name : product.name;
        const buyPrice = variant
            ? (variant.discountedPrice ?? variant.price)
            : (product.discountedPrice ?? product.price);
        const buyImage = (variant ? (variant.mainImageUrl ?? product.mainImageUrl) : product.mainImageUrl) ?? undefined;

        if (isAuthenticated) {
            addToCartApi(
                { productId: buyId, quantity },
                {
                    onSuccess: () => {
                        setSnackbar({ open: true, message: `${quantity} adet ürün sepete eklendi!`, severity: 'success' });
                        setQuantity(1);
                    },
                    onError: (error: unknown) =>
                        setSnackbar({
                            open: true,
                            message: (error as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Hata oluştu.',
                            severity: 'error',
                        }),
                }
            );
        } else {
            localAddItem({
                productId: buyId,
                name: buyName,
                price: buyPrice,
                quantity,
                mainImageUrl: buyImage,
            });
            setSnackbar({ open: true, message: `${quantity} adet ürün sepete eklendi!`, severity: 'success' });
            setQuantity(1);
        }
    };

    const handleReviewSubmit = () => {
        if (!reviewForm.title.trim() || !reviewForm.reviewText.trim()) return;

        const body: ReviewCreateRequest = {
            title: reviewForm.title,
            reviewText: reviewForm.reviewText,
            rating: reviewForm.rating,
            imageUrls: reviewForm.imageUrls.map((img) => img.url),
        };

        createReview(body, {
            onSuccess: () => {
                setReviewDialogOpen(false);
                setReviewForm(EMPTY_FORM);
                setSnackbar({ open: true, message: 'Yorumunuz başarıyla eklendi.', severity: 'success' });
            },
            onError: (error: unknown) => {
                const msg =
                    (error as { response?: { data?: { message?: string } } })
                        ?.response?.data?.message
                    ?? 'Yorum eklenirken hata oluştu.';
                setSnackbar({ open: true, message: msg, severity: 'error' });
            },
        });
    };

    // ─── Guards ───────────────────────────────────────────────────────────────

    if (isLoading)
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 20 }}>
                <CircularProgress />
            </Box>
        );

    if (isError || !product)
        return (
            <Container>
                <Alert severity="error" sx={{ mt: 5 }}>
                    Ürün bulunamadı veya yayından kaldırılmış olabilir.
                </Alert>
            </Container>
        );

    const formatPrice = (p: number) =>
        new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(p);

    // ─── Varyant durumu ─────────────────────────────────────────────────
    const variantAxes = buildVariantAxes(product.variants);
    const hasVariants = (product.variants?.length ?? 0) > 0;
    const selectedVariant = resolveVariant(product.variants, selectedAttrs);
    const needsVariantSelection = hasVariants && !selectedVariant;

    // Varyant-bazlı canlı stok haritası (productId → durum). Veri gelmeden hiçbir şey "tükendi" sayılmaz (degrade).
    const variantStockMap = new Map<number, VariantStock>();
    (variantStockList ?? []).forEach((s) => variantStockMap.set(s.productId, s));

    // Bir eksen seçeneği (ör. Numara: 42) — diğer seçili eksenlerle birlikte stoklu bir varyant var mı?
    // (ör. siyah seçiliyken 42 numara stokta yoksa çarpılı gösterilir.)
    const isOptionInStock = (axis: string, opt: string): boolean => {
        const candidates = (product.variants ?? []).filter((v) => {
            if (v.attributes?.[axis] !== opt) return false;
            return Object.entries(selectedAttrs).every(([k, val]) => k === axis || v.attributes?.[k] === val);
        });
        if (candidates.length === 0) return true; // kombinasyon yok / veri yok → engelleme
        return candidates.some((v) => {
            const st = variantStockMap.get(v.id);
            return !st || st.inStock; // stok bilgisi yoksa alınabilir varsay
        });
    };

    const selectedVariantStock = selectedVariant ? variantStockMap.get(selectedVariant.id) : undefined;
    const selectedVariantOutOfStock = !!selectedVariant && !!selectedVariantStock && !selectedVariantStock.inStock;
    // Seçili varyantın gösterilebilir adedi (yalnız düşük stokta dolu) — "Son X adet" + miktar tavanı
    const selectedVariantLowQty =
        selectedVariant && selectedVariantStock?.inStock ? selectedVariantStock.availableQuantity : null;

    // Seçilen varyant varsa fiyat/görsel ondan; yoksa parent'tan (varyantlıda "fiyat varyanta göre").
    const displayPrice = selectedVariant ? selectedVariant.price : product.price;
    const displayDiscounted = selectedVariant ? selectedVariant.discountedPrice : product.discountedPrice;

    // Detay endpoint'inde status her zaman ACTIVE'dir (getPublicProductInfo aksini fırlatır).
    // Fiziksel stok kartla aynı ES kaynağından (availability) gelir; salesStatus merchant'ın elle kararı.
    // Varyantlı üründe stok varyant bazındadır → seçilen varyantın canlı stoğuna bakılır (parent inStock değil).
    const physicalOutOfStock = availability?.inStock === false;
    const isOutOfStock = hasVariants
        ? selectedVariantOutOfStock
        : (product.salesStatus === 'OUT_OF_STOCK' || physicalOutOfStock);
    const isAvailableToBuy =
        product.status === 'ACTIVE' && product.salesStatus === 'ON_SALE'
        && !isOutOfStock && !needsVariantSelection;
    // Miktar tavanı: seçili varyantın düşük stoğu biliniyorsa + butonunu kilitle
    const maxQty = selectedVariantLowQty ?? null;
    const allImages = [product.mainImageUrl, ...(product.imageUrls ?? [])].filter(Boolean) as string[];
    const hasImages = allImages.length > 0;
    const activeIndex = Math.max(0, allImages.indexOf(activeImage ?? ''));
    const showImageAt = (idx: number) => {
        if (allImages.length === 0) return;
        const len = allImages.length;
        setActiveImage(allImages[((idx % len) + len) % len]);
    };

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 8 }}>

            {/* ─── Breadcrumb ─────────────────────────────────────────────── */}
            <Box sx={{ bgcolor: 'white', borderBottom: '1px solid', borderColor: 'divider', py: 2 }}>
                <Container maxWidth="lg">
                    <Breadcrumbs aria-label="breadcrumb" sx={{ fontSize: '0.9rem' }}>
                        <Link component={RouterLink} to="/" underline="hover" color="inherit">
                            Anasayfa
                        </Link>
                        <Link component={RouterLink} to="/productlist" underline="hover" color="inherit">
                            Ürünler
                        </Link>
                        {/* categoryName artık backend'den geliyor */}
                        {product.categoryName && (
                            <Link
                                component={RouterLink}
                                to={`/productlist?categoryId=${product.categoryId}`}
                                underline="hover"
                                color="inherit"
                            >
                                {product.categoryName}
                            </Link>
                        )}
                        <Typography color="text.primary">{product.name}</Typography>
                    </Breadcrumbs>
                </Container>
            </Box>

            <Container maxWidth="lg" sx={{ mt: 4 }}>
                <Grid container spacing={6}>

                    {/* ─── Sol — Galeri ──────────────────────────────────── */}
                    <Grid size={{ xs: 12, md: 6 }}>
                        <Paper
                            elevation={0}
                            sx={{
                                border: '1px solid #e0e0e0', borderRadius: 4, overflow: 'hidden',
                                position: 'relative', bgcolor: 'white', mb: 2,
                            }}
                        >
                            <IconButton
                                onClick={handleFavorite}
                                aria-label={isFavorite ? 'Favorilerden çıkar' : 'Favorilere ekle'}
                                sx={{ position: 'absolute', top: 15, right: 15, bgcolor: 'white', boxShadow: 1, color: isFavorite ? 'error.main' : 'inherit', '&:hover': { color: 'error.main' } }}
                            >
                                {isFavorite ? <Favorite /> : <FavoriteBorder />}
                            </IconButton>
                            <Box
                                onClick={() => hasImages && setLightboxOpen(true)}
                                sx={{
                                    p: 4, display: 'flex', justifyContent: 'center', alignItems: 'center', height: 450,
                                    cursor: hasImages ? 'zoom-in' : 'default',
                                }}
                            >
                                <img
                                    src={activeImage || photo}
                                    alt={product.name}
                                    style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                                />
                            </Box>
                        </Paper>

                        {allImages.length > 1 && (
                            <Stack direction="row" spacing={2} sx={{ overflowX: 'auto', py: 1 }}>
                                {allImages.map((imgUrl, idx) => (
                                    <Paper
                                        key={idx}
                                        elevation={0}
                                        onClick={() => setActiveImage(imgUrl)}
                                        sx={{
                                            width: 80, height: 80, flexShrink: 0, cursor: 'pointer',
                                            border: '2px solid',
                                            borderColor: activeImage === imgUrl ? 'primary.main' : '#e0e0e0',
                                            borderRadius: 2, overflow: 'hidden',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}
                                    >
                                        <img src={imgUrl} alt="thumbnail" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'cover' }} />
                                    </Paper>
                                ))}
                            </Stack>
                        )}

                        {/* ─── Lightbox ──────────────────────────────────── */}
                        <Dialog
                            open={lightboxOpen}
                            onClose={() => setLightboxOpen(false)}
                            maxWidth="lg"
                            fullWidth
                            slotProps={{ paper: { sx: { bgcolor: 'rgba(20,20,20,0.97)', backgroundImage: 'none' } } }}
                        >
                            <DialogContent sx={{ p: 0, position: 'relative' }}>
                                <IconButton
                                    onClick={() => setLightboxOpen(false)}
                                    sx={{ position: 'absolute', top: 8, right: 8, zIndex: 2, color: 'white', bgcolor: 'rgba(0,0,0,.4)', '&:hover': { bgcolor: 'rgba(0,0,0,.6)' } }}
                                >
                                    <CloseIcon />
                                </IconButton>

                                {allImages.length > 1 && (
                                    <IconButton
                                        onClick={() => showImageAt(activeIndex - 1)}
                                        sx={{ position: 'absolute', top: '50%', left: 8, transform: 'translateY(-50%)', zIndex: 2, color: 'white', bgcolor: 'rgba(0,0,0,.4)', '&:hover': { bgcolor: 'rgba(0,0,0,.6)' } }}
                                    >
                                        <ChevronLeft fontSize="large" />
                                    </IconButton>
                                )}

                                <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '75vh', p: 2 }}>
                                    <img
                                        src={activeImage || photo}
                                        alt={product.name}
                                        style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }}
                                    />
                                </Box>

                                {allImages.length > 1 && (
                                    <IconButton
                                        onClick={() => showImageAt(activeIndex + 1)}
                                        sx={{ position: 'absolute', top: '50%', right: 8, transform: 'translateY(-50%)', zIndex: 2, color: 'white', bgcolor: 'rgba(0,0,0,.4)', '&:hover': { bgcolor: 'rgba(0,0,0,.6)' } }}
                                    >
                                        <ChevronRight fontSize="large" />
                                    </IconButton>
                                )}

                                {allImages.length > 1 && (
                                    <Stack direction="row" spacing={1.5} justifyContent="center" sx={{ overflowX: 'auto', px: 2, pb: 2 }}>
                                        {allImages.map((imgUrl, idx) => (
                                            <Box
                                                key={idx}
                                                component="img"
                                                src={imgUrl}
                                                alt="thumbnail"
                                                onClick={() => setActiveImage(imgUrl)}
                                                sx={{
                                                    width: 64, height: 64, flexShrink: 0, cursor: 'pointer',
                                                    objectFit: 'cover', borderRadius: 1,
                                                    border: '2px solid',
                                                    borderColor: activeImage === imgUrl ? 'primary.main' : 'transparent',
                                                    opacity: activeImage === imgUrl ? 1 : 0.6,
                                                    transition: 'opacity .2s',
                                                }}
                                            />
                                        ))}
                                    </Stack>
                                )}
                            </DialogContent>
                        </Dialog>
                    </Grid>

                    {/* ─── Sağ — Bilgiler ────────────────────────────────── */}
                    <Grid size={{ xs: 12, md: 6 }}>
                        <Box>
                            {product.brand && (
                                <Typography
                                    variant="subtitle2"
                                    color="text.secondary"
                                    sx={{ textTransform: 'uppercase', letterSpacing: 1, mb: 1 }}
                                >
                                    {product.brand}
                                </Typography>
                            )}
                            <Typography variant="h4" component="h1" fontWeight="bold" color="text.primary" gutterBottom>
                                {product.name}
                            </Typography>

                            <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                                <Rating value={product.ratingAverage ?? 0} precision={0.5} readOnly size="small" />
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                    sx={{ cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
                                    onClick={() => setTabValue(2)}
                                >
                                    ({product.reviewCount ?? 0} Değerlendirme)
                                </Typography>
                                <Divider orientation="vertical" flexItem sx={{ height: 15, alignSelf: 'center' }} />
                                <Typography variant="caption" color="text.secondary">SKU: {selectedVariant?.sku ?? product.sku}</Typography>
                            </Stack>

                            {/* Satıcı */}
                            {product.tenantName && (
                                <RouterLink
                                    to={`/store/${product.tenantId}`}
                                    style={{ textDecoration: 'none', color: 'inherit' }}
                                >
                                    <Box
                                        sx={{
                                            display: 'inline-flex', alignItems: 'center', gap: 1,
                                            px: 1.5, py: 0.75, mb: 2,
                                            border: '1px solid', borderColor: 'divider',
                                            borderRadius: 2, bgcolor: 'grey.50',
                                            cursor: 'pointer',
                                            '&:hover': { bgcolor: 'grey.100', borderColor: 'primary.light' },
                                        }}
                                    >
                                        <Typography variant="caption" color="text.secondary">Satıcı:</Typography>
                                        <Avatar
                                            src={product.tenantLogoUrl ?? undefined}
                                            alt={product.tenantName}
                                            sx={{ width: 22, height: 22, fontSize: '0.65rem', bgcolor: 'primary.light' }}
                                        >
                                            {product.tenantName.charAt(0).toUpperCase()}
                                        </Avatar>
                                        <Typography variant="body2" fontWeight={500}>
                                            {product.tenantName}
                                        </Typography>
                                    </Box>
                                </RouterLink>
                            )}

                            <Box sx={{ my: 3, p: 2, bgcolor: 'primary.lighter', borderRadius: 2, border: '1px dashed', borderColor: 'primary.light' }}>
                                {displayDiscounted && displayDiscounted < displayPrice ? (
                                    <Stack spacing={0.5}>
                                        <Stack direction="row" spacing={1} alignItems="center">
                                            <Typography variant="h6" color="text.secondary" sx={{ textDecoration: 'line-through' }}>
                                                {formatPrice(displayPrice)}
                                            </Typography>
                                            <Chip
                                                size="small"
                                                color="error"
                                                label={`%${Math.round((1 - displayDiscounted / displayPrice) * 100)} İNDİRİM`}
                                                sx={{ fontWeight: 700 }}
                                            />
                                        </Stack>
                                        <Typography variant="h3" fontWeight="bold" color="error.main">
                                            {formatPrice(displayDiscounted)}
                                        </Typography>
                                    </Stack>
                                ) : (
                                    <Typography variant="h3" fontWeight="bold" color="primary.main">
                                        {formatPrice(displayPrice)}
                                    </Typography>
                                )}
                                {needsVariantSelection && (
                                    <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                                        Fiyat seçilen varyanta göre değişir
                                    </Typography>
                                )}
                            </Box>

                            {/* ─── Varyant seçici (Renk / Numara vb.) ─────────────── */}
                            {hasVariants && (
                                <Stack spacing={2} sx={{ mb: 3 }}>
                                    {Object.entries(variantAxes).map(([axis, options]) => (
                                        <Box key={axis}>
                                            <Typography variant="overline" color="text.secondary" fontWeight="bold">
                                                {axis}{selectedAttrs[axis] ? `: ${selectedAttrs[axis]}` : ''}
                                            </Typography>
                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 0.5 }}>
                                                {options.map((opt) => {
                                                    const active = selectedAttrs[axis] === opt;
                                                    const optInStock = isOptionInStock(axis, opt);
                                                    return (
                                                        <Tooltip key={opt} title={optInStock ? '' : 'Tükendi'} disableHoverListener={optInStock}>
                                                            <span>
                                                                <Chip
                                                                    label={opt}
                                                                    clickable={optInStock}
                                                                    disabled={!optInStock}
                                                                    color={active ? 'primary' : 'default'}
                                                                    variant={active ? 'filled' : 'outlined'}
                                                                    onClick={() =>
                                                                        optInStock &&
                                                                        setSelectedAttrs((prev) => ({ ...prev, [axis]: opt }))
                                                                    }
                                                                    sx={{
                                                                        fontWeight: 600,
                                                                        ...(optInStock
                                                                            ? {}
                                                                            : { textDecoration: 'line-through', opacity: 0.55 }),
                                                                    }}
                                                                />
                                                            </span>
                                                        </Tooltip>
                                                    );
                                                })}
                                            </Stack>
                                        </Box>
                                    ))}
                                    {needsVariantSelection && (
                                        <Typography variant="body2" color="warning.main" fontWeight={600}>
                                            Lütfen tüm seçenekleri belirleyin.
                                        </Typography>
                                    )}
                                </Stack>
                            )}

                            {/* Düşük stok uyarısı — seçilen varyant az kaldıysa */}
                            {hasVariants && selectedVariantLowQty != null && (
                                <Typography variant="body2" color="warning.main" fontWeight={700} sx={{ mb: 2 }}>
                                    ⚠ Son {selectedVariantLowQty} adet kaldı!
                                </Typography>
                            )}

                            {/* Stok / satış durumu rozeti */}
                            {isOutOfStock ? (
                                <Chip
                                    label="Tükendi"
                                    color="default"
                                    sx={{ mb: 2, fontWeight: 700, bgcolor: 'grey.300', color: 'text.secondary' }}
                                />
                            ) : (!isAvailableToBuy && !needsVariantSelection) && (
                                // Sadece gerçekten satışa kapalıysa göster — yalnızca varyant seçilmediği için değil.
                                <Chip
                                    label="Şu An Satışta Değil"
                                    color="default"
                                    sx={{ mb: 2, fontWeight: 700, bgcolor: 'grey.300', color: 'text.secondary' }}
                                />
                            )}

                            <Stack direction="row" spacing={2} alignItems="center" mb={4}>
                                <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid #ccc', borderRadius: 1 }}>
                                    <IconButton
                                        onClick={() => handleQuantityChange('decrease')}
                                        disabled={quantity <= 1 || isAddingToCart || !isAvailableToBuy}
                                    >
                                        <Remove fontSize="small" />
                                    </IconButton>
                                    <Typography sx={{ px: 2, fontWeight: 'bold' }}>{quantity}</Typography>
                                    <IconButton
                                        onClick={() => handleQuantityChange('increase')}
                                        disabled={isAddingToCart || !isAvailableToBuy || (maxQty != null && quantity >= maxQty)}
                                    >
                                        <Add fontSize="small" />
                                    </IconButton>
                                </Box>

                                <Button
                                    variant="contained" size="large" fullWidth
                                    startIcon={isAddingToCart ? <CircularProgress size={20} color="inherit" /> : <ShoppingCartIcon />}
                                    onClick={handleAddToCart}
                                    disabled={isAddingToCart || !isAvailableToBuy}
                                    sx={{ py: 1.5, fontSize: '1.1rem', borderRadius: 2, boxShadow: 2 }}
                                >
                                    {isOutOfStock ? 'Tükendi' : needsVariantSelection ? 'Varyant Seçin' : !isAvailableToBuy ? 'Şu An Satışta Değil' : isAddingToCart ? 'Ekleniyor...' : 'Sepete Ekle'}
                                </Button>
                            </Stack>

                            <Grid container spacing={2}>
                                <Grid size={4}><Stack alignItems="center" spacing={1}><LocalShipping color="action" fontSize="large" /><Typography variant="caption" align="center">Hızlı Kargo</Typography></Stack></Grid>
                                <Grid size={4}><Stack alignItems="center" spacing={1}><VerifiedUser color="action" fontSize="large" /><Typography variant="caption" align="center">%100 Orijinal</Typography></Stack></Grid>
                                <Grid size={4}><Stack alignItems="center" spacing={1}><Cached color="action" fontSize="large" /><Typography variant="caption" align="center">Kolay İade</Typography></Stack></Grid>
                            </Grid>
                        </Box>
                    </Grid>
                </Grid>

                {/* ─── Alt Sekmeler ─────────────────────────────────────── */}
                <Paper sx={{ mt: 6, borderRadius: 4, overflow: 'hidden' }} elevation={1}>
                    <Tabs
                        value={tabValue}
                        onChange={(_e, val) => setTabValue(val)}
                        variant="fullWidth"
                        sx={{ bgcolor: 'grey.50', borderBottom: '1px solid #e0e0e0' }}
                    >
                        <Tab label="Ürün Açıklaması" sx={{ fontWeight: 'bold' }} />
                        <Tab label="Özellikler" sx={{ fontWeight: 'bold' }} />
                        <Tab label={`Değerlendirmeler (${product.reviewCount ?? 0})`} sx={{ fontWeight: 'bold' }} />
                    </Tabs>

                    {/* Açıklama */}
                    <CustomTabPanel value={tabValue} index={0}>
                        <Container maxWidth="md">
                            <Typography paragraph sx={{ whiteSpace: 'pre-line' }}>
                                {product.description || 'Bu ürün için açıklama bulunmuyor.'}
                            </Typography>
                        </Container>
                    </CustomTabPanel>

                    {/* Özellikler */}
                    <CustomTabPanel value={tabValue} index={1}>
                        <Container maxWidth="md">
                            {product.attributes && Object.keys(product.attributes).length > 0 ? (
                                <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid #eee' }}>
                                    <Table>
                                        <TableBody>
                                            {Object.entries(product.attributes).map(([key, value]) => (
                                                <TableRow key={key} sx={{ '&:nth-of-type(odd)': { bgcolor: 'grey.50' } }}>
                                                    <TableCell component="th" scope="row" sx={{ fontWeight: 'bold', width: '40%', borderRight: '1px solid #eee' }}>
                                                        {key}
                                                    </TableCell>
                                                    <TableCell>{String(value)}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            ) : (
                                <Typography color="text.secondary">Bu ürün için detaylı özellik girilmemiş.</Typography>
                            )}
                        </Container>
                    </CustomTabPanel>

                    {/* ─── Yorumlar ─────────────────────────────────────── */}
                    <CustomTabPanel value={tabValue} index={2}>
                        <Container maxWidth="md">

                            {/* AI yorum özeti kartı — yorumlar varsa gösterilir */}
                            <AiReviewSummaryCard
                                productId={id}
                                hasReviews={(product.reviewCount ?? 0) > 0}
                            />

                            {/* Yorum yaz butonu / giriş uyarısı */}
                            <Box sx={{ mb: 4, display: 'flex', justifyContent: 'flex-end' }}>
                                {isAuthenticated ? (
                                    <Button
                                        variant="outlined"
                                        startIcon={<RateReview />}
                                        onClick={() => setReviewDialogOpen(true)}
                                    >
                                        Yorum Yaz
                                    </Button>
                                ) : (
                                    <Typography variant="body2" color="text.secondary">
                                        Yorum yazmak için{' '}
                                        <Link component={RouterLink} to="/" color="primary">
                                            giriş yapın
                                        </Link>
                                    </Typography>
                                )}
                            </Box>

                            {isReviewsLoading ? (
                                <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}>
                                    <CircularProgress />
                                </Box>
                            ) : reviews.length === 0 ? (
                                <Box textAlign="center" py={5}>
                                    <Typography variant="h6" color="text.secondary">
                                        Bu ürüne henüz yorum yapılmamış.
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        İlk yorumu yapan siz olun!
                                    </Typography>
                                </Box>
                            ) : (
                                <Stack spacing={4}>
                                    {reviews.map((review) => {
                                        const initials = review.reviewerName
                                            ? review.reviewerName.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
                                            : '?';
                                        const isOwn =
                                            isAuthenticated &&
                                            currentUser?.sub === review.userId;
                                        const hasVoted = votedReviews.has(review.id);

                                        return (
                                            <Box key={review.id} sx={{ pb: 3, borderBottom: '1px solid #eee' }}>
                                                <Stack direction="row" spacing={2} alignItems="flex-start">
                                                    <Avatar sx={{ bgcolor: 'primary.light', color: 'white', width: 45, height: 45 }}>
                                                        {initials}
                                                    </Avatar>

                                                    <Box sx={{ flexGrow: 1 }}>
                                                        <Stack
                                                            direction={{ xs: 'column', sm: 'row' }}
                                                            alignItems={{ xs: 'flex-start', sm: 'center' }}
                                                            spacing={1}
                                                            mb={0.5}
                                                        >
                                                            <Typography variant="subtitle2" fontWeight="bold">
                                                                {review.reviewerName ?? 'Anonim Kullanıcı'}
                                                            </Typography>
                                                            {review.isVerifiedPurchase && (
                                                                <Chip
                                                                    size="small"
                                                                    icon={<CheckCircle sx={{ '&&': { fontSize: 14 } }} />}
                                                                    label="Doğrulanmış Alıcı"
                                                                    color="success"
                                                                    variant="outlined"
                                                                    sx={{ height: 20, fontSize: '0.65rem' }}
                                                                />
                                                            )}
                                                            <Typography
                                                                variant="caption"
                                                                color="text.secondary"
                                                                sx={{ ml: { sm: 'auto' } }}
                                                            >
                                                                {new Date(review.reviewedAt).toLocaleDateString('tr-TR')}
                                                            </Typography>
                                                        </Stack>

                                                        <Rating value={review.rating} readOnly size="small" sx={{ mb: 1 }} />
                                                        <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                                                            {review.title}
                                                        </Typography>
                                                        <Typography variant="body2" paragraph>
                                                            {review.reviewText}
                                                        </Typography>

                                                        {/* Satıcı Yanıtı */}
                                                        {review.sellerResponse && (
                                                            <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 2, borderLeft: '3px solid', borderColor: 'primary.main' }}>
                                                                <Typography variant="subtitle2" fontWeight="bold" color="primary.main" mb={0.5}>
                                                                    Satıcı Yanıtı:
                                                                </Typography>
                                                                <Typography variant="body2" color="text.secondary">
                                                                    {review.sellerResponse}
                                                                </Typography>
                                                                {review.sellerResponseAt && (
                                                                    <Typography variant="caption" color="text.secondary" display="block" mt={1}>
                                                                        {new Date(review.sellerResponseAt).toLocaleDateString('tr-TR')}
                                                                    </Typography>
                                                                )}
                                                            </Box>
                                                        )}

                                                        {/* Helpful / Sil */}
                                                        <Stack direction="row" alignItems="center" spacing={1} mt={2}>
                                                            <Tooltip title={!isAuthenticated ? 'Oy vermek için giriş yapın' : hasVoted ? 'Zaten oy kullandınız' : ''}>
                                                                <span>
                                                                    <Button
                                                                        size="small"
                                                                        startIcon={<ThumbUpOutlined />}
                                                                        disabled={!isAuthenticated || hasVoted}
                                                                        sx={{ color: 'text.secondary', textTransform: 'none' }}
                                                                        onClick={() => {
                                                                            markHelpful({ reviewId: review.id, helpful: true });
                                                                            setVotedReviews(prev => new Set(prev).add(review.id));
                                                                        }}
                                                                    >
                                                                        Faydalı ({review.helpfulCount})
                                                                    </Button>
                                                                </span>
                                                            </Tooltip>
                                                            <Tooltip title={!isAuthenticated ? 'Oy vermek için giriş yapın' : hasVoted ? 'Zaten oy kullandınız' : ''}>
                                                                <span>
                                                                    <Button
                                                                        size="small"
                                                                        startIcon={<ThumbDownOutlined />}
                                                                        disabled={!isAuthenticated || hasVoted}
                                                                        sx={{ color: 'text.secondary', textTransform: 'none' }}
                                                                        onClick={() => {
                                                                            markHelpful({ reviewId: review.id, helpful: false });
                                                                            setVotedReviews(prev => new Set(prev).add(review.id));
                                                                        }}
                                                                    >
                                                                        Faydasız ({review.notHelpfulCount})
                                                                    </Button>
                                                                </span>
                                                            </Tooltip>
                                                            {isOwn && (
                                                                <Button
                                                                    size="small"
                                                                    startIcon={<DeleteIcon />}
                                                                    color="error"
                                                                    sx={{ textTransform: 'none', ml: 'auto' }}
                                                                    onClick={() => deleteReview(review.id)}
                                                                >
                                                                    Yorumu Sil
                                                                </Button>
                                                            )}
                                                        </Stack>
                                                    </Box>
                                                </Stack>
                                            </Box>
                                        );
                                    })}
                                </Stack>
                            )}
                        </Container>
                    </CustomTabPanel>
                </Paper>

                {/* ── Birlikte sıkça alınanlar (herkese açık) ── */}
                <ProductRail
                    title="Birlikte Sıkça Alınanlar"
                    products={fbtProducts}
                    isLoading={fbtLoading}
                />

                {/* ── Benzer ürünler ── */}
                <ProductRail
                    title="Benzer Ürünler"
                    products={similarProducts}
                    isLoading={similarLoading}
                />
            </Container>

            {/* ─── Yorum Yazma Dialog ───────────────────────────────────── */}
            <Dialog open={reviewDialogOpen} onClose={() => setReviewDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle fontWeight="bold">Yorum Yaz</DialogTitle>
                <DialogContent>
                    <Stack spacing={3} sx={{ mt: 1 }}>
                        <Box>
                            <Typography variant="body2" gutterBottom>Puanınız</Typography>
                            <Rating
                                value={reviewForm.rating}
                                onChange={(_e, val) => setReviewForm((f) => ({ ...f, rating: val ?? 5 }))}
                                size="large"
                            />
                        </Box>
                        <TextField
                            label="Başlık"
                            fullWidth
                            required
                            value={reviewForm.title}
                            onChange={(e) => setReviewForm((f) => ({ ...f, title: e.target.value }))}
                            inputProps={{ maxLength: 100 }}
                        />
                        <TextField
                            label="Yorumunuz"
                            fullWidth
                            required
                            multiline
                            rows={4}
                            value={reviewForm.reviewText}
                            onChange={(e) => setReviewForm((f) => ({ ...f, reviewText: e.target.value }))}
                            inputProps={{ maxLength: 1000 }}
                        />
                        <MultiImageUpload
                            label="Fotoğraf Ekle (isteğe bağlı, maks. 3)"
                            values={reviewForm.imageUrls}
                            onChange={(imgs) => setReviewForm((f) => ({ ...f, imageUrls: imgs }))}
                            onError={(msg) => setSnackbar({ open: true, message: msg, severity: 'error' })}
                            max={3}
                            uploadFn={(file) => productService.uploadReviewImage(id, file)}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setReviewDialogOpen(false)} color="inherit" disabled={isSubmittingReview}>
                        İptal
                    </Button>
                    <Button
                        variant="contained"
                        onClick={handleReviewSubmit}
                        disabled={isSubmittingReview || !reviewForm.title.trim() || !reviewForm.reviewText.trim()}
                    >
                        {isSubmittingReview ? <CircularProgress size={22} color="inherit" /> : 'Gönder'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* ─── Snackbar ─────────────────────────────────────────────── */}
            <Snackbar
                open={snackbar.open}
                autoHideDuration={4000}
                onClose={() => setSnackbar((prev) => ({ ...prev, open: false }))}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert severity={snackbar.severity} sx={{ width: '100%', borderRadius: 2, boxShadow: 3 }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default ProductDetailPage;