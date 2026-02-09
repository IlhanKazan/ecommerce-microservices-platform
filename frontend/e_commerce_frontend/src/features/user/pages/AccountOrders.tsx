import React from 'react';
import { Typography, Box } from '@mui/material';

const AccountOrders: React.FC = () => {
    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 2 }}>Siparişlerim</Typography>
            <Typography>Henüz hiç siparişiniz bulunmamaktadır.</Typography>
        </Box>
    );
};

export default AccountOrders;