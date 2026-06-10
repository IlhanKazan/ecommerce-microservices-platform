import React from 'react';
import { Card, Box, Skeleton, Grid } from '@mui/material';

/** ProductCard ile aynı iskelet — yüklenirken layout zıplamasını önler. */
export const ProductCardSkeleton: React.FC = () => (
    <Card sx={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', borderRadius: 2 }}>
        <Box sx={{ position: 'relative', paddingTop: '100%', bgcolor: 'grey.50' }}>
            <Skeleton
                variant="rectangular"
                sx={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
            />
        </Box>
        <Box sx={{ px: 1.5, pt: 1.25, pb: 1.5, flexGrow: 1 }}>
            <Skeleton width="40%" height={14} />
            <Skeleton width="90%" height={20} sx={{ mt: 0.5 }} />
            <Skeleton width="70%" height={20} />
            <Skeleton width="35%" height={16} sx={{ mt: 1 }} />
            <Box sx={{ mt: 2 }}>
                <Skeleton width="50%" height={26} />
            </Box>
        </Box>
    </Card>
);

interface ProductGridSkeletonProps {
    /** Kaç skeleton kart gösterilsin (varsayılan 8). */
    count?: number;
    /** Grid sütun boyutları (ProductCard listeleriyle aynı). */
    size?: { xs: number; sm: number; md: number; lg?: number };
}

/** Ürün grid'i için skeleton seti — liste/anasayfa/mağaza sayfalarında kullanılır. */
export const ProductGridSkeleton: React.FC<ProductGridSkeletonProps> = ({
    count = 8,
    size = { xs: 6, sm: 4, md: 3 },
}) => (
    <Grid container spacing={{ xs: 1.5, sm: 2, md: 3 }}>
        {Array.from({ length: count }).map((_, i) => (
            <Grid key={i} size={size}>
                <ProductCardSkeleton />
            </Grid>
        ))}
    </Grid>
);

export default ProductCardSkeleton;
