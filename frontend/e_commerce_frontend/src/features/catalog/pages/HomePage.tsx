import React from 'react';
import { Typography, Box, Button, CircularProgress, Alert, Container, Grid, Paper, Stack, Avatar, useTheme } from '@mui/material';
import { useSearchProducts } from '../../../query/useProductQueries';
import { Link as RouterLink } from 'react-router-dom';
import ProductCard from '../../../components/customer/ProductCard';
import { ArrowForward, LocalOffer } from '@mui/icons-material';

const HomePage: React.FC = () => {
    const { data, isLoading, isError } = useSearchProducts({ page: 0, size: 8 });
    const theme = useTheme();

    const categories = [
        { name: 'Teknoloji', color: '#e3f2fd', icon: '💻' },
        { name: 'Moda', color: '#fce4ec', icon: '👗' },
        { name: 'Ev & Yaşam', color: '#e8f5e9', icon: '🏠' },
        { name: 'Kozmetik', color: '#f3e5f5', icon: '💄' },
        { name: 'Spor', color: '#fff3e0', icon: '⚽' },
        { name: 'Aksesuar', color: '#e0f7fa', icon: '⌚' },
    ];

    return (
        <Box sx={{ bgcolor: 'background.paper', minHeight: '100vh', pb: 8 }}>

            <Box
                sx={{
                    background: `linear-gradient(135deg, ${theme.palette.secondary.main} 0%, ${theme.palette.primary.dark} 100%)`,
                    color: 'white',
                    py: { xs: 8, md: 12 },
                    px: 2,
                    borderRadius: { xs: '0 0 20px 20px', md: '0 0 50% 20px/ 0 0 30px 30px' },
                    mb: 6,
                    position: 'relative',
                    overflow: 'hidden',
                    boxShadow: 3
                }}
            >
                <Box sx={{ position: 'absolute', top: -50, right: -50, width: 300, height: 300, bgcolor: 'rgba(255,255,255,0.05)', borderRadius: '50%' }} />
                <Box sx={{ position: 'absolute', bottom: -30, left: 20, width: 150, height: 150, bgcolor: 'rgba(255,255,255,0.05)', borderRadius: '50%' }} />

                <Container maxWidth="md" sx={{ textAlign: 'center' }}>
                    <Typography variant="overline" sx={{ letterSpacing: 3, color: 'primary.light', fontWeight: 'bold' }}>
                        YENİ SEZON
                    </Typography>
                    <Typography variant="h2" component="h1" fontWeight="900" sx={{ mb: 3, fontSize: { xs: '2.5rem', md: '4rem' }, lineHeight: 1.2 }}>
                        Tarzını Keşfetmeye Hazır Mısın?
                    </Typography>
                    <Typography variant="h6" sx={{ mb: 5, opacity: 0.9, fontWeight: 300, maxWidth: 600, mx: 'auto' }}>
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
                            color: 'secondary.main',
                            borderRadius: 8,
                            px: 5,
                            py: 1.5,
                            fontSize: '1.1rem',
                            fontWeight: 'bold',
                            boxShadow: 2,
                            '&:hover': { bgcolor: 'grey.100' }
                        }}
                    >
                        Alışverişe Başla
                    </Button>
                </Container>
            </Box>

            <Container maxWidth="lg">

                <Box sx={{ mb: 8 }}>
                    <Stack
                        direction="row"
                        spacing={{ xs: 3, md: 5 }}
                        justifyContent="center"
                        sx={{ overflowX: 'auto', py: 2 }}
                    >
                        {categories.map((cat) => (
                            <Stack key={cat.name} alignItems="center" spacing={1} sx={{ cursor: 'pointer', transition: 'transform 0.2s', '&:hover': { transform: 'translateY(-5px)' } }}>
                                <Avatar
                                    sx={{
                                        width: { xs: 60, md: 80 },
                                        height: { xs: 60, md: 80 },
                                        bgcolor: cat.color,
                                        fontSize: { xs: '1.5rem', md: '2rem' },
                                        boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
                                    }}
                                >
                                    {cat.icon}
                                </Avatar>
                                <Typography variant="body2" fontWeight="600" color="text.primary">
                                    {cat.name}
                                </Typography>
                            </Stack>
                        ))}
                    </Stack>
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 4, borderBottom: '1px solid', borderColor: 'divider', pb: 2 }}>
                    <Stack direction="row" alignItems="center" spacing={1}>
                        <LocalOffer color="primary" fontSize="large" />
                        <Typography variant="h5" fontWeight="800" color="text.primary">
                            Öne Çıkan Fırsatlar
                        </Typography>
                    </Stack>
                    <Button component={RouterLink} to="/productlist" endIcon={<ArrowForward />} color="primary" sx={{ fontWeight: 'bold' }}>
                        Tüm Ürünler
                    </Button>
                </Box>

                {isLoading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 10 }}>
                        <CircularProgress />
                    </Box>
                )}

                {isError && (
                    <Alert severity="error" sx={{ mb: 3 }}>Ürünler yüklenirken bir hata oluştu.</Alert>
                )}

                {data?.content && (
                    <Grid container spacing={3}>
                        {data.content.map((product: any) => (
                            <Grid size={{ xs: 6, sm: 4, md: 3 }} key={product.id}>
                                <ProductCard product={product} />
                            </Grid>
                        ))}
                    </Grid>
                )}

                <Paper
                    elevation={0}
                    sx={{
                        mt: 8,
                        p: { xs: 4, md: 6 },
                        borderRadius: 4,
                        background: `linear-gradient(to right, ${theme.palette.primary.main}, ${theme.palette.secondary.light})`,
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        flexWrap: 'wrap',
                        gap: 3,
                        boxShadow: 3
                    }}
                >
                    <Box sx={{ maxWidth: 600 }}>
                        <Typography variant="h4" fontWeight="900" gutterBottom>
                            Mobil Uygulamamızı İndirin(Tamamlandığında...)!
                        </Typography>
                        <Typography variant="h6" sx={{ fontWeight: 300, opacity: 0.9 }}>
                            Özel indirimlerden ve flaş fırsatlardan anında haberdar olun.
                        </Typography>
                    </Box>
                    <Button variant="contained" size="large" sx={{ bgcolor: 'white', color: 'primary.main', borderRadius: 8, fontWeight: 'bold', px: 4, '&:hover': { bgcolor: 'grey.100' } }}>
                        Hemen İndir
                    </Button>
                </Paper>

            </Container>
        </Box>
    );
};

export default HomePage;