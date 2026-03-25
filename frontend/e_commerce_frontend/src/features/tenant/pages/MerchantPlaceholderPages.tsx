import React, { useState } from 'react';
import {
    Box, Typography, Button, Paper, TextField, Stack,
    CircularProgress, Grid, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Dialog, DialogTitle,
    DialogContent, DialogActions, MenuItem, Tabs, Tab, Alert
} from '@mui/material';
import { Add as AddIcon, Inventory as InventoryIcon, Store as StoreIcon } from '@mui/icons-material';

import { useMerchantStore } from '../../../store/useMerchantStore';
import { useGetWarehouses, useCreateWarehouse, useAddManualStock } from '../../../query/useProductQueries';

const AddStockModal = ({ open, onClose, tenantId, productId }: { open: boolean, onClose: () => void, tenantId: number, productId: number | null }) => {
    const [warehouseId, setWarehouseId] = useState<number | ''>('');
    const [amount, setAmount] = useState<number | ''>('');

    const { data: warehouses, isLoading: isWarehousesLoading } = useGetWarehouses(tenantId);
    const { mutate: addStock, isPending } = useAddManualStock(tenantId);

    const handleSubmit = () => {
        if (!warehouseId || !amount || !productId) return;

        addStock({ warehouseId: Number(warehouseId), productId, amount: Number(amount) }, {
            onSuccess: () => {
                alert('Stok başarıyla eklendi! Olaylar fırlatıldı ve cache temizlendi.');
                setWarehouseId('');
                setAmount('');
                onClose();
            },
            onError: () => {
                alert('Stok eklenirken bir hata oluştu.');
            }
        });
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ fontWeight: 'bold' }}>Ürüne Stok Gir</DialogTitle>
            <DialogContent dividers>
                <Alert severity="info" sx={{ mb: 3 }}>
                    Stok eklemek için önce malın gireceği depoyu seçmelisiniz.
                </Alert>

                {isWarehousesLoading ? <CircularProgress /> : (
                    <Stack spacing={3} mt={1}>
                        <TextField
                            select
                            fullWidth
                            label="Depo Seçiniz"
                            value={warehouseId}
                            onChange={(e) => setWarehouseId(Number(e.target.value))}
                            required
                        >
                            {warehouses?.length === 0 && (
                                <MenuItem disabled value="">Önce bir depo oluşturmalısınız.</MenuItem>
                            )}
                            {warehouses?.map((w: any) => (
                                <MenuItem key={w.id} value={w.id}>
                                    {w.name} ({w.code})
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            type="number"
                            fullWidth
                            label="Eklenecek Miktar"
                            value={amount}
                            onChange={(e) => setAmount(Number(e.target.value))}
                            required
                            slotProps={{ htmlInput: { min: 1 } }}
                        />
                    </Stack>
                )}
            </DialogContent>
            <DialogActions sx={{ p: 2 }}>
                <Button onClick={onClose} color="inherit">İptal</Button>
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    disabled={isPending || !warehouseId || !amount}
                    startIcon={isPending ? <CircularProgress size={20} color="inherit" /> : <AddIcon />}
                >
                    {isPending ? 'Ekleniyor...' : 'Stok Ekle'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};


export const MerchantProducts: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const [tabValue, setTabValue] = useState(0);

    const [warehouseForm, setWarehouseForm] = useState({ code: '', name: '', locationDetails: '' });

    const [stockModalOpen, setStockModalOpen] = useState(false);
    const [selectedProductId, setSelectedProductId] = useState<number | null>(null);

    const { data: warehouses, isLoading: isWarehousesLoading } = useGetWarehouses(activeTenant?.id || 0);
    const { mutate: createWarehouse, isPending: isCreatingWarehouse } = useCreateWarehouse(activeTenant?.id || 0);

    if (!activeTenant) return <Typography>Mağaza bilgisi bulunamadı.</Typography>;

    const handleCreateWarehouse = (e: React.FormEvent) => {
        e.preventDefault();
        createWarehouse(warehouseForm, {
            onSuccess: () => {
                alert('Depo başarıyla oluşturuldu!');
                setWarehouseForm({ code: '', name: '', locationDetails: '' });
            }
        });
    };

    const handleOpenStockModal = (productId: number) => {
        setSelectedProductId(productId);
        setStockModalOpen(true);
    };

    return (
        <Box>
            <Typography variant="h4" fontWeight="bold" gutterBottom>
                Ürün ve Envanter Yönetimi
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={4}>
                Ürünlerinizi listeleyin, depolarınızı yönetin ve stok girişlerinizi gerçekleştirin.
            </Typography>

            <Paper sx={{ mb: 4, borderRadius: 2 }}>
                <Tabs value={tabValue} onChange={(_, val) => setTabValue(val)} indicatorColor="primary" textColor="primary" sx={{ borderBottom: 1, borderColor: 'divider' }}>
                    <Tab icon={<InventoryIcon />} iconPosition="start" label="Ürünlerim" />
                    <Tab icon={<StoreIcon />} iconPosition="start" label="Depolarım" />
                </Tabs>

                {tabValue === 0 && (
                    <Box p={3}>
                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                            <Typography variant="h6">Mağaza Ürünleri</Typography>
                            <Button variant="contained" startIcon={<AddIcon />}>Yeni Ürün Ekle</Button>
                        </Box>

                        <TableContainer component={Paper} variant="outlined">
                            <Table>
                                <TableHead sx={{ bgcolor: 'grey.50' }}>
                                    <TableRow>
                                        <TableCell><b>ID</b></TableCell>
                                        <TableCell><b>Ürün Adı</b></TableCell>
                                        <TableCell><b>Fiyat</b></TableCell>
                                        <TableCell align="center"><b>İşlemler</b></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {/* TODO: İleride bu listeyi satıcının kendi ürünlerini getiren bir endpoint ile bağlayacağız. Şimdilik mock UI. */}
                                    <TableRow>
                                        <TableCell>#101</TableCell>
                                        <TableCell>Oversize Basic Tişört</TableCell>
                                        <TableCell>299.99 TL</TableCell>
                                        <TableCell align="center">
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="secondary"
                                                onClick={() => handleOpenStockModal(101)}
                                            >
                                                Stok Gir
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>#102</TableCell>
                                        <TableCell>Kablosuz Kulaklık</TableCell>
                                        <TableCell>1499.00 TL</TableCell>
                                        <TableCell align="center">
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="secondary"
                                                onClick={() => handleOpenStockModal(102)}
                                            >
                                                Stok Gir
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Box>
                )}

                {tabValue === 1 && (
                    <Box p={3}>
                        <Typography variant="h6" mb={2}>Yeni Depo Ekle</Typography>
                        <Box component="form" onSubmit={handleCreateWarehouse} sx={{ mb: 5, p: 3, bgcolor: 'grey.50', borderRadius: 2, border: '1px dashed #ccc' }}>
                            <Grid container spacing={2} alignItems="center">
                                <Grid size={{ xs: 12, md: 3 }}>
                                    <TextField
                                        label="Depo Kodu (Örn: IST-01)" required fullWidth size="small"
                                        value={warehouseForm.code} onChange={e => setWarehouseForm({...warehouseForm, code: e.target.value})}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 3 }}>
                                    <TextField
                                        label="Depo Adı" required fullWidth size="small"
                                        value={warehouseForm.name} onChange={e => setWarehouseForm({...warehouseForm, name: e.target.value})}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 4 }}>
                                    <TextField
                                        label="Açık Adres / Lokasyon" fullWidth size="small"
                                        value={warehouseForm.locationDetails} onChange={e => setWarehouseForm({...warehouseForm, locationDetails: e.target.value})}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 2 }}>
                                    <Button type="submit" variant="contained" fullWidth disabled={isCreatingWarehouse}>
                                        {isCreatingWarehouse ? <CircularProgress size={24} color="inherit" /> : 'Kaydet'}
                                    </Button>
                                </Grid>
                            </Grid>
                        </Box>

                        <Typography variant="h6" mb={2}>Mevcut Depolarınız</Typography>
                        <TableContainer component={Paper} variant="outlined">
                            <Table>
                                <TableHead sx={{ bgcolor: 'grey.50' }}>
                                    <TableRow>
                                        <TableCell><b>Depo Kodu</b></TableCell>
                                        <TableCell><b>Depo Adı</b></TableCell>
                                        <TableCell><b>Lokasyon</b></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {isWarehousesLoading ? (
                                        <TableRow><TableCell colSpan={3} align="center"><CircularProgress size={30} /></TableCell></TableRow>
                                    ) : warehouses?.length === 0 ? (
                                        <TableRow><TableCell colSpan={3} align="center" sx={{ color: 'text.secondary', py: 3 }}>Henüz depo eklemediniz.</TableCell></TableRow>
                                    ) : (
                                        warehouses?.map((w: any) => (
                                            <TableRow key={w.id}>
                                                <TableCell>
                                                    <Typography fontWeight="bold" color="primary.main">{w.code}</Typography>
                                                </TableCell>
                                                <TableCell>{w.name}</TableCell>
                                                <TableCell>{w.locationDetails || '-'}</TableCell>
                                            </TableRow>
                                        ))
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Box>
                )}
            </Paper>

            <AddStockModal
                open={stockModalOpen}
                onClose={() => setStockModalOpen(false)}
                tenantId={activeTenant.id}
                productId={selectedProductId}
            />
        </Box>
    );
};

export const MerchantOrders = () => <Typography p={5} align="center">Sipariş Yönetimi Yakında...</Typography>;
export const MerchantReviews = () => <Typography p={5} align="center">Değerlendirmeler Yakında...</Typography>;