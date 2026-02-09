import React from 'react';
import { CircularProgress, Box } from '@mui/material';

const LoadingSpinner: React.FC = () => {
    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '80vh',
            }}
        >
            <CircularProgress size={60} thickness={4} color="primary" />
        </Box>
    );
};

export default LoadingSpinner;