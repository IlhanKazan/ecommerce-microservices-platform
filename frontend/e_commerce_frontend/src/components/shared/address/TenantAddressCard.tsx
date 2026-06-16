import React from 'react';
import { Card, CardContent, Typography, Stack, Box, Avatar, IconButton, Tooltip, Chip } from '@mui/material';
import {
    LocationOn as LocationIcon,
    Business as BusinessIcon,
    LocalShipping as ShippingIcon,
    PersonOutline as PersonIcon,
    DeleteOutline as DeleteIcon,
} from '@mui/icons-material';

interface TenantAddressCardProps {
    address: {
        label?: string | null;
        recipientName?: string | null;
        phoneNumber?: string | null;
        line1?: string | null;
        line2?: string | null;
        city?: string | null;
        country?: string | null;
        zipCode?: string | null;
        type?: string | null;
        addressType?: string | null;
    };
    onDelete?: () => void;
}

const TYPE_TITLE: Record<string, string> = {
    BILLING: 'Fatura Adresi',
    FATURA: 'Fatura Adresi',
    SHIPPING: 'Teslimat Adresi',
    WAREHOUSE: 'Depo Adresi',
    REGISTERED: 'Kayıtlı Adres',
};

const clean = (s?: string | null) => (s ?? '').trim();

const TenantAddressCard: React.FC<TenantAddressCardProps> = ({ address, onDelete }) => {
    const rawType = clean(address.type || address.addressType).toUpperCase();
    const isBilling = rawType === 'BILLING' || rawType === 'FATURA';
    const typeLabel = TYPE_TITLE[rawType] || 'Mağaza Adresi';

    // Başlık: kullanıcı bir etiket girdiyse onu, yoksa adres tipinden anlamlı bir başlık kullan.
    const title = clean(address.label) || typeLabel;
    const recipient = clean(address.recipientName);
    const addressLines = [clean(address.line1), clean(address.line2)].filter(Boolean);
    const locationLine = [clean(address.zipCode), clean(address.city), clean(address.country)]
        .filter(Boolean)
        .join(' · ');

    return (
        <Card
            elevation={0}
            variant="outlined"
            sx={{
                borderRadius: 4,
                border: '1px solid #e2e8f0',
                position: 'relative',
                overflow: 'hidden',
                '&:hover .delete-btn': { opacity: 1 },
            }}
        >
            <Box
                sx={{
                    position: 'absolute', left: 0, top: 0, bottom: 0, width: 6,
                    bgcolor: isBilling ? 'secondary.main' : 'primary.main',
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
                            '&:hover': { opacity: 1, bgcolor: '#fee2e2' },
                        }}
                    >
                        <DeleteIcon />
                    </IconButton>
                </Tooltip>
            )}

            <CardContent sx={{ pl: 3, pr: 6 }}>
                {/* Başlık satırı */}
                <Stack direction="row" gap={2} alignItems="center" sx={{ mb: 2 }}>
                    <Avatar
                        sx={{
                            bgcolor: isBilling ? 'secondary.50' : 'primary.50',
                            color: isBilling ? 'secondary.main' : 'primary.main',
                            width: 48, height: 48,
                        }}
                    >
                        {isBilling ? <BusinessIcon /> : <ShippingIcon />}
                    </Avatar>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                        <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
                            <Typography variant="h6" fontWeight="bold" lineHeight={1.2}>
                                {title}
                            </Typography>
                            <Chip size="small" label={typeLabel} variant="outlined"
                                  color={isBilling ? 'secondary' : 'primary'} sx={{ height: 22 }} />
                        </Stack>
                        {recipient && (
                            <Stack direction="row" gap={0.5} alignItems="center" sx={{ mt: 0.25 }}>
                                <PersonIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                                <Typography variant="caption" color="text.secondary" fontWeight={500}>
                                    {recipient}
                                </Typography>
                            </Stack>
                        )}
                    </Box>
                </Stack>

                <Box sx={{ borderBottom: '1px dashed #e2e8f0', mb: 2 }} />

                {/* Adres detayları */}
                {addressLines.length === 0 && !locationLine ? (
                    <Typography variant="body2" color="text.disabled" sx={{ fontStyle: 'italic' }}>
                        Adres detayı girilmemiş.
                    </Typography>
                ) : (
                    <Stack direction="row" gap={1}>
                        <LocationIcon color="action" fontSize="small" sx={{ mt: 0.3 }} />
                        <Box>
                            {addressLines.map((line, i) => (
                                <Typography key={i} variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                                    {line}
                                </Typography>
                            ))}
                            {locationLine && (
                                <Typography variant="body2" fontWeight={600} color="text.primary" sx={{ mt: 0.5 }}>
                                    {locationLine}
                                </Typography>
                            )}
                        </Box>
                    </Stack>
                )}
            </CardContent>
        </Card>
    );
};

export default TenantAddressCard;
