import { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
    Container, Box, Avatar, Typography, Pagination,
    Chip, CircularProgress, Alert, Link, Paper,
} from '@mui/material';
import { Store as StoreIcon, Verified as VerifiedIcon, StorefrontOutlined } from '@mui/icons-material';
import { useGetTenantStorefront, useSearchProducts } from '../../../query/useProductQueries';
import ProductCard from '../../../components/customer/ProductCard';
import { ProductGridSkeleton } from '../../../components/shared/ProductCardSkeleton';
import EmptyState from '../../../components/shared/EmptyState';
import { tokens } from '../../../utils/themeTokens';

export default function StorePage() {
    const { tenantId } = useParams<{ tenantId: string }>();
    const id = Number(tenantId);
    const [page, setPage] = useState(0);

    const { data: storefront, isLoading: isStorefrontLoading, isError: isStorefrontError } =
        useGetTenantStorefront(id);

    const storeSearch = useMemo(
        () => ({ tenantId: id, page, size: 20, sortBy: 'newest' as const }),
        [id, page],
    );
    const { data: productsPage, isLoading: isProductsLoading } = useSearchProducts(storeSearch);

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

                {/* Ürünler */}
                <Box sx={{ mt: 5 }}>
                    <Typography variant="h5" fontWeight={800} sx={{ mb: 3 }}>
                        Mağaza Ürünleri
                        {productsPage && (
                            <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1.5 }}>
                                ({productsPage.totalElements} ürün)
                            </Typography>
                        )}
                    </Typography>

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
            </Container>
        </Box>
    );
}
