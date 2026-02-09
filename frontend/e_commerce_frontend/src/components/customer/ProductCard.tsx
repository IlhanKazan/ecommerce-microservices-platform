import React from 'react';
import {
    Typography,
    Card,
    CardContent,
    CardMedia,
    Box,
    IconButton,
    Rating,
    Button,
    Stack,
    Chip
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { FavoriteBorder, ShoppingCartOutlined, Visibility } from '@mui/icons-material';
import type { Product } from '../../types';
import photo from './react.svg';

interface ProductCardProps {
    product: Product;
}

const ProductCard: React.FC<ProductCardProps> = ({ product }) => {
    const formattedPrice = new Intl.NumberFormat('tr-TR', {
        style: 'currency',
        currency: 'TRY',
    }).format(product.price);

    return (
        <Card
            elevation={0}
            sx={{
                bgcolor: 'white',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                border: '1px solid #e0e0e0',
                borderRadius: 3,
                position: 'relative',
                overflow: 'hidden',
                transition: 'all 0.3s ease',
                '&:hover': {
                    transform: 'translateY(-5px)',
                    boxShadow: '0 10px 20px rgba(0,0,0,0.1)',
                    borderColor: 'transparent',
                    '& .action-buttons': {
                        transform: 'translateY(0)',
                        opacity: 1
                    }
                }
            }}
        >
            <Chip
                label="Yeni"
                size="small"
                color="secondary"
                sx={{ position: 'absolute', top: 10, left: 10, zIndex: 2, fontSize: '0.7rem', height: 20 }}
            />

            <IconButton
                size="small"
                sx={{ position: 'absolute', top: 8, right: 8, zIndex: 2, bgcolor: 'rgba(255,255,255,0.8)', '&:hover': { bgcolor: 'white', color: 'red' } }}
            >
                <FavoriteBorder fontSize="small" />
            </IconButton>

            <Box sx={{ position: 'relative', paddingTop: '100%', overflow: 'hidden' }}>

                <Box
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    sx={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: '100%',
                        zIndex: 1,
                        display: 'block'
                    }}
                >
                    <CardMedia
                        component="img"
                        image={photo}
                        alt={product.name}
                        sx={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'contain',
                            p: 2,
                        }}
                    />
                </Box>

                <Box
                    className="action-buttons"
                    sx={{
                        position: 'absolute',
                        bottom: 0,
                        left: 0,
                        right: 0,
                        zIndex: 3,
                        padding: 1,
                        background: 'linear-gradient(to top, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0) 100%)',
                        transform: 'translateY(100%)',
                        opacity: 0,
                        transition: 'all 0.3s ease-in-out',
                        display: { xs: 'none', md: 'block' }
                    }}
                >
                    <Button
                        component={RouterLink}
                        to={`/product/${product.id}`}
                        variant="contained"
                        fullWidth
                        size="small"
                        startIcon={<Visibility />}
                        color="primary"
                        sx={{ borderRadius: 2, boxShadow: 2, textTransform: 'none' }}
                    >
                        Hızlı İncele
                    </Button>
                </Box>
            </Box>

            <CardContent sx={{ flexGrow: 1, pt: 1, px: 2, pb: 2, zIndex: 2, bgcolor: 'white' }}>
                <Typography
                    component={RouterLink}
                    to={`/product/${product.id}`}
                    variant="subtitle2"
                    sx={{
                        fontWeight: 600,
                        mb: 0.5,
                        textDecoration: 'none',
                        color: 'text.primary',
                        display: '-webkit-box',
                        overflow: 'hidden',
                        WebkitBoxOrient: 'vertical',
                        WebkitLineClamp: 2,
                        minHeight: '2.5em',
                        '&:hover': { color: 'primary.main' }
                    }}
                >
                    {product.name}
                </Typography>

                <Stack direction="row" alignItems="center" spacing={0.5} mb={1}>
                    <Rating value={product.rating} readOnly size="small" precision={0.5} sx={{ fontSize: '0.9rem' }} />
                    <Typography variant="caption" color="text.secondary">({product.rating})</Typography>
                </Stack>

                <Stack direction="row" justifyContent="space-between" alignItems="flex-end" mt="auto">
                    <Typography variant="h6" color="primary.main" fontWeight="bold">
                        {formattedPrice}
                    </Typography>

                    <IconButton
                        color="primary"
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 2,
                            '&:hover': { bgcolor: 'primary.main', color: 'white', borderColor: 'primary.main' }
                        }}
                    >
                        <ShoppingCartOutlined fontSize="small" />
                    </IconButton>
                </Stack>
            </CardContent>
        </Card>
    );
};

export default ProductCard;