import { useState, useMemo, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
    Container, Box, Avatar, Typography, Pagination,
    Chip, CircularProgress, Alert, Link, Paper,
    TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem,
    Stack, Button, FormControlLabel, Switch,
} from '@mui/material';
import { Store as StoreIcon, Verified as VerifiedIcon, StorefrontOutlined, Search as SearchIcon } from '@mui/icons-material';
import { useGetTenantStorefront, useSearchProducts } from '../../../query/useProductQueries';
import ProductCard from '../../../components/customer/ProductCard';
import { ProductGridSkeleton } from '../../../components/shared/ProductCardSkeleton';
import EmptyState from '../../../components/shared/EmptyState';
import { tokens } from '../../../utils/themeTokens';
import { useDebounce } from '../../../hooks/useDebounce';
import type { ProductSearchPayload } from '../../../types/product';

type StoreSort = NonNullable<ProductSearchPayload['sortBy']>;

export default function StorePage() {
    const { tenantId } = useParams<{ tenantId: string }>();
    const id = Number(tenantId);
    const [page, setPage] = useState(0);

    // Arama + filtre (mağaza içi, tenantId sabit)
    const [keywordInput, setKeywordInput] = useState('');
    const [sortBy, setSortBy] = useState<StoreSort>('newest');
    const [inStockOnly, setInStockOnly] = useState(false);
    const [priceDraft, setPriceDraft] = useState<{ min: string; max: string }>({ min: '', max: '' });
    const [priceApplied, setPriceApplied] = useState<{ min?: number; max?: number }>({});
    const keyword = useDebounce(keywordInput, 300);

    // Filtre/arama değişince ilk sayfaya dön
    useEffect(() => {
        setPage(0);
    }, [keyword, sortBy, inStockOnly, priceApplied]);

    const { data: storefront, isLoading: isStorefrontLoading, isError: isStorefrontError } =
        useGetTenantStorefront(id);

    const storeSearch = useMemo<ProductSearchPayload>(
        () => ({
            tenantId: id,
            page,
            size: 20,
            sortBy,
            ...(keyword.trim() ? { keyword: keyword.trim() } : {}),
            ...(inStockOnly ? { inStock: true } : {}),
            ...(priceApplied.min != null ? { minPrice: priceApplied.min } : {}),
            ...(priceApplied.max != null ? { maxPrice: priceApplied.max } : {}),
        }),
        [id, page, sortBy, keyword, inStockOnly, priceApplied],
    );
    const { data: productsPage, isLoading: isProductsLoading } = useSearchProducts(storeSearch);

    // Öne çıkan ürünler şeridi (mağazaya özel, ilk 8)
    const featuredSearch = useMemo<ProductSearchPayload>(
        () => ({ tenantId: id, featured: true, sortBy: 'newest', page: 0, size: 8 }),
        [id],
    );
    const { data: featuredPage } = useSearchProducts(featuredSearch);
    const featuredProducts = featuredPage?.content ?? [];

    const applyPrice = () => {
        setPriceApplied({
            min: priceDraft.min.trim() ? Number(priceDraft.min) : undefined,
            max: priceDraft.max.trim() ? Number(priceDraft.max) : undefined,
        });
    };

    if (isStorefrontLoading) {
        return (
            <Container maxWidth="lg" sx={{ py: 8, display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
            </Container>
        );
    }

    if (isStorefrontError || !storefront) {
        return (
            <Container maxWidth="lg" sx={{ py: 4 }}>
                <Alert severity="error">Mağaza bulunamadı.</Alert>
            </Container>
        );
    }

    const products = productsPage?.content ?? [];
    const totalPages = productsPage?.totalPages ?? 1;
    const isStoreOpen = storefront.status === 'ACTIVE';

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 6 }}>
            {/* Mağaza banner */}
            <Box sx={{ background: tokens.gradient.dark, height: { xs: 120, md: 160 } }} />

            <Container maxWidth="lg">
                <Paper
                    elevation={0}
                    sx={{
                        mt: { xs: -6, md: -8 }, p: { xs: 2.5, md: 3.5 }, borderRadius: 4,
                        boxShadow: tokens.shadow.md,
                        display: 'flex', alignItems: 'flex-start', gap: 3, flexWrap: 'wrap',
                    }}
                >
                    <Avatar
                        src={storefront.logoUrl ?? undefined}
                        alt={storefront.name}
                        sx={{
                            width: { xs: 80, md: 104 }, height: { xs: 80, md: 104 },
                            fontSize: '2.5rem', bgcolor: 'primary.main',
                            border: '4px solid white', boxShadow: tokens.shadow.sm,
                        }}
                    >
                        <StoreIcon fontSize="large" />
                    </Avatar>

                    <Box sx={{ flex: 1, minWidth: 200 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                            <Typography variant="h4" fontWeight={800}>{storefront.name}</Typography>
                            {storefront.isVerified && (
                                <Chip icon={<VerifiedIcon />} label="Doğrulanmış" size="small" color="primary" variant="outlined" />
                            )}
                        </Box>

                        {storefront.businessName && (
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                                {storefront.businessName}
                            </Typography>
                        )}
                        {storefront.description && (
                            <Typography variant="body2" sx={{ mt: 1, maxWidth: 640 }}>
                                {storefront.description}
                            </Typography>
                        )}
                        {storefront.websiteUrl && (
                            <Link href={storefront.websiteUrl} target="_blank" rel="noopener noreferrer" variant="body2" sx={{ mt: 1, display: 'inline-block' }}>
                                {storefront.websiteUrl}
                            </Link>
                        )}
                    </Box>
                </Paper>

                {/* Mağaza kapalı/duraklatılmış uyarısı */}
                {!isStoreOpen && (
                    <Box sx={{ mt: 5 }}>
                        <EmptyState
                            icon={<StorefrontOutlined />}
                            title="Bu mağaza şu an kapalı"
                            description="Mağaza geçici olarak satışa kapatılmıştır. Lütfen daha sonra tekrar deneyin."
                        />
                    </Box>
                )}

                {/* Öne çıkan ürünler şeridi */}
                {isStoreOpen && featuredProducts.length > 0 && (
                    <Box sx={{ mt: 5 }}>
                        <Typography variant="h5" fontWeight={800} sx={{ mb: 3 }}>
                            ⭐ Öne Çıkanlar
                        </Typography>
                        <Box sx={{
                            display: 'grid',
                            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', md: 'repeat(4, 1fr)' },
                            gap: { xs: 1.5, sm: 2, md: 3 },
                        }}>
                            {featuredProducts.map((product) => (
                                <ProductCard key={`featured-${product.id}`} product={product} />
                            ))}
                        </Box>
                    </Box>
                )}

                {/* Ürünler */}
                {isStoreOpen && (
                <Box sx={{ mt: 5 }}>
                    <Typography variant="h5" fontWeight={800} sx={{ mb: 3 }}>
                        Mağaza Ürünleri
                        {productsPage && (
                            <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1.5 }}>
                                ({productsPage.totalElements} ürün)
                            </Typography>
                        )}
                    </Typography>

                    {/* Mağaza içi arama + filtre */}
                    <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }} flexWrap="wrap" useFlexGap>
                            <TextField
                                placeholder="Bu mağazada ara..."
                                size="small"
                                value={keywordInput}
                                onChange={(e) => setKeywordInput(e.target.value)}
                                sx={{ flex: 1, minWidth: 220 }}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon fontSize="small" color="action" />
                                        </InputAdornment>
                                    ),
                                }}
                            />
                            <FormControl size="small" sx={{ minWidth: 170 }}>
                                <InputLabel>Sırala</InputLabel>
                                <Select
                                    label="Sırala"
                                    value={sortBy}
                                    onChange={(e) => setSortBy(e.target.value as StoreSort)}
                                >
                                    <MenuItem value="newest">En Yeni</MenuItem>
                                    <MenuItem value="price_asc">Fiyat: Artan</MenuItem>
                                    <MenuItem value="price_desc">Fiyat: Azalan</MenuItem>
                                    <MenuItem value="rating">Puan</MenuItem>
                                    <MenuItem value="popular">Popüler</MenuItem>
                                </Select>
                            </FormControl>
                            <Stack direction="row" spacing={1} alignItems="center">
                                <TextField
                                    label="Min ₺" size="small" type="number"
                                    value={priceDraft.min}
                                    onChange={(e) => setPriceDraft((p) => ({ ...p, min: e.target.value }))}
                                    sx={{ width: 100 }}
                                />
                                <TextField
                                    label="Max ₺" size="small" type="number"
                                    value={priceDraft.max}
                                    onChange={(e) => setPriceDraft((p) => ({ ...p, max: e.target.value }))}
                                    sx={{ width: 100 }}
                                />
                                <Button size="small" variant="outlined" onClick={applyPrice}>Uygula</Button>
                            </Stack>
                            <FormControlLabel
                                control={<Switch checked={inStockOnly} onChange={(e) => setInStockOnly(e.target.checked)} />}
                                label="Sadece stokta"
                            />
                        </Stack>
                    </Paper>

                    {isProductsLoading ? (
                        <ProductGridSkeleton count={8} />
                    ) : products.length === 0 ? (
                        <EmptyState
                            icon={<StorefrontOutlined />}
                            title="Henüz ürün yok"
                            description="Bu mağazada şu an satışta ürün bulunmuyor."
                        />
                    ) : (
                        <>
                            <Box sx={{
                                display: 'grid',
                                gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', md: 'repeat(4, 1fr)' },
                                gap: { xs: 1.5, sm: 2, md: 3 },
                            }}>
                                {products.map((product) => (
                                    <ProductCard key={product.id} product={product} />
                                ))}
                            </Box>

                            {totalPages > 1 && (
                                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
                                    <Pagination
                                        count={totalPages} page={page + 1}
                                        onChange={(_, val) => setPage(val - 1)}
                                        color="primary" shape="rounded"
                                    />
                                </Box>
                            )}
                        </>
                    )}
                </Box>
                )}
            </Container>
        </Box>
    );
}
