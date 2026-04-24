import React, { useState } from 'react';
import {
    Box, Typography, Button, Paper, TextField, Stack,
    CircularProgress, Grid, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Dialog, DialogTitle,
    DialogContent, DialogActions, MenuItem, Alert, Chip,
    InputAdornment,
} from '@mui/material';
import { Add as AddIcon, Search as SearchIcon } from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import {
    useGetWarehouses,
    useCreateWarehouse,
    useAddManualStock,
    useGetTenantProducts,
} from '../../../query/useProductQueries';
import { useNotification } from '../../../components/shared/NotificationProvider';

// ─── Add Stock Dialog ─────────────────────────────────────────────────────────

interface AddStockDialogProps {
    open: boolean;
    onClose: () => void;
    tenantId: number;
    /** Dışarıdan geçirilirse ürün seçimi kilitlenir */
    preselectedProductId?: number | null;
}

const AddStockDialog: React.FC<AddStockDialogProps> = ({
                                                           open,
                                                           onClose,
                                                           tenantId,
                                                           preselectedProductId,
                                                       }) => {
    const { notify } = useNotification();

    const [warehouseId, setWarehouseId] = useState<number | ''>('');
    const [productId,   setProductId]   = useState<number | ''>(preselectedProductId ?? '');
    const [amount,      setAmount]      = useState<number | ''>('');
    const [search,      setSearch]      = useState('');

    const { data: warehouses,   isLoading: loadingWarehouses } = useGetWarehouses(tenantId);
    const { data: productsPage, isLoading: loadingProducts }   = useGetTenantProducts(tenantId, 0, 200);
    const { mutate: addStock,   isPending }                     = useAddManualStock(tenantId);

    const products = productsPage?.content ?? [];
    const filtered = search.trim()
        ? products.filter((p) =>
            p.name.toLowerCase().includes(search.toLowerCase()) ||
            p.sku.toLowerCase().includes(search.toLowerCase()),
        )
        : products;

    const isValid = !!warehouseId && !!productId && !!amount && Number(amount) > 0;

    const handleClose = () => {
        setWarehouseId('');
        setProductId(preselectedProductId ?? '');
        setAmount('');
        setSearch('');
        onClose();
    };

    const handleSubmit = () => {
        if (!isValid) return;
        addStock(
            { warehouseId: Number(warehouseId), productId: Number(productId), amount: Number(amount) },
            {
                onSuccess: () => {
                    notify('Stok başarıyla eklendi.', 'success');
                    handleClose();
                },
                onError: () => notify('Stok eklenirken hata oluştu.', 'error'),
            },
        );
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
            <DialogTitle fontWeight="bold">Stok Girişi</DialogTitle>

            <DialogContent dividers>
                <Stack spacing={3} sx={{ pt: 0.5 }}>

                    {/* Ürün seçimi */}
                    {preselectedProductId ? (
                        <TextField
                            label="Ürün"
                            disabled
                            value={
                                products.find((p) => p.id === preselectedProductId)?.name ??
                                `#${preselectedProductId}`
                            }
                            fullWidth
                        />
                    ) : (
                        <>
                            <TextField
                                label="Ürün Ara"
                                size="small"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                slotProps={{
                                    input: {
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <SearchIcon fontSize="small" />
                                            </InputAdornment>
                                        ),
                                    },
                                }}
                            />
                            <TextField
                                select fullWidth required
                                label="Ürün Seçin"
                                value={productId}
                                onChange={(e) => setProductId(Number(e.target.value))}
                                disabled={loadingProducts}
                            >
                                {loadingProducts && (
                                    <MenuItem disabled>Ürünler yükleniyor...</MenuItem>
                                )}
                                {!loadingProducts && filtered.length === 0 && (
                                    <MenuItem disabled>Ürün bulunamadı</MenuItem>
                                )}
                                {filtered.map((p) => (
                                    <MenuItem key={p.id} value={p.id}>
                                        <Stack direction="row" spacing={1} alignItems="center">
                                            <Typography variant="body2" fontWeight={500}>
                                                {p.name}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary" fontFamily="monospace">
                                                {p.sku}
                                            </Typography>
                                        </Stack>
                                    </MenuItem>
                                ))}
                            </TextField>
                        </>
                    )}

                    {/* Depo seçimi */}
                    <TextField
                        select fullWidth required
                        label="Depo Seçin"
                        value={warehouseId}
                        onChange={(e) => setWarehouseId(Number(e.target.value))}
                        disabled={loadingWarehouses}
                    >
                        {loadingWarehouses && (
                            <MenuItem disabled>Depolar yükleniyor...</MenuItem>
                        )}
                        {!loadingWarehouses && (!warehouses || warehouses.length === 0) && (
                            <MenuItem disabled>Önce bir depo oluşturmalısınız</MenuItem>
                        )}
                        {warehouses?.map((w: any) => (
                            <MenuItem key={w.id} value={w.id}>
                                {w.name}
                                <Typography
                                    component="span"
                                    variant="caption"
                                    color="text.secondary"
                                    sx={{ ml: 1, fontFamily: 'monospace' }}
                                >
                                    ({w.code})
                                </Typography>
                            </MenuItem>
                        ))}
                    </TextField>

                    {/* Miktar */}
                    <TextField
                        type="number" fullWidth required
                        label="Eklenecek Miktar (adet)"
                        value={amount}
                        onChange={(e) => setAmount(Number(e.target.value))}
                        slotProps={{ htmlInput: { min: 1 } }}
                        helperText="Mevcut stoka eklenir"
                    />
                </Stack>
            </DialogContent>

            <DialogActions sx={{ px: 3, pb: 2 }}>
                <Button onClick={handleClose} color="inherit" disabled={isPending}>
                    İptal
                </Button>
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    disabled={!isValid || isPending}
                    startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : <AddIcon />}
                >
                    {isPending ? 'Ekleniyor...' : 'Stok Ekle'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

// ─── Page ─────────────────────────────────────────────────────────────────────

const MerchantWarehousePage: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const { notify }       = useNotification();

    const [stockDialogOpen,      setStockDialogOpen]      = useState(false);
    const [warehouseForm,        setWarehouseForm]         = useState({ code: '', name: '', locationDetails: '' });

    // Hooks — Rules of Hooks: koşulsuz çağrılmalı
    const tenantId = activeTenant?.id ?? 0;

    const { data: warehouses, isLoading: loadingWarehouses } = useGetWarehouses(tenantId);
    const { mutate: createWarehouse, isPending: isCreating } = useCreateWarehouse(tenantId);

    if (!activeTenant) {
        return <Alert severity="warning">Aktif mağaza bulunamadı.</Alert>;
    }

    const handleCreateWarehouse = (e: React.FormEvent) => {
        e.preventDefault();
        if (!warehouseForm.code.trim() || !warehouseForm.name.trim()) return;
        createWarehouse(warehouseForm, {
            onSuccess: () => {
                notify('Depo başarıyla oluşturuldu!', 'success');
                setWarehouseForm({ code: '', name: '', locationDetails: '' });
            },
            onError: () => notify('Depo oluşturulurken hata oluştu.', 'error'),
        });
    };

    return (
        <Box>
            {/* Başlık */}
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={4}>
                <Box>
                    <Typography variant="h4" fontWeight="bold">Depo & Stok Yönetimi</Typography>
                    <Typography variant="body2" color="text.secondary">
                        Depolarınızı yönetin ve ürünlerinize stok girişi yapın.
                    </Typography>
                </Box>
                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={() => setStockDialogOpen(true)}
                >
                    Stok Girişi Yap
                </Button>
            </Stack>

            {/* Depo oluşturma formu */}
            <Paper variant="outlined" sx={{ p: 3, borderRadius: 3, mb: 4 }}>
                <Typography variant="h6" mb={2}>Yeni Depo Ekle</Typography>
                <Box
                    component="form"
                    onSubmit={handleCreateWarehouse}
                    sx={{ bgcolor: 'grey.50', borderRadius: 2, p: 3, border: '1px dashed #ccc' }}
                >
                    <Grid container spacing={2} alignItems="center">
                        <Grid size={{ xs: 12, md: 3 }}>
                            <TextField
                                label="Depo Kodu" required fullWidth size="small"
                                placeholder="Örn: IST-01"
                                value={warehouseForm.code}
                                onChange={(e) =>
                                    setWarehouseForm((p) => ({ ...p, code: e.target.value }))
                                }
                            />
                        </Grid>
                        <Grid size={{ xs: 12, md: 3 }}>
                            <TextField
                                label="Depo Adı" required fullWidth size="small"
                                value={warehouseForm.name}
                                onChange={(e) =>
                                    setWarehouseForm((p) => ({ ...p, name: e.target.value }))
                                }
                            />
                        </Grid>
                        <Grid size={{ xs: 12, md: 4 }}>
                            <TextField
                                label="Lokasyon / Adres" fullWidth size="small"
                                value={warehouseForm.locationDetails}
                                onChange={(e) =>
                                    setWarehouseForm((p) => ({
                                        ...p,
                                        locationDetails: e.target.value,
                                    }))
                                }
                            />
                        </Grid>
                        <Grid size={{ xs: 12, md: 2 }}>
                            <Button
                                type="submit" variant="contained" fullWidth
                                disabled={isCreating}
                            >
                                {isCreating
                                    ? <CircularProgress size={22} color="inherit" />
                                    : 'Kaydet'}
                            </Button>
                        </Grid>
                    </Grid>
                </Box>
            </Paper>

            {/* Depo listesi */}
            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                <Box sx={{ p: 3, borderBottom: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="h6">Mevcut Depolar</Typography>
                </Box>
                <TableContainer>
                    <Table>
                        <TableHead sx={{ bgcolor: 'grey.50' }}>
                            <TableRow>
                                <TableCell><b>Depo Kodu</b></TableCell>
                                <TableCell><b>Depo Adı</b></TableCell>
                                <TableCell><b>Lokasyon</b></TableCell>
                                <TableCell align="center"><b>İşlem</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {loadingWarehouses ? (
                                <TableRow>
                                    <TableCell colSpan={4} align="center" sx={{ py: 4 }}>
                                        <CircularProgress size={28} />
                                    </TableCell>
                                </TableRow>
                            ) : !warehouses || warehouses.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary', py: 4 }}>
                                        Henüz depo eklemediniz.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                warehouses.map((w: any) => (
                                    <TableRow key={w.id} hover sx={{ '&:last-child td': { border: 0 } }}>
                                        <TableCell>
                                            <Chip label={w.code} size="small" color="primary" variant="outlined" />
                                        </TableCell>
                                        <TableCell>{w.name}</TableCell>
                                        <TableCell>{w.locationDetails || '—'}</TableCell>
                                        <TableCell align="center">
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                startIcon={<AddIcon />}
                                                onClick={() => setStockDialogOpen(true)}
                                            >
                                                Stok Gir
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            {/* Stok giriş dialog */}
            <AddStockDialog
                open={stockDialogOpen}
                onClose={() => setStockDialogOpen(false)}
                tenantId={tenantId}
            />
        </Box>
    );
};

export default MerchantWarehousePage;