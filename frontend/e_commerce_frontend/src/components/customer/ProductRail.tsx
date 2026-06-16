import React from 'react';
import { Box, Typography, Stack, Button, Skeleton } from '@mui/material';
import { ArrowForward } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';
import ProductCard from './ProductCard';
import type { ProductSummary } from '../../types/product';

interface ProductRailProps {
    title: string;
    products: ProductSummary[] | undefined;
    isLoading?: boolean;
    /** "Tümü" linki (opsiyonel) */
    viewAllTo?: string;
    icon?: React.ReactNode;
}

/**
 * Yatay kaydırmalı ürün şeridi (Trendyol-vari rail). Son gezilenler / öne çıkanlar / benzer ürünler
 * gibi bölümler bunu kullanır. Yüklenmiyor + boşsa hiç render edilmez (rail gizlenir).
 */
const ProductRail: React.FC<ProductRailProps> = ({ title, products, isLoading, viewAllTo, icon }) => {
    if (!isLoading && (!products || products.length === 0)) {
        return null;
    }

    return (
        <Box sx={{ mt: 6 }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
                <Stack direction="row" alignItems="center" spacing={1}>
                    {icon}
                    <Typography variant="h5" fontWeight={800}>{title}</Typography>
                </Stack>
                {viewAllTo && (
                    <Button component={RouterLink} to={viewAllTo} endIcon={<ArrowForward />} color="primary" sx={{ fontWeight: 700 }}>
                        Tümü
                    </Button>
                )}
            </Stack>

            <Stack
                direction="row"
                spacing={2}
                sx={{ overflowX: 'auto', pb: 1, '&::-webkit-scrollbar': { height: 8 } }}
            >
                {isLoading
                    ? Array.from({ length: 5 }).map((_, i) => (
                        <Skeleton key={i} variant="rounded" sx={{ flexShrink: 0, width: 200, height: 320, borderRadius: 3 }} />
                    ))
                    : products!.map((product) => (
                        <Box key={product.id} sx={{ flexShrink: 0, width: { xs: 160, sm: 190, md: 210 } }}>
                            <ProductCard product={product} />
                        </Box>
                    ))}
            </Stack>
        </Box>
    );
};

export default ProductRail;
