import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
    Box, Typography, Alert, Pagination, Select, MenuItem, FormControl,
    Container, Stack, Paper, List, ListItemButton, ListItemText, Divider,
    TextField, Button, Switch, FormControlLabel, IconButton, Drawer, Collapse,
    InputAdornment, type SelectChangeEvent,
} from '@mui/material';
import TuneIcon from '@mui/icons-material/Tune';
import CloseIcon from '@mui/icons-material/Close';
import SearchOffIcon from '@mui/icons-material/SearchOff';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { useSearchProducts, useGetCategories } from '../../../query/useProductQueries';
import { useCategoryStore } from '../../../store/useCategoryStore';
import { collectCategoryIds, findCategoryPath } from '../../../utils/categoryUtils';
import ProductCard from '../../../components/customer/ProductCard';
import { ProductGridSkeleton } from '../../../components/shared/ProductCardSkeleton';
import EmptyState from '../../../components/shared/EmptyState';
import type { ProductSearchPayload, CategoryResponse } from '../../../types/product';

const ITEMS_PER_PAGE = 12;

type SortOption = NonNullable<ProductSearchPayload['sortBy']>;
const SORT_OPTIONS: { value: SortOption; label: string }[] = [
    { value: 'newest', label: 'En Yeniler' },
    { value: 'price_asc', label: 'Fiyat: Artan' },
    { value: 'price_desc', label: 'Fiyat: Azalan' },
    { value: 'rating', label: 'En Çok Beğenilen' },
    { value: 'popular', label: 'Popüler' },
];

const ProductListPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const keywordFromUrl = searchParams.get('keyword') || '';
    const categoryIdFromUrl = searchParams.get('categoryId');

    const [page, setPage] = useState(1);
    const [selectedCategoryId, setSelectedCategoryId] = useState<number | ''>(
        categoryIdFromUrl ? Number(categoryIdFromUrl) : '',
    );
    const [keyword, setKeyword] = useState(keywordFromUrl);
    const [sortBy, setSortBy] = useState<SortOption>('newest');
    const [inStockOnly, setInStockOnly] = useState(false);
    // Fiyat taslağı (apply'a basınca uygulanır → her tuşta refetch yok)
    const [priceDraft, setPriceDraft] = useState<{ min: string; max: string }>({ min: '', max: '' });
    const [appliedPrice, setAppliedPrice] = useState<{ min?: number; max?: number }>({});
    const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

    const storedCategories = useCategoryStore((s) => s.categories);
    const categoriesLoaded = useCategoryStore((s) => s.isLoaded);
    const { data: fetchedCategories } = useGetCategories();
    const categoryTree = useMemo(
        () => (categoriesLoaded ? storedCategories : (fetchedCategories ?? [])),
        [categoriesLoaded, storedCategories, fetchedCategories],
    );
    // Sidebar'da açık (genişletilmiş) kategori düğümleri — varsayılan hepsi kapalı
    const [expandedCategories, setExpandedCategories] = useState<Set<number>>(new Set());

    const toggleCategoryExpand = (id: number) => {
        setExpandedCategories((prev) => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    // URL'den/dışarıdan bir kategori seçilince ata zincirini açık başlat
    useEffect(() => {
        if (selectedCategoryId !== '' && categoryTree.length > 0) {
            const path = findCategoryPath(selectedCategoryId, categoryTree);
            if (path.length > 0) {
                setExpandedCategories((prev) => new Set([...prev, ...path]));
            }
        }
    }, [selectedCategoryId, categoryTree]);

    // URL → state senkron
    useEffect(() => {
        setKeyword(keywordFromUrl);
        setPage(1);
    }, [keywordFromUrl]);

    useEffect(() => {
        setSelectedCategoryId(categoryIdFromUrl ? Number(categoryIdFromUrl) : '');
        setPage(1);
    }, [categoryIdFromUrl]);

    const resolvedCategoryIds = useMemo(
        () => (selectedCategoryId !== '' ? collectCategoryIds(selectedCategoryId, categoryTree) : undefined),
        [selectedCategoryId, categoryTree],
    );

    // Search payload'ı stabilize et — referans gereksiz değişmesin
    const searchPayload = useMemo(
        () => ({
            page: page > 0 ? page - 1 : 0,
            size: ITEMS_PER_PAGE,
            categoryIds: resolvedCategoryIds,
            keyword: keyword === '' ? undefined : keyword,
            inStock: inStockOnly ? true : undefined,
            minPrice: appliedPrice.min,
            maxPrice: appliedPrice.max,
            sortBy,
        }),
        [page, resolvedCategoryIds, keyword, inStockOnly, appliedPrice.min, appliedPrice.max, sortBy],
    );

    const { data, isLoading, isError, isFetching } = useSearchProducts(searchPayload);

    const handlePageChange = (_e: React.ChangeEvent<unknown>, value: number) => {
        setPage(value);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const selectCategory = (id: number | '') => {
        setSelectedCategoryId(id);
        setPage(1);
        setMobileFilterOpen(false);
    };

    const applyPrice = () => {
        setAppliedPrice({
            min: priceDraft.min ? Number(priceDraft.min) : undefined,
            max: priceDraft.max ? Number(priceDraft.max) : undefined,
        });
        setPage(1);
    };

    const hasActiveFilters =
        selectedCategoryId !== '' || inStockOnly || appliedPrice.min != null || appliedPrice.max != null;

    const clearFilters = () => {
        setSelectedCategoryId('');
        setInStockOnly(false);
        setPriceDraft({ min: '', max: '' });
        setAppliedPrice({});
        setSortBy('newest');
        setPage(1);
    };

    // Sadece ilk yüklemede (henüz veri yokken) skeleton; sonraki fetch'lerde keepPreviousData ile grid korunur
    const showSkeleton = isLoading;

    const renderCategoryNodes = (nodes: CategoryResponse[], depth = 0): React.ReactNode =>
        nodes.map((cat) => {
            const hasChildren = cat.subCategories.length > 0;
            const isOpen = expandedCategories.has(cat.id);
            return (
                <React.Fragment key={cat.id}>
                    <ListItemButton
                        selected={selectedCategoryId === cat.id}
                        onClick={() => selectCategory(cat.id)}
                        sx={{
                            borderRadius: 1.5, pl: 1 + depth * 1.5, pr: 0.5,
                            '&.Mui-selected': { bgcolor: 'primary.lighter', color: 'primary.dark' },
                        }}
                    >
                        <ListItemText
                            primary={cat.name}
                            primaryTypographyProps={{ fontSize: '0.85rem', fontWeight: selectedCategoryId === cat.id ? 700 : 500 }}
                        />
                        {hasChildren && (
                            <IconButton
                                size="small"
                                edge="end"
                                aria-label={isOpen ? 'Daralt' : 'Genişlet'}
                                onClick={(e) => { e.stopPropagation(); toggleCategoryExpand(cat.id); }}
                                sx={{ ml: 0.5, color: 'text.secondary' }}
                            >
                                {isOpen ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
                            </IconButton>
                        )}
                    </ListItemButton>
                    {hasChildren && (
                        <Collapse in={isOpen} timeout="auto" unmountOnExit>
                            {renderCategoryNodes(cat.subCategories, depth + 1)}
                        </Collapse>
                    )}
                </React.Fragment>
            );
        });

    const FilterPanel = (
        <Stack spacing={3}>
            {/* Kategoriler */}
            <Box>
                <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1 }}>Kategoriler</Typography>
                <List dense disablePadding sx={{ maxHeight: 320, overflowY: 'auto' }}>
                    <ListItemButton
                        selected={selectedCategoryId === ''}
                        onClick={() => selectCategory('')}
                        sx={{ borderRadius: 1.5, '&.Mui-selected': { bgcolor: 'primary.lighter', color: 'primary.dark' } }}
                    >
                        <ListItemText primary="Tümü" primaryTypographyProps={{ fontWeight: selectedCategoryId === '' ? 700 : 500 }} />
                    </ListItemButton>
                    {renderCategoryNodes(categoryTree)}
                </List>
            </Box>

            <Divider />

            {/* Fiyat aralığı */}
            <Box>
                <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1.5 }}>Fiyat Aralığı</Typography>
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1.5 }}>
                    <TextField
                        size="small" placeholder="Min" type="number" value={priceDraft.min}
                        onChange={(e) => setPriceDraft((p) => ({ ...p, min: e.target.value }))}
                        InputProps={{ endAdornment: <InputAdornment position="end">₺</InputAdornment> }}
                    />
                    <Typography color="text.disabled">–</Typography>
                    <TextField
                        size="small" placeholder="Max" type="number" value={priceDraft.max}
                        onChange={(e) => setPriceDraft((p) => ({ ...p, max: e.target.value }))}
                        InputProps={{ endAdornment: <InputAdornment position="end">₺</InputAdornment> }}
                    />
                </Stack>
                <Button fullWidth size="small" variant="outlined" onClick={applyPrice}>Uygula</Button>
            </Box>

            <Divider />

            {/* Stok */}
            <FormControlLabel
                control={<Switch checked={inStockOnly} onChange={(e) => { setInStockOnly(e.target.checked); setPage(1); }} color="primary" />}
                label={<Typography variant="body2" fontWeight={600}>Sadece stoktakiler</Typography>}
            />

            {hasActiveFilters && (
                <Button color="inherit" size="small" onClick={clearFilters} sx={{ color: 'text.secondary' }}>
                    Filtreleri Temizle
                </Button>
            )}
        </Stack>
    );

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', py: 4 }}>
            <Container maxWidth="lg">
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    justifyContent="space-between"
                    alignItems={{ xs: 'flex-start', sm: 'center' }}
                    spacing={2}
                    sx={{ mb: 3 }}
                >
                    <Box>
                        <Typography variant="h4" component="h1" fontWeight={800}>
                            {keyword ? `"${keyword}" için Sonuçlar` : 'Tüm Ürünler'}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {data?.totalElements ?? 0} ürün listeleniyor
                        </Typography>
                    </Box>

                    <Stack direction="row" spacing={1} alignItems="center">
                        <Button
                            variant="outlined" startIcon={<TuneIcon />}
                            onClick={() => setMobileFilterOpen(true)}
                            sx={{ display: { xs: 'flex', md: 'none' } }}
                        >
                            Filtrele
                        </Button>
                        <FormControl size="small" sx={{ minWidth: 180 }}>
                            <Select
                                value={sortBy}
                                onChange={(e: SelectChangeEvent) => { setSortBy(e.target.value as SortOption); setPage(1); }}
                            >
                                {SORT_OPTIONS.map((o) => (
                                    <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Stack>
                </Stack>

                <Box sx={{ display: 'flex', gap: 3, alignItems: 'flex-start' }}>
                    {/* Sol filtre paneli — desktop */}
                    <Paper
                        elevation={0}
                        sx={{
                            display: { xs: 'none', md: 'block' },
                            width: 260, flexShrink: 0, p: 2.5, borderRadius: 3,
                            boxShadow: (t) => t.shadows[0], border: '1px solid', borderColor: 'divider',
                            position: 'sticky', top: 88,
                        }}
                    >
                        {FilterPanel}
                    </Paper>

                    {/* Sonuçlar */}
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        {isError && (
                            <Alert severity="error" sx={{ mb: 3 }}>
                                Ürünler listelenirken bir sorun oluştu. Servis şu an yanıt vermiyor olabilir.
                            </Alert>
                        )}

                        {showSkeleton ? (
                            <ProductGridSkeleton count={9} size={{ xs: 6, sm: 4, md: 4 }} />
                        ) : data?.content?.length === 0 ? (
                            <EmptyState
                                icon={<SearchOffIcon />}
                                title="Sonuç bulunamadı"
                                description="Aradığınız kriterlere uygun ürün yok. Filtreleri değiştirmeyi deneyin."
                                actionLabel={hasActiveFilters ? 'Filtreleri Temizle' : undefined}
                                onAction={clearFilters}
                            />
                        ) : (
                            <Box sx={{
                                display: 'grid',
                                gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)' },
                                gap: { xs: 1.5, sm: 2, md: 2.5 },
                                // Yeni veri gelirken (filtre/sayfa değişimi) mevcut grid'i hafif soldur — flicker yok
                                opacity: isFetching ? 0.55 : 1,
                                transition: 'opacity 0.2s ease',
                                pointerEvents: isFetching ? 'none' : 'auto',
                            }}>
                                {data?.content?.map((product) => (
                                    <ProductCard key={product.id} product={product} />
                                ))}
                            </Box>
                        )}

                        {data && data.totalPages > 1 && !showSkeleton && (
                            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 6, mb: 2 }}>
                                <Pagination
                                    count={data.totalPages} page={page} onChange={handlePageChange}
                                    color="primary" size="large" shape="rounded" showFirstButton showLastButton
                                />
                            </Box>
                        )}
                    </Box>
                </Box>
            </Container>

            {/* Mobil filtre drawer */}
            <Drawer anchor="right" open={mobileFilterOpen} onClose={() => setMobileFilterOpen(false)}>
                <Box sx={{ width: 300, p: 2.5 }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                        <Typography variant="h6" fontWeight={800}>Filtrele</Typography>
                        <IconButton onClick={() => setMobileFilterOpen(false)}><CloseIcon /></IconButton>
                    </Stack>
                    {FilterPanel}
                </Box>
            </Drawer>
        </Box>
    );
};

export default ProductListPage;
