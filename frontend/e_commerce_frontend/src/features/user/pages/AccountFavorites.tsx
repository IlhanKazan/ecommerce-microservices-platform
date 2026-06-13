import React from 'react';
import { Box, Typography, CircularProgress, Alert, Grid } from '@mui/material';
import { FavoriteBorder } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { productService } from '../../catalog/api/productService';
import { useFavoriteStore } from '../../../store/useFavoriteStore';
import ProductCard from '../../../components/customer/ProductCard';
import EmptyState from '../../../components/shared/EmptyState';

const AccountFavorites: React.FC = () => {
    const { data, isLoading, isError } = useQuery({
        queryKey: ['favorites'],
        queryFn: () => productService.getFavorites(),
    });

    // Sayfadan kalbe basıp çıkarınca kart anında kaybolsun diye store ile kesişim alınır
    const favoriteIds = useFavoriteStore((s) => s.ids);

    if (isLoading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', p: 6 }}><CircularProgress /></Box>;
    }
    if (isError) {
        return <Alert severity="error">Favoriler yüklenirken hata oluştu.</Alert>;
    }

    const items = (data ?? []).filter((p) => favoriteIds.has(Number(p.id)));

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Favorilerim</Typography>

            {items.length === 0 ? (
                <EmptyState
                    icon={<FavoriteBorder sx={{ fontSize: 48 }} />}
                    title="Henüz favoriniz yok"
                    description="Beğendiğiniz ürünleri kalp ikonuna tıklayarak favorilerinize ekleyin."
                    actionLabel="Ürünleri keşfet"
                    actionTo="/productlist"
                />
            ) : (
                <Grid container spacing={2}>
                    {items.map((p) => (
                        <Grid size={{ xs: 6, sm: 4 }} key={p.id}>
                            <ProductCard product={p} />
                        </Grid>
                    ))}
                </Grid>
            )}
        </Box>
    );
};

export default AccountFavorites;
