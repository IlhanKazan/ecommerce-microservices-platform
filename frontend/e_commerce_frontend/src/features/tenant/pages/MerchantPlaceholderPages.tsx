import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Construction as BuildIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

interface PlaceholderProps {
    title: string;
}

const PlaceholderPage: React.FC<PlaceholderProps> = ({ title }) => {
    const navigate = useNavigate();
    return (
        <Box sx={{
            textAlign: 'center', py: 10, px: 3,
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2
        }}>
            <BuildIcon sx={{ fontSize: 80, color: 'text.secondary', opacity: 0.5 }} />
            <Typography variant="h4" fontWeight="bold" color="text.secondary">
                {title}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 500 }}>
                Bu modül şu anda geliştirme aşamasındadır. Backend endpointleri hazırlanıyor.
            </Typography>
            <Button variant="outlined" onClick={() => navigate('/merchant/dashboard')}>
                Panele Dön
            </Button>
        </Box>
    );
};

export const MerchantProducts = () => <PlaceholderPage title="Ürün Yönetimi" />;
export const MerchantOrders = () => <PlaceholderPage title="Sipariş Yönetimi" />;
export const MerchantReviews = () => <PlaceholderPage title="Değerlendirmeler" />;