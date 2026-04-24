import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
    Box, Typography, CircularProgress, Alert,
    Pagination, Select, MenuItem, FormControl, InputLabel,
    Container, Grid, Stack, type SelectChangeEvent
} from '@mui/material';
import FilterListIcon from '@mui/icons-material/FilterList';
import { useSearchProducts, useGetCategories } from '../../../query/useProductQueries';
import { useCategoryStore } from '../../../store/useCategoryStore';
import { collectCategoryIds, flattenCategories } from '../../../utils/categoryUtils';
import ProductCard from '../../../components/customer/ProductCard';

const ITEMS_PER_PAGE = 12;

const ProductListPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const keywordFromUrl = searchParams.get('keyword') || '';

    const [page, setPage] = useState(1);
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | ''>('');
    const [keyword, setKeyword] = useState(keywordFromUrl);

    // ─── Kategoriler ──────────────────────────────────────────────────────────
    // Store'dan kategori ağacını al; App.tsx zaten fetch etmiş olabilir
    const { categories: storedCategories, isLoaded: categoriesLoaded } = useCategoryStore();

    // Store boşsa (ilk yüklemede veya yenileme sonrası) tekrar fetch et
    const { data: fetchedCategories } = useGetCategories();
    const categoryTree = categoriesLoaded ? storedCategories : (fetchedCategories ?? []);

    // Dropdown için düzleştirilmiş liste
    const flatCategories = flattenCategories(categoryTree);

    // ─── Keyword URL sync ─────────────────────────────────────────────────────
    useEffect(() => {
        setKeyword(keywordFromUrl);
        setPage(1);
    }, [keywordFromUrl]);

    // ─── Search payload ───────────────────────────────────────────────────────
    // Seçilen kategori + tüm torunlarının ID'lerini hesapla
    const resolvedCategoryIds =
        selectedCategoryId !== ''
            ? collectCategoryIds(selectedCategoryId, categoryTree)
            : undefined;

    const { data, isLoading, isError, isFetching } = useSearchProducts({
        page: page > 0 ? page - 1 : 0,
        size: ITEMS_PER_PAGE,
        categoryIds: resolvedCategoryIds,
        keyword: keyword === '' ? undefined : keyword,
    });

    // ─── Handlers ────────────────────────────────────────────────────────────
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
                            {keyword ? `"${keyword}" için Sonuçlar` : 'Tüm Ürünler'}
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

                            {flatCategories.map((cat) => (
                                <MenuItem
                                    key={cat.id}
                                    value={cat.id}
                                    sx={{ pl: 2 + cat.depth * 1.5 }} // alt kategorileri indent et
                                >
                                    {cat.depth > 0 && (
                                        <Typography
                                            component="span"
                                            sx={{ color: 'text.disabled', mr: 0.5, fontSize: '0.75rem' }}
                                        >
                                            {'└ '.repeat(cat.depth)}
                                        </Typography>
                                    )}
                                    {cat.name}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Stack>

                {isError && (
                    <Alert severity="error" sx={{ mb: 4 }}>
                        Ürünler listelenirken bir sorun oluştu. Servis şu an yanıt vermiyor olabilir.
                    </Alert>
                )}

                {showLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}>
                        <CircularProgress />
                    </Box>
                ) : (
                    <>
                        {data?.content?.length === 0 ? (
                            <Alert severity="info" sx={{ mt: 2 }}>
                                Aradığınız kriterlere uygun ürün bulunamadı.
                            </Alert>
                        ) : (
                            <Grid container spacing={3}>
                                {data?.content?.map((product) => (
                                    <Grid size={{ xs: 6, sm: 4, md: 3 }} key={product.id}>
                                        <ProductCard product={product} />
                                    </Grid>
                                ))}
                            </Grid>
                        )}

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