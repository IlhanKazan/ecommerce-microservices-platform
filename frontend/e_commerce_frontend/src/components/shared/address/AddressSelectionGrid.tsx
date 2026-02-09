import React from 'react';
import { Card, CardContent, Typography, Stack, Radio, Chip, Grid } from '@mui/material';
import type { Address } from '../../../types/user';

interface AddressSelectionGridProps {
    addresses: Address[];
    selectedId: number | null | '';
    onSelect: (id: number) => void;
}

const AddressSelectionGrid: React.FC<AddressSelectionGridProps> = ({ addresses, selectedId, onSelect }) => {
    return (
        <Grid container spacing={2}>
            {addresses.map((addr) => {
                const isSelected = selectedId === addr.id;
                return (
                    <Grid size={{ xs: 12, md: 6 }} key={addr.id}>
                        <Card
                            variant="outlined"
                            sx={{
                                cursor: 'pointer',
                                transition: 'all 0.2s',
                                borderColor: isSelected ? 'primary.main' : 'divider',
                                bgcolor: isSelected ? 'primary.50' : 'background.paper',
                                borderWidth: isSelected ? 2 : 1,
                                position: 'relative'
                            }}
                            onClick={() => onSelect(addr.id)}
                        >
                            <CardContent sx={{ pb: 2 }}>
                                <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
                                    <Stack direction="row" alignItems="center" gap={1}>
                                        <Radio checked={isSelected} size="small" sx={{ p: 0 }} />
                                        <Typography fontWeight="bold">{addr.label}</Typography>
                                    </Stack>
                                    <Chip label={addr.addressType} size="small" variant="outlined" sx={{ fontSize: '0.7rem' }} />
                                </Stack>

                                <Typography variant="body2" color="text.secondary" sx={{ ml: 3.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                                    {addr.line1} {addr.line2}
                                </Typography>
                                <Typography variant="caption" sx={{ ml: 3.5, display: 'block', mt: 0.5, fontWeight: 500 }}>
                                    {addr.city} / {addr.stateProvince}
                                </Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                );
            })}
        </Grid>
    );
};

export default AddressSelectionGrid;