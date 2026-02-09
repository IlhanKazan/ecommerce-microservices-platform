import React from 'react';
import { Box, Typography, Button, Paper } from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import RefreshIcon from '@mui/icons-material/Refresh';

interface ErrorStateProps {
    title?: string;
    description?: string;
    onRetry?: () => void;
    fullHeight?: boolean;
}

const ErrorState: React.FC<ErrorStateProps> = ({
                                                   title = "Bir şeyler ters gitti",
                                                   description = "Veriler yüklenirken beklenmedik bir hata oluştu.",
                                                   onRetry,
                                                   fullHeight = false
                                               }) => {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                p: 4,
                textAlign: 'center',
                minHeight: fullHeight ? '60vh' : 'auto',
                bgcolor: 'background.paper',
                borderRadius: 2,
            }}
        >
            <Paper
                elevation={0}
                sx={{
                    width: 80,
                    height: 80,
                    borderRadius: '50%',
                    bgcolor: 'error.lighter',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    mb: 2,
                    color: 'error.main'
                }}
            >
                <ErrorOutlineIcon sx={{ fontSize: 40 }} />
            </Paper>

            <Typography variant="h6" fontWeight="bold" gutterBottom color="text.primary">
                {title}
            </Typography>

            <Typography variant="body2" color="text.secondary" sx={{ mb: 3, maxWidth: 400 }}>
                {description}
            </Typography>

            {onRetry && (
                <Button
                    variant="outlined"
                    color="primary"
                    startIcon={<RefreshIcon />}
                    onClick={onRetry}
                    sx={{ borderRadius: 2, textTransform: 'none', fontWeight: 'bold' }}
                >
                    Tekrar Dene
                </Button>
            )}
        </Box>
    );
};

export default ErrorState;