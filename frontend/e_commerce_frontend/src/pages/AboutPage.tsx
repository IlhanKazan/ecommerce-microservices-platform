import React from 'react';
import { Typography, Box, Divider } from '@mui/material';

const AboutPage: React.FC = () => {
    return (
        <Box sx={{ py: { xs: 4, md: 8 } }}>
            <Typography variant="h3" component="h1" gutterBottom>
                Hakkımızda
            </Typography>
            <Divider sx={{ mb: 4 }} />

            <Box sx={{ mb: 5 }}>
                <Typography variant="h5" component="h2" gutterBottom color="primary">
                    Misyonumuz
                </Typography>
                <Typography variant="body1" paragraph>
                    Backend geliştiricilerinden oluşan ekibimiz(ben), en karmaşık iş mantıklarını en hızlı ve en güvenilir API'ler aracılığıyla sunmayı hedefler. Temiz kod, performans ve ölçeklenebilirlik, misyonumuzun temelini oluşturur.
                </Typography>
            </Box>

            <Box>
                <Typography variant="h5" component="h2" gutterBottom color="primary">
                    Vizyonumuz
                </Typography>
                <Typography variant="body1" paragraph>
                    Tüm frontend sorunlarını çözebilen, hatasız çalışan ve geliştirme süreçlerini basitleştiren altyapılar inşa ederek, teknoloji dünyasında lider bir çözüm ortağı olmaktır. TypeScript ile geleceği kodluyoruz.
                </Typography>
            </Box>
        </Box>
    );
};

export default AboutPage;