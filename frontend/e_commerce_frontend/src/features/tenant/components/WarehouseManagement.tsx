import React, { useState } from 'react';
import { Box, Button, TextField, Typography, Paper, Stack, CircularProgress } from '@mui/material';
import { useCreateWarehouse, useGetWarehouses } from '../../../query/useProductQueries';

export const WarehouseManagement = ({ tenantId }: { tenantId: number }) => {
    const { data: warehouses, isLoading } = useGetWarehouses(tenantId);
    const { mutate: createWarehouse, isPending } = useCreateWarehouse(tenantId);

    const [formData, setFormData] = useState({ code: '', name: '', locationDetails: '' });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        createWarehouse(formData, {
            onSuccess: () => {
                setFormData({ code: '', name: '', locationDetails: '' });
            }
        });
    };

    return (
        <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" mb={2}>Depo Yönetimi</Typography>
            <Box component="form" onSubmit={handleSubmit} sx={{ mb: 4 }}>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                    <TextField
                        label="Depo Kodu"
                        required
                        value={formData.code}
                        onChange={(e) => setFormData({...formData, code: e.target.value})}
                    />
                    <TextField
                        label="Depo Adı"
                        required
                        value={formData.name}
                        onChange={(e) => setFormData({...formData, name: e.target.value})}
                    />
                    <TextField
                        label="Lokasyon"
                        fullWidth
                        value={formData.locationDetails}
                        onChange={(e) => setFormData({...formData, locationDetails: e.target.value})}
                    />
                    <Button
                        type="submit"
                        variant="contained"
                        disabled={isPending}
                    >
                        {isPending ? <CircularProgress size={24} /> : 'Depo Ekle'}
                    </Button>
                </Stack>
            </Box>

            <Typography variant="subtitle2" color="textSecondary">Mevcut Depolarınız:</Typography>
            {isLoading ? <CircularProgress size={20} sx={{ mt: 1 }} /> : (
                <ul>
                    {warehouses?.map((w: any) => (
                        <li key={w.id}>{w.code} - {w.name} ({w.locationDetails})</li>
                    ))}
                </ul>
            )}
        </Paper>
    );
};