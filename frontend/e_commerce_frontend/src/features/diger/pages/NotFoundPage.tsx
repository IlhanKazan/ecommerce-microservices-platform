import React from 'react';
import { Typography, Box, Button, Container } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';

const NotFoundPage: React.FC = () => {
    return (
        <Container
            maxWidth="md"
            sx={{
                textAlign: 'center',
                py: 10,
                minHeight: '80vh',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center'
            }}
        >
            <Box sx={{ color: 'error.main', mb: 3 }}>
                <SentimentVeryDissatisfiedIcon sx={{ fontSize: 80 }} />
            </Box>

            <Typography variant="h1" component="h1" gutterBottom color="text.primary">
                404
            </Typography>

            <Typography variant="h5" gutterBottom color="text.secondary">
                Üzgünüz, aradığınız sayfayı bulamadık.
            </Typography>

            <Typography variant="body1" sx={{ mb: 4 }} color="text.secondary">
                Sayfa silinmiş olabilir, adı değiştirilmiş olabilir veya yazdığınız adreste bir hata olabilir.
            </Typography>

            <Button
                variant="contained"
                color="primary"
                size="large"
                component={RouterLink}
                to="/"
            >
                Anasayfaya Dön
            </Button>
        </Container>
    );
};

export default NotFoundPage;