import React, { useState } from 'react';
import {
    Box, Typography, CircularProgress, Alert,
    Pagination, Select, MenuItem, FormControl, InputLabel,
    Container, Grid, Stack, type SelectChangeEvent
} from '@mui/material';
import { useSearchProducts } from '../../../query/useProductQueries';
import ProductCard from '../../../components/customer/ProductCard';
import FilterListIcon from '@mui/icons-material/FilterList';

const ITEMS_PER_PAGE = 12;

const ProductListPage: React.FC = () => {
    const [page, setPage] = useState(1);
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | ''>('');

    const { data, isLoading, isError, isFetching } = useSearchProducts({
        page: page - 1,
        size: ITEMS_PER_PAGE,
        categoryId: selectedCategoryId === '' ? undefined : selectedCategoryId
    });

    const handlePageChange = (_event: React.ChangeEvent<unknown>, value: number) => {
        setPage(value);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const handleCategoryChange = (event: SelectChangeEvent<number | ''>) => {
        setSelectedCategoryId(event.target.value as number | '');
        setPage(1);
    };

    const showLoading = isLoading || isFetching;

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', py: 4 }}>
            <Container maxWidth="lg">
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    justifyContent="space-between"
                    alignItems={{ xs: 'flex-start', sm: 'center' }}
                    spacing={2}
                    sx={{ mb: 4, pb: 2, borderBottom: '1px solid', borderColor: 'divider' }}
                >
                    <Box>
                        <Typography variant="h4" component="h1" fontWeight="bold" color="text.primary">
                            Tüm Ürünler
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {data?.totalElements || 0} ürün listeleniyor
                        </Typography>
                    </Box>

                    <FormControl sx={{ minWidth: 220 }} size="small" variant="outlined">
                        <InputLabel id="category-select-label" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <FilterListIcon fontSize="small" /> Kategori Filtrele
                        </InputLabel>
                        <Select
                            labelId="category-select-label"
                            value={selectedCategoryId}
                            label="Kategori Filtrele"
                            onChange={handleCategoryChange}
                            sx={{ borderRadius: 2 }}
                        >
                            <MenuItem value="">Tümü</MenuItem>
                            {/* TODO: İleride bu kategorileri de backend'den (Category Service) çekeceğiz */}
                            <MenuItem value={1}>Giyim</MenuItem>
                            <MenuItem value={2}>Aksesuar</MenuItem>
                            <MenuItem value={3}>Elektronik</MenuItem>
                        </Select>
                    </FormControl>
                </Stack>

                {isError && (
                    <Alert severity="error" sx={{ mb: 4 }}>Ürünler listelenirken bir sorun oluştu. Servis şu an yanıt vermiyor olabilir.</Alert>
                )}

                {showLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}><CircularProgress /></Box>
                ) : (
                    <>
                        <Grid container spacing={3}>
                            {data?.content?.map((product: any) => (
                                <Grid size={{ xs: 6, sm: 4, md: 3 }} key={product.id}>
                                    <ProductCard product={product} />
                                </Grid>
                            ))}
                        </Grid>

                        {data && data.totalPages > 1 && (
                            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6, mb: 2 }}>
                                <Pagination
                                    count={data.totalPages}
                                    page={page}
                                    onChange={handlePageChange}
                                    color="primary"
                                    size="large"
                                    shape="rounded"
                                    showFirstButton
                                    showLastButton
                                />
                            </Box>
                        )}
                    </>
                )}
            </Container>
        </Box>
    );
};

export default ProductListPage;