import React from 'react';
import { Box, Typography, Button, Paper, IconButton, Chip, Grid, Stack, Divider, Tooltip, CircularProgress } from '@mui/material';
import {
    DeleteOutline as DeleteIcon, Edit as EditIcon,
    Home as HomeIcon, Work as WorkIcon, LocationOn as LocationIcon,
    Star as StarIcon, StarBorder as StarBorderIcon
} from '@mui/icons-material';
import type { Address } from '../../types/user';
import { AddressType } from '../../types/enums';

interface AddressCardProps {
    address: Address;
    onEdit: (addr: Address) => void;
    onDelete: (id: number) => void;
    onSetDefault: (id: number) => void;
    isSettingDefault: boolean;
}

const AddressCard: React.FC<AddressCardProps> = ({ address, onEdit, onDelete, onSetDefault, isSettingDefault }) => {

    const getIcon = (label: string) => {
        const l = (label || '').toLowerCase();
        if (l.includes('iş') || l.includes('ofis')) return <WorkIcon color="action" />;
        if (l.includes('ev')) return <HomeIcon color="action" />;
        return <LocationIcon color="action" />;
    };

    return (
        <Grid size={{ xs: 12, md: 6 }}>
            <Paper
                elevation={0}
                sx={{
                    p: 3,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    borderRadius: 4,
                    border: '1px solid',
                    borderColor: address.isDefault ? 'primary.main' : 'divider',
                    backgroundColor: address.isDefault ? 'white' : 'whitesmoke',
                    position: 'relative',
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                    overflow: 'hidden',
                    '&:hover': {
                        transform: 'translateY(-4px)',
                        boxShadow: '0 12px 24px rgba(0,0,0,0.1)',
                        borderColor: 'primary.light',
                    }
                }}
            >
                {/* HEADER */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: address.isDefault ? 'white' : 'action.hover', flexShrink: 0 }}>
                        {getIcon(address.label)}
                    </Box>
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography variant="subtitle1" fontWeight="bold" lineHeight={1.2} noWrap>
                            {address.label}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {address.addressType === AddressType.SHIPPING ? 'Teslimat Adresi' : 'Fatura Adresi'}
                        </Typography>
                    </Box>
                    {address.isDefault && (
                        <Chip label="Varsayılan" size="small" color="primary" icon={<StarIcon sx={{ "&&": { fontSize: 14 } }} />} sx={{ fontWeight: 'bold', flexShrink: 0 }} />
                    )}
                </Box>

                <Divider sx={{ mb: 2, borderStyle: 'dashed' }} />

                {/* CONTENT */}
                <Stack spacing={0.5} sx={{ color: 'text.secondary', mb: 2, flexGrow: 1 }}>
                    <Typography variant="body2" fontWeight="600" color="text.primary">
                        {address.recipientName}
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            wordBreak: 'break-word',
                            whiteSpace: 'pre-wrap'
                        }}
                    >
                        {address.line1}
                    </Typography>

                    {address.line2 && (
                        <Typography
                            variant="body2"
                            sx={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}
                        >
                            {address.line2}
                        </Typography>
                    )}

                    <Typography variant="body2" sx={{ mt: 1 }}>
                        {address.stateProvince} / {address.city}
                    </Typography>
                    <Typography variant="caption" sx={{ pt: 1, display: 'block' }}>
                        📞 {address.phoneNumber}
                    </Typography>
                </Stack>

                {/* FOOTER ACTIONS */}
                <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 'auto' }}>
                    {!address.isDefault ? (
                        <Button
                            size="small"
                            color="primary"
                            startIcon={isSettingDefault ? <CircularProgress size={12} /> : <StarBorderIcon />}
                            onClick={() => onSetDefault(address.id)}
                            disabled={isSettingDefault}
                            sx={{ textTransform: 'none', borderRadius: 2 }}
                        >
                            Varsayılan Yap
                        </Button>
                    ) : <Box />}

                    <Stack direction="row" spacing={1}>
                        <Tooltip title="Düzenle">
                            <IconButton size="small" onClick={() => onEdit(address)} sx={{ border: '1px solid', borderColor: 'divider' }}>
                                <EditIcon fontSize="small" />
                            </IconButton>
                        </Tooltip>
                        <Tooltip title="Sil">
                            <IconButton size="small" color="error" onClick={() => onDelete(address.id)} sx={{ border: '1px solid', borderColor: 'divider' }}>
                                <DeleteIcon fontSize="small" />
                            </IconButton>
                        </Tooltip>
                    </Stack>
                </Stack>
            </Paper>
        </Grid>
    );
};

export default AddressCard;