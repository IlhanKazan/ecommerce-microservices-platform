import React from 'react';
import { Box, Typography, Alert } from '@mui/material';

const MerchantOrdersStub: React.FC = () => (
    <Box>
        <Typography variant="h4" fontWeight="bold" gutterBottom>
            Sipariş Yönetimi
        </Typography>
        <Alert severity="info">
            Order Service henüz geliştirilme aşamasında. Bu bölüm yakında aktif olacak.
        </Alert>
    </Box>
);

export { MerchantOrdersStub as MerchantOrders };
export default MerchantOrdersStub;