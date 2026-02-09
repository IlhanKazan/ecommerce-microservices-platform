import React from 'react';
import { Box, Typography, Link } from '@mui/material';

const Footer: React.FC = () => {
    return (
        <Box
            component="footer"
            sx={{
                bgcolor: 'grey.800',
                color: 'white',
                p: { xs: 3, md: 6 },
                mt: 'auto',
                width: '100%'
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: { xs: 'column', md: 'row' },
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    maxWidth: '1200px',
                    margin: '0 auto',
                }}
            >
                <Typography
                    variant="body2"
                    align="center"
                    sx={{ mb: { xs: 2, md: 0 } }}
                >
                    &copy; {new Date().getFullYear()} Tüm Hakları Saklıdır.
                </Typography>

                <Box>
                    <Link href="#" color="inherit" sx={{ mx: 1 }}>Gizlilik</Link>
                    <Link href="#" color="inherit" sx={{ mx: 1 }}>Şartlar</Link>
                    <Link href="#" color="inherit" sx={{ mx: 1 }}>Yardım</Link>
                </Box>
            </Box>
        </Box>
    );
};

export default Footer;