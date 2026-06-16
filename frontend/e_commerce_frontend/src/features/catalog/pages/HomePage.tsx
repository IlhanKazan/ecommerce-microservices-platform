import React from 'react';
import { Typography, Box, Button, Alert, Container, Paper, Stack, Avatar } from '@mui/material';
import {
    useSearchProducts, useGetCategories, useGetProductsByIds, useRelatedProducts,
} from '../../../query/useProductQueries';
import { useRecentlyViewed } from '../../../query/useUserQueries';
import { useCategoryStore } from '../../../store/useCategoryStore';
import { useAuthStore } from '../../../store/useAuthStore';
import { useFavoriteStore } from '../../../store/useFavoriteStore';
import { useBasketItemCount } from '../../../query/useBasketQueries';
import { Link as RouterLink } from 'react-router-dom';
import ProductCard from '../../../components/customer/ProductCard';
import ProductRail from '../../../components/customer/ProductRail';
import { ProductGridSkeleton } from '../../../components/shared/ProductCardSkeleton';
import {
    ArrowForward, LocalOffer, LocalShippingOutlined,
    VerifiedUserOutlined, ReplayOutlined, SupportAgentOutlined,
    HistoryOutlined, RecommendOutlined, WhatshotOutlined, ShoppingCartOutlined, StarOutlined,
} from '@mui/icons-material';
import { tokens } from '../../../utils/themeTokens';

// Modül seviyesinde sabit payload'lar (her render'da yeni obje üretilmesin)
const TREND_SEARCH = { page: 0, size: 10, sortBy: 'popular' } as const;
const FEATURED_SEARCH = { page: 0, size: 10, featured: true, sortBy: 'newest' } as const;

// Sabit search payload — referans modül seviyesinde, her render'da yeni obje üretilmez
const HOME_SEARCH = { page: 0, size: 8, sortBy: 'newest' } as const;

// Kategori avatarları için döngüsel pastel arka planlar (gerçek kategori adına bağlanır)
const CAT_COLORS = ['#FFF1E6', '#E8F2FB', '#E6F4EC', '#FDECEA', '#F3E8FB', '#FFF6E5'];

const TRUST_ITEMS = [
    { icon: <LocalShippingOutlined />, title: 'Hızlı Kargo', desc: 'Aynı gün kargo' },
    { icon: <VerifiedUserOutlined />, title: '%100 Orijinal', desc: 'Güvenli alışveriş' },
    { icon: <ReplayOutlined />, title: 'Kolay İade', desc: '14 gün koşulsuz' },
    { icon: <SupportAgentOutlined />, title: '7/24 Destek', desc: 'Her zaman yanında' },
];

const HomePage: React.FC = () => {
    const { data, isLoading, isError } = useSearchProducts(HOME_SEARCH);

    // Gerçek kategoriler — store'dan (App.tsx dolduruyor), yoksa fetch
    const storedCategories = useCategoryStore((s) => s.categories);
    const isLoaded = useCategoryStore((s) => s.isLoaded);
    const { data: fetchedCategories } = useGetCategories();
    const topCategories = (isLoaded ? storedCategories : (fetchedCategories ?? [])).slice(0, 6);

    // ── Kişiselleştirilmiş rail'ler (giriş yapmış kullanıcı) ──
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const favoriteIds = useFavoriteStore((s) => s.ids);

    const { data: recentIds } = useRecentlyViewed(isAuthenticated, 12);
    const { data: recentProducts, isLoading: recentLoading } = useGetProductsByIds(recentIds ?? []);

    // "Senin İçin" tohumları: son gezilen + favori (dedup). AI Engine sonradan bu yolu devralır.
    const seedIds = React.useMemo(() => {
        const set = new Set<number>([...(recentIds ?? []), ...Array.from(favoriteIds)]);
        return Array.from(set).slice(0, 10);
    }, [recentIds, favoriteIds]);
    const { data: forYou, isLoading: forYouLoading } = useRelatedProducts(
        isAuthenticated ? seedIds : [], (recentIds ?? []), 12);

    // Trend (çok satanlar) + Öne çıkanlar (merchant-featured) — herkese açık
    const { data: trend, isLoading: trendLoading } = useSearchProducts(TREND_SEARCH);
    const { data: featured, isLoading: featuredLoading } = useSearchProducts(FEATURED_SEARCH);

    // Sepette unuttukların — giriş yapmış + sepeti dolu kullanıcıya hatırlatma
    const cartCount = useBasketItemCount();

    return (
        <Box sx={{ bgcolor: 'background.default', minHeight: '100vh', pb: 8 }}>
            {/* ── Hero ── */}
            <Box
                sx={{
                    background: tokens.gradient.sunset,
                    color: 'white',
                    py: { xs: 7, md: 11 },
                    px: 2,
                    position: 'relative',
                    overflow: 'hidden',
                }}
            >
                <Box sx={{ position: 'absolute', top: -80, right: -60, width: 320, height: 320, bgcolor: 'rgba(255,255,255,0.06)', borderRadius: '50%' }} />
                <Box sx={{ position: 'absolute', bottom: -50, left: -20, width: 200, height: 200, bgcolor: 'rgba(255,255,255,0.05)', borderRadius: '50%' }} />

                <Container maxWidth="md" sx={{ textAlign: 'center', position: 'relative' }}>
                    <Typography sx={{ letterSpacing: 4, color: 'rgba(255,255,255,0.85)', fontWeight: 700, fontSize: '0.8rem', mb: 1.5 }}>
                        YENİ SEZON
                    </Typography>
                    <Typography variant="h1" component="h1" sx={{ mb: 2.5, fontSize: { xs: '2.25rem', md: '3.5rem' }, fontWeight: 800 }}>
                        Tarzını Keşfetmeye Hazır Mısın?
                    </Typography>
                    <Typography variant="h6" sx={{ mb: 4, opacity: 0.92, fontWeight: 400, maxWidth: 580, mx: 'auto' }}>
                        En trend ürünler, özel indirimler ve kaçırılmayacak fırsatlar şimdi kapında.
                    </Typography>
                    <Button
                        variant="contained"
                        size="large"
                        component={RouterLink}
                        to="/productlist"
                        endIcon={<ArrowForward />}
                        sx={{
                            bgcolor: 'white',
                            color: 'primary.main',
                            borderRadius: 999,
                            px: 5, py: 1.5,
                            fontSize: '1.05rem',
                            fontWeight: 700,
                            '&:hover': { bgcolor: 'grey.100', transform: 'translateY(-2px)' },
                        }}
                    >
                        Alışverişe Başla
                    </Button>
                </Container>
            </Box>

            <Container maxWidth="lg" sx={{ mt: { xs: -4, md: -5 }, position: 'relative' }}>
                {/* ── Güven şeridi ── */}
                <Paper
                    elevation={0}
                    sx={{
                        p: { xs: 2, md: 3 },
                        borderRadius: 3,
                        boxShadow: tokens.shadow.md,
                        display: 'grid',
                        gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
                        gap: { xs: 2, md: 1 },
                    }}
                >
                    {TRUST_ITEMS.map((item) => (
                        <Stack key={item.title} direction="row" alignItems="center" spacing={1.5} sx={{ px: { md: 2 } }}>
                            <Box sx={{
                                width: 44, height: 44, flexShrink: 0, borderRadius: 2,
                                bgcolor: 'primary.lighter', color: 'primary.main',
                                display: 'flex', alignItems: 'center', justifyContent: 'center',
                            }}>
                                {item.icon}
                            </Box>
                            <Box sx={{ minWidth: 0 }}>
                                <Typography variant="subtitle2" fontWeight={700} noWrap>{item.title}</Typography>
                                <Typography variant="caption" color="text.secondary" noWrap>{item.desc}</Typography>
                            </Box>
                        </Stack>
                    ))}
                </Paper>

                {/* ── Sepette Unuttukların ── */}
                {isAuthenticated && cartCount > 0 && (
                    <Paper
                        elevation={0}
                        sx={{
                            mt: 4, p: { xs: 2, md: 2.5 }, borderRadius: 3,
                            boxShadow: tokens.shadow.sm, bgcolor: 'primary.lighter',
                            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                            gap: 2, flexWrap: 'wrap',
                        }}
                    >
                        <Stack direction="row" spacing={1.5} alignItems="center">
                            <ShoppingCartOutlined color="primary" />
                            <Typography fontWeight={700}>
                                Sepetinde {cartCount} ürün seni bekliyor
                            </Typography>
                        </Stack>
                        <Button
                            variant="contained"
                            component={RouterLink}
                            to="/cart"
                            endIcon={<ArrowForward />}
                            sx={{ borderRadius: 999, fontWeight: 700 }}
                        >
                            Sepete Git
                        </Button>
                    </Paper>
                )}

                {/* ── Kategoriler (gerçek, tıklanınca filtreler) ── */}
                {topCategories.length > 0 && (
                    <Box sx={{ mt: 6 }}>
                        <Typography variant="h5" fontWeight={800} sx={{ mb: 3 }}>Kategoriler</Typography>
                        <Stack direction="row" spacing={{ xs: 2, md: 4 }} sx={{ overflowX: 'auto', pb: 1 }}>
                            {topCategories.map((cat, i) => (
                                <Stack
                                    key={cat.id}
                                    component={RouterLink}
                                    to={`/productlist?categoryId=${cat.id}`}
                                    alignItems="center"
                                    spacing={1}
                                    sx={{
                                        textDecoration: 'none', flexShrink: 0,
                                        transition: 'transform 0.2s',
                                        '&:hover': { transform: 'translateY(-4px)' },
                                    }}
                                >
                                    <Avatar
                                        sx={{
                                            width: { xs: 64, md: 76 }, height: { xs: 64, md: 76 },
                                            bgcolor: CAT_COLORS[i % CAT_COLORS.length],
                                            color: 'secondary.main', fontWeight: 800, fontSize: '1.4rem',
                                            boxShadow: tokens.shadow.sm,
                                        }}
                                    >
                                        {cat.name.charAt(0).toUpperCase()}
                                    </Avatar>
                                    <Typography variant="body2" fontWeight={600} color="text.primary" noWrap sx={{ maxWidth: 90, textAlign: 'center' }}>
                                        {cat.name}
                                    </Typography>
                                </Stack>
                            ))}
                        </Stack>
                    </Box>
                )}

                {/* ── Senin İçin Önerilenler (giriş yapmışsa) ── */}
                {isAuthenticated && (
                    <ProductRail
                        title="Senin İçin Önerilenler"
                        products={forYou}
                        isLoading={forYouLoading}
                        icon={<RecommendOutlined color="primary" />}
                    />
                )}

                {/* ── Son Gezdiklerin (giriş yapmışsa) ── */}
                {isAuthenticated && (
                    <ProductRail
                        title="Son Gezdiklerin"
                        products={recentProducts}
                        isLoading={recentLoading}
                        icon={<HistoryOutlined color="action" />}
                    />
                )}

                {/* ── Öne Çıkanlar (merchant-featured) ── */}
                <ProductRail
                    title="Öne Çıkanlar"
                    products={featured?.content}
                    isLoading={featuredLoading}
                    icon={<StarOutlined sx={{ color: 'warning.main' }} />}
                />

                {/* ── Çok Satanlar / Trend ── */}
                <ProductRail
                    title="Çok Satanlar"
                    products={trend?.content}
                    isLoading={trendLoading}
                    viewAllTo="/productlist?sortBy=popular"
                    icon={<WhatshotOutlined color="error" />}
                />

                {/* ── Öne çıkanlar ── */}
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 7, mb: 3 }}>
                    <Stack direction="row" alignItems="center" spacing={1}>
                        <LocalOffer color="primary" />
                        <Typography variant="h5" fontWeight={800}>Öne Çıkan Fırsatlar</Typography>
                    </Stack>
                    <Button component={RouterLink} to="/productlist" endIcon={<ArrowForward />} color="primary" sx={{ fontWeight: 700 }}>
                        Tümü
                    </Button>
                </Box>

                {isError ? (
                    <Alert severity="error">Ürünler yüklenirken bir hata oluştu.</Alert>
                ) : isLoading ? (
                    <ProductGridSkeleton count={8} />
                ) : (
                    <Box sx={{
                        display: 'grid',
                        gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', md: 'repeat(4, 1fr)' },
                        gap: { xs: 1.5, sm: 2, md: 3 },
                    }}>
                        {data?.content?.map((product) => (
                            <ProductCard key={product.id} product={product} />
                        ))}
                    </Box>
                )}

                {/* ── Kampanya bandı ── */}
                <Paper
                    elevation={0}
                    sx={{
                        mt: 8, p: { xs: 4, md: 6 }, borderRadius: 4,
                        background: tokens.gradient.brand,
                        color: 'white',
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        flexWrap: 'wrap', gap: 3,
                        boxShadow: tokens.shadow.lg,
                    }}
                >
                    <Box sx={{ maxWidth: 600 }}>
                        <Typography variant="h4" fontWeight={800} gutterBottom>
                            Fırsatları Kaçırma!
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 400, opacity: 0.95 }}>
                            Binlerce üründe özel indirimler ve flaş fırsatlar seni bekliyor.
                        </Typography>
                    </Box>
                    <Button
                        variant="contained"
                        size="large"
                        component={RouterLink}
                        to="/productlist"
                        sx={{ bgcolor: 'white', color: 'primary.main', borderRadius: 999, fontWeight: 700, px: 4, '&:hover': { bgcolor: 'grey.100' } }}
                    >
                        Hemen Keşfet
                    </Button>
                </Paper>
            </Container>
        </Box>
    );
};

export default HomePage;
