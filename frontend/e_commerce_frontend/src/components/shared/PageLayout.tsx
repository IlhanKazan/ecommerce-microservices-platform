import React from 'react';
import Header from './Header';
import Footer from './Footer';
import { Box, Container } from '@mui/material';
import { Outlet } from 'react-router-dom';

const Layout: React.FC = () => {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                minHeight: '100vh'
            }}
        >
            <Header />

            <Box
                component="main"
                sx={{ flexGrow: 1, p: { xs: 2, md: 4 } }}
            >
                <Container maxWidth="lg">

                    <Outlet />

                </Container>
            </Box>

            <Footer />
        </Box>
    );
};

export default Layout;