import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import InboxOutlinedIcon from '@mui/icons-material/Inbox';

interface EmptyStateProps {
    /** Üst kısımda gösterilecek ikon (varsayılan: kutu). */
    icon?: React.ReactNode;
    title: string;
    description?: string;
    /** CTA butonu metni — verilmezse buton render edilmez. */
    actionLabel?: string;
    /** Tıklama handler'ı (onClick) — actionTo verilirse yok sayılır. */
    onAction?: () => void;
    /** RouterLink hedefi — verilirse buton link gibi davranır. */
    actionTo?: string;
    /** Dikey ortalama için min yükseklik. */
    fullHeight?: boolean;
}

const EmptyState: React.FC<EmptyStateProps> = ({
    icon,
    title,
    description,
    actionLabel,
    onAction,
    actionTo,
    fullHeight = false,
}) => {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
                px: 3,
                py: { xs: 6, md: 8 },
                minHeight: fullHeight ? '60vh' : 'auto',
            }}
        >
            <Box
                sx={{
                    width: 96,
                    height: 96,
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    mb: 2.5,
                    bgcolor: 'primary.lighter',
                    color: 'primary.main',
                    '& svg': { fontSize: 46 },
                }}
            >
                {icon ?? <InboxOutlinedIcon />}
            </Box>

            <Typography variant="h6" fontWeight={700} gutterBottom color="text.primary">
                {title}
            </Typography>

            {description && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3, maxWidth: 420 }}>
                    {description}
                </Typography>
            )}

            {actionLabel && (actionTo ? (
                <Button
                    component={RouterLink}
                    to={actionTo}
                    variant="contained"
                    color="primary"
                    size="large"
                    sx={{ borderRadius: 2 }}
                >
                    {actionLabel}
                </Button>
            ) : (
                <Button
                    variant="contained"
                    color="primary"
                    size="large"
                    onClick={onAction}
                    sx={{ borderRadius: 2 }}
                >
                    {actionLabel}
                </Button>
            ))}
        </Box>
    );
};

export default EmptyState;
