import React, { useState } from 'react';
import {
    Box, Typography, Stack, Paper, Alert, MenuItem,
    TextField, CircularProgress, Avatar, Rating, Chip,
    Button, Divider, Collapse, InputAdornment,
} from '@mui/material';
import {
    Reply as ReplyIcon,
    CheckCircle as VerifiedIcon,
    Search as SearchIcon,
    ExpandLess as ExpandLessIcon,
    Send as SendIcon,
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useNotification } from '../../../components/shared/NotificationProvider';
import {
    useGetTenantProducts,
    useGetProductReviews,
} from '../../../query/useProductQueries';
import { productService } from '../../catalog/api/productService';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { ProductReviewDTO } from '../../../types/product';

// ─── Seller Reply Form ────────────────────────────────────────────────────────

interface SellerReplyFormProps {
    productId: number;
    reviewId: number;
    tenantId: number;
    existingReply: string | null;
    onSuccess: () => void;
}

const SellerReplyForm: React.FC<SellerReplyFormProps> = ({
                                                             productId,
                                                             reviewId,
                                                             tenantId,
                                                             existingReply,
                                                             onSuccess,
                                                         }) => {
    const { notify }      = useNotification();
    const queryClient     = useQueryClient();
    const [text, setText] = useState(existingReply ?? '');
    const [open, setOpen] = useState(!!existingReply);

    const { mutate, isPending } = useMutation({
        mutationFn: (response: string) =>
            productService.addSellerResponse(tenantId, productId, reviewId, response),
        onSuccess: () => {
            notify('Yanıtınız başarıyla kaydedildi.', 'success');
            queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
            onSuccess();
        },
        onError: () => notify('Yanıt kaydedilirken hata oluştu.', 'error'),
    });

    return (
        <Box>
            <Button
                size="small"
                startIcon={open ? <ExpandLessIcon /> : <ReplyIcon />}
                onClick={() => setOpen((v) => !v)}
                sx={{ textTransform: 'none', color: 'primary.main' }}
            >
                {existingReply ? 'Yanıtı Düzenle' : 'Yanıtla'}
            </Button>

            <Collapse in={open}>
                <Box sx={{ mt: 1.5, pl: 1 }}>
                    {existingReply && !open ? null : (
                        <Stack spacing={1}>
                            <TextField
                                multiline
                                rows={3}
                                fullWidth
                                size="small"
                                placeholder="Müşteriye yanıtınızı yazın..."
                                value={text}
                                onChange={(e) => setText(e.target.value)}
                                inputProps={{ maxLength: 1000 }}
                            />
                            <Stack direction="row" justifyContent="flex-end" spacing={1}>
                                <Button
                                    size="small"
                                    color="inherit"
                                    onClick={() => { setOpen(false); setText(existingReply ?? ''); }}
                                    disabled={isPending}
                                >
                                    İptal
                                </Button>
                                <Button
                                    size="small"
                                    variant="contained"
                                    disabled={!text.trim() || isPending}
                                    onClick={() => mutate(text.trim())}
                                    startIcon={
                                        isPending
                                            ? <CircularProgress size={14} color="inherit" />
                                            : <SendIcon />
                                    }
                                >
                                    {isPending ? 'Gönderiliyor...' : 'Yanıtı Kaydet'}
                                </Button>
                            </Stack>
                        </Stack>
                    )}
                </Box>
            </Collapse>
        </Box>
    );
};

// ─── Single Review Card ───────────────────────────────────────────────────────

interface ReviewCardProps {
    review: ProductReviewDTO;
    productId: number;
    tenantId: number;
}

const ReviewCard: React.FC<ReviewCardProps> = ({ review, productId, tenantId }) => {
    const avatarLetter = review.userId?.charAt(0).toUpperCase() ?? 'A';

    const sentimentColor: Record<string, 'success' | 'error' | 'warning' | 'default'> = {
        POSITIVE: 'success',
        NEGATIVE: 'error',
        NEUTRAL:  'warning',
    };

    return (
        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 2 }}>
            <Stack direction="row" spacing={2} alignItems="flex-start">
                <Avatar sx={{ bgcolor: 'primary.light', width: 40, height: 40, flexShrink: 0 }}>
                    {avatarLetter}
                </Avatar>

                <Box sx={{ flexGrow: 1 }}>
                    {/* Başlık satırı */}
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        alignItems={{ sm: 'center' }}
                        justifyContent="space-between"
                        spacing={1}
                        mb={0.5}
                    >
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Typography variant="body2" fontWeight="bold">Alıcı</Typography>

                            {review.isVerifiedPurchase && (
                                <Chip
                                    size="small"
                                    icon={<VerifiedIcon sx={{ '&&': { fontSize: 12 } }} />}
                                    label="Doğrulanmış Alım"
                                    color="success"
                                    variant="outlined"
                                    sx={{ height: 20, fontSize: '0.65rem' }}
                                />
                            )}

                            {review.sentimentLabel && (
                                <Chip
                                    size="small"
                                    label={review.sentimentLabel}
                                    color={sentimentColor[review.sentimentLabel] ?? 'default'}
                                    variant="outlined"
                                    sx={{ height: 20, fontSize: '0.65rem' }}
                                />
                            )}
                        </Stack>

                        <Typography variant="caption" color="text.secondary">
                            {new Date(review.reviewedAt).toLocaleDateString('tr-TR', {
                                day: 'numeric', month: 'long', year: 'numeric',
                            })}
                        </Typography>
                    </Stack>

                    <Rating value={review.rating} readOnly size="small" sx={{ mb: 0.5 }} />

                    <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                        {review.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" paragraph>
                        {review.reviewText}
                    </Typography>

                    {/* Faydalı sayıları */}
                    <Stack direction="row" spacing={2} mb={1.5}>
                        <Typography variant="caption" color="text.secondary">
                            👍 {review.helpfulCount} faydalı
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            👎 {review.notHelpfulCount} faydasız
                        </Typography>
                    </Stack>

                    {/* Mevcut satıcı yanıtı */}
                    {review.sellerResponse && (
                        <Box
                            sx={{
                                mb: 2, p: 2,
                                bgcolor: 'primary.50',
                                borderRadius: 2,
                                borderLeft: '3px solid',
                                borderColor: 'primary.main',
                            }}
                        >
                            <Typography variant="subtitle2" fontWeight="bold" color="primary.main" mb={0.5}>
                                Satıcı Yanıtı
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {review.sellerResponse}
                            </Typography>
                            {review.sellerResponseAt && (
                                <Typography variant="caption" color="text.disabled" display="block" mt={0.5}>
                                    {new Date(review.sellerResponseAt).toLocaleDateString('tr-TR')}
                                </Typography>
                            )}
                        </Box>
                    )}

                    <Divider sx={{ mb: 1.5 }} />

                    {/* Yanıt formu */}
                    <SellerReplyForm
                        productId={productId}
                        reviewId={review.id}
                        tenantId={tenantId}
                        existingReply={review.sellerResponse}
                        onSuccess={() => {}}
                    />
                </Box>
            </Stack>
        </Paper>
    );
};

// ─── Product Reviews Panel ────────────────────────────────────────────────────

interface ProductReviewsPanelProps {
    productId: number;
    tenantId: number;
}

const ProductReviewsPanel: React.FC<ProductReviewsPanelProps> = ({ productId, tenantId }) => {
    const [page] = useState(0);
    const { data, isLoading, isError } = useGetProductReviews(productId, page, 20);
    const reviews = data?.content ?? [];

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
                <CircularProgress />
            </Box>
        );
    }

    if (isError) {
        return <Alert severity="error">Yorumlar yüklenirken hata oluştu.</Alert>;
    }

    if (reviews.length === 0) {
        return (
            <Box sx={{ py: 6, textAlign: 'center' }}>
                <Typography color="text.secondary">Bu ürüne henüz yorum yapılmamış.</Typography>
            </Box>
        );
    }

    return (
        <Stack spacing={2}>
            <Typography variant="body2" color="text.secondary">
                {data?.totalElements ?? 0} değerlendirme
            </Typography>
            {reviews.map((review) => (
                <ReviewCard
                    key={review.id}
                    review={review}
                    productId={productId}
                    tenantId={tenantId}
                />
            ))}
        </Stack>
    );
};

// ─── Page ─────────────────────────────────────────────────────────────────────

const MerchantReviewsPage: React.FC = () => {
    const { activeTenant } = useMerchantStore();

    const [productSearch,     setProductSearch]     = useState('');
    const [selectedProductId, setSelectedProductId] = useState<number | ''>('');

    const tenantId = activeTenant?.id ?? 0;

    const { data: productsPage, isLoading: loadingProducts } = useGetTenantProducts(tenantId, 0, 200);
    const products = productsPage?.content ?? [];

    const filtered = productSearch.trim()
        ? products.filter((p) =>
            p.name.toLowerCase().includes(productSearch.toLowerCase()),
        )
        : products;

    if (!activeTenant) {
        return <Alert severity="warning">Aktif mağaza bulunamadı.</Alert>;
    }

    return (
        <Box>
            <Typography variant="h4" fontWeight="bold" gutterBottom>
                Değerlendirme Yönetimi
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={4}>
                Müşteri yorumlarını görüntüleyin ve yanıtlayın.
            </Typography>

            {/* Ürün seçici */}
            <Paper variant="outlined" sx={{ p: 3, borderRadius: 3, mb: 4 }}>
                <Typography variant="subtitle1" fontWeight="bold" mb={2}>
                    Ürün Seçin
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                        size="small"
                        placeholder="Ürün adı ile filtrele..."
                        value={productSearch}
                        onChange={(e) => setProductSearch(e.target.value)}
                        sx={{ minWidth: 240 }}
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon fontSize="small" />
                                    </InputAdornment>
                                ),
                            },
                        }}
                    />
                    <TextField
                        select
                        size="small"
                        label="Ürün"
                        value={selectedProductId}
                        onChange={(e) => setSelectedProductId(Number(e.target.value))}
                        sx={{ minWidth: 320 }}
                        disabled={loadingProducts}
                    >
                        <MenuItem value="">— Ürün seçin —</MenuItem>
                        {filtered.map((p) => (
                            <MenuItem key={p.id} value={p.id}>
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <Typography variant="body2">{p.name}</Typography>
                                    {p.reviewCount > 0 && (
                                        <Chip
                                            size="small"
                                            label={`${p.reviewCount} yorum`}
                                            sx={{ height: 18, fontSize: '0.65rem' }}
                                        />
                                    )}
                                </Stack>
                            </MenuItem>
                        ))}
                    </TextField>
                </Stack>
            </Paper>

            {/* Yorumlar */}
            {!selectedProductId ? (
                <Box
                    sx={{
                        py: 10,
                        textAlign: 'center',
                        bgcolor: 'grey.50',
                        borderRadius: 3,
                        border: '1px dashed',
                        borderColor: 'divider',
                    }}
                >
                    <ReplyIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
                    <Typography color="text.secondary">
                        Değerlendirmeleri görmek için yukarıdan bir ürün seçin.
                    </Typography>
                </Box>
            ) : (
                <ProductReviewsPanel
                    productId={selectedProductId}
                    tenantId={tenantId}
                />
            )}
        </Box>
    );
};

export default MerchantReviewsPage;