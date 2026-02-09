import React from 'react';
import { Card, CardContent, Typography, Stack, Grid, Box, Avatar, IconButton, Tooltip } from '@mui/material';
import {
    LocationOn as LocationIcon,
    Business as BusinessIcon,
    LocalShipping as ShippingIcon,
    DeleteOutline as DeleteIcon
} from '@mui/icons-material';
import { AddressType } from '../../../types/enums';

interface TenantAddressCardProps {
    address: {
        label?: string | null;
        recipientName: string;
        phoneNumber?: string | null;
        line1?: string;
        line2?: string | null;
        city: string;
        country: string;
        zipCode?: string | null;
        type?: string;
        addressType?: string;
    };
    onDelete?: () => void;
}

const TenantAddressCard: React.FC<TenantAddressCardProps> = ({ address, onDelete }) => {

    const displayTitle = address.label || 'Adres Başlığı Yok';
    const displayType = (address.type || address.addressType || 'GENEL') as string;
    const displayAddress1 = `${address.line1 || ''}`;
    const displayAddress2 = `${address.line2 || ''}`;
    const isBilling = displayType === AddressType.BILLING || displayType === 'FATURA';

    return (
        <Card
            elevation={0}
            variant="outlined"
            sx={{
                borderRadius: 4,
                border: '1px solid #e2e8f0',
                position: 'relative',
                overflow: 'hidden',
                '&:hover .delete-btn': { opacity: 1 }
            }}
        >
            <Box
                sx={{
                    position: 'absolute', left: 0, top: 0, bottom: 0, width: 6,
                    bgcolor: isBilling ? 'secondary.main' : 'primary.main'
                }}
            />

            {onDelete && (
                <Tooltip title="Adresi Sil ve Değiştir">
                    <IconButton
                        className="delete-btn"
                        onClick={onDelete}
                        color="error"
                        sx={{
                            position: 'absolute', top: 8, right: 8,
                            opacity: 0.6, transition: '0.2s',
                            bgcolor: 'background.paper',
                            '&:hover': { opacity: 1, bgcolor: '#fee2e2' }
                        }}
                    >
                        <DeleteIcon />
                    </IconButton>
                </Tooltip>
            )}

            <CardContent sx={{ pl: 3, pr: 5 }}>
                <Grid container spacing={2}>
                    <Grid size={12}>
                        <Stack direction="row" gap={2} alignItems="center">
                            <Avatar
                                sx={{
                                    bgcolor: isBilling ? 'secondary.50' : 'primary.50',
                                    color: isBilling ? 'secondary.main' : 'primary.main',
                                    width: 48, height: 48
                                }}
                            >
                                {isBilling ? <BusinessIcon /> : <ShippingIcon />}
                            </Avatar>
                            <Box>
                                <Typography variant="h6" fontWeight="bold" lineHeight={1.2}>
                                    {displayTitle}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" fontWeight="500">
                                    {address.recipientName}
                                </Typography>
                            </Box>
                        </Stack>
                    </Grid>

                    <Grid size={12}><Box sx={{ borderBottom: '1px dashed #e2e8f0' }} /></Grid>

                    <Grid size={12}>
                        <Stack direction="row" gap={1} mb={1}>
                            <LocationIcon color="action" fontSize="small" sx={{ mt: 0.3 }} />
                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                {displayAddress1}
                            </Typography>
                        </Stack>
                        <Stack direction="row" gap={1} mb={1}>
                            <LocationIcon color="action" fontSize="small" sx={{ mt: 0.3 }} />
                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                {displayAddress2}
                            </Typography>
                        </Stack>
                        <Typography variant="body2" fontWeight="600" color="text.primary" sx={{ ml: 3.5 }}>
                            {address.zipCode ? `${address.zipCode} - ` : ''} {address.city} / {address.country}
                        </Typography>
                    </Grid>
                </Grid>
            </CardContent>
        </Card>
    );
};

export default TenantAddressCard;