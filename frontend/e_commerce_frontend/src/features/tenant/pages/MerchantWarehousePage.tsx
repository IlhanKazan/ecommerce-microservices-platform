import React, { useState } from 'react';
import {
    Box, Typography, Button, Paper, TextField, Stack,
    CircularProgress, Grid, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Dialog, DialogTitle,
    DialogContent, DialogActions, MenuItem, Alert, Chip,
    InputAdornment, Collapse, IconButton, Tooltip,
} from '@mui/material';
import {
    Add as AddIcon,
    Search as SearchIcon,
    KeyboardArrowDown as ExpandIcon,
    KeyboardArrowUp as CollapseIcon,
    Remove as RemoveIcon,
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import {
    useGetWarehouses,
    useCreateWarehouse,
    useAddManualStock,
    useRemoveManualStock,
    useGetTenantProducts,
    useGetTenantStocks,
} from '../../../query/useProductQueries';
import { useNotification } from '../../../components/shared/NotificationContext';
import type { StockSummaryItem } from '../../../types/product';

// ─── Add Stock Dialog ─────────────────────────────────────────────────────────

interface AddStockDialogProps {
    open: boolean;
    onClose: () => void;
    tenantId: number;
    preselectedProductId?: number | null;
    preselectedWarehouseId?: number | null;
}

const AddStockDialog: React.FC<AddStockDialogProps> = ({
    open,
    onClose,
    tenantId,
    preselectedProductId,
    preselectedWarehouseId,
}) => {
    const { notify } = useNotification();

    const [warehouseId, setWarehouseId] = useState<number | ''>(preselectedWarehouseId ?? '');
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
        setWarehouseId(preselectedWarehouseId ?? '');
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
                    {preselectedWarehouseId ? (
                        <TextField
                            label="Depo"
                            disabled
                            value={
                                warehouses?.find((w) => w.id === preselectedWarehouseId)?.name ??
                                `#${preselectedWarehouseId}`
                            }
                            fullWidth
                        />
                    ) : (
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
                            {warehouses?.map((w) => (
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
                    )}

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

// ─── Remove Stock Dialog ──────────────────────────────────────────────────────

interface RemoveStockDialogProps {
    open: boolean;
    onClose: () => void;
    tenantId: number;
    warehouseId: number;
    productId: number;
    currentStock: number;
    productSku: string;
    warehouseName: string;
}

const RemoveStockDialog: React.FC<RemoveStockDialogProps> = ({
    open,
    onClose,
    tenantId,
    warehouseId,
    productId,
    currentStock,
    productSku,
    warehouseName,
}) => {
    const { notify } = useNotification();
    const [amount, setAmount] = useState<number | ''>('');
    const { mutate: removeStock, isPending } = useRemoveManualStock(tenantId);

    const isValid = !!amount && Number(amount) > 0 && Number(amount) <= currentStock;

    const handleClose = () => {
        setAmount('');
        onClose();
    };

    const handleSubmit = () => {
        if (!isValid) return;
        removeStock(
            { warehouseId, productId, amount: Number(amount) },
            {
                onSuccess: () => {
                    notify('Stok başarıyla düşüldü.', 'success');
                    handleClose();
                },
                onError: () => notify('Stok düşürülürken hata oluştu.', 'error'),
            },
        );
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
            <DialogTitle fontWeight="bold">Stok Düşür</DialogTitle>

            <DialogContent dividers>
                <Stack spacing={2} sx={{ pt: 0.5 }}>
                    <Stack direction="row" spacing={1} flexWrap="wrap">
                        <Chip label={productSku} size="small" variant="outlined" sx={{ fontFamily: 'monospace' }} />
                        <Chip label={warehouseName} size="small" color="primary" variant="outlined" />
                        <Chip
                            label={`Mevcut: ${currentStock} adet`}
                            size="small"
                            color={currentStock === 0 ? 'error' : currentStock <= 5 ? 'warning' : 'success'}
                        />
                    </Stack>

                    <TextField
                        type="number" fullWidth required autoFocus
                        label="Düşülecek Miktar (adet)"
                        value={amount}
                        onChange={(e) => setAmount(Number(e.target.value))}
                        slotProps={{ htmlInput: { min: 1, max: currentStock } }}
                        helperText={`En fazla ${currentStock} adet düşürebilirsiniz`}
                        error={!!amount && (Number(amount) <= 0 || Number(amount) > currentStock)}
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
                    color="error"
                    disabled={!isValid || isPending}
                    startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : <RemoveIcon />}
                >
                    {isPending ? 'Düşürülüyor...' : 'Stok Düş'}
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
    const [addStockWarehouseId,  setAddStockWarehouseId]  = useState<number | null>(null);
    const [addStockProductId,    setAddStockProductId]    = useState<number | null>(null);
    const [removeTarget,         setRemoveTarget]         = useState<{
        warehouseId: number;
        productId: number;
        currentStock: number;
        sku: string;
        warehouseName: string;
    } | null>(null);
    const [expandedWarehouseId,  setExpandedWarehouseId]  = useState<number | null>(null);
    const [warehouseForm,        setWarehouseForm]         = useState({ code: '', name: '', locationDetails: '' });

    const tenantId = activeTenant?.id ?? 0;

    const { data: warehouses,  isLoading: loadingWarehouses } = useGetWarehouses(tenantId);
    const { data: stockSummary                               } = useGetTenantStocks(tenantId);
    const { mutate: createWarehouse, isPending: isCreating }   = useCreateWarehouse(tenantId);

    // warehouseId → stok kayıtları
    const stockByWarehouse = new Map<number, StockSummaryItem[]>();
    stockSummary?.forEach((s) => {
        const list = stockByWarehouse.get(s.warehouseId) ?? [];
        list.push(s);
        stockByWarehouse.set(s.warehouseId, list);
    });

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

    const openAddStockDialog = (warehouseId?: number | null, productId?: number | null) => {
        setAddStockWarehouseId(warehouseId ?? null);
        setAddStockProductId(productId ?? null);
        setStockDialogOpen(true);
    };

    const closeAddStockDialog = () => {
        setStockDialogOpen(false);
        setAddStockWarehouseId(null);
        setAddStockProductId(null);
    };

    const toggleExpand = (warehouseId: number) => {
        setExpandedWarehouseId((prev) => (prev === warehouseId ? null : warehouseId));
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
                    onClick={() => openAddStockDialog()}
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
                                <TableCell padding="checkbox" />
                                <TableCell><b>Depo Kodu</b></TableCell>
                                <TableCell><b>Depo Adı</b></TableCell>
                                <TableCell><b>Lokasyon</b></TableCell>
                                <TableCell align="center"><b>Stok Kalemleri</b></TableCell>
                                <TableCell align="center"><b>İşlem</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {loadingWarehouses ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                                        <CircularProgress size={28} />
                                    </TableCell>
                                </TableRow>
                            ) : !warehouses || warehouses.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center" sx={{ color: 'text.secondary', py: 4 }}>
                                        Henüz depo eklemediniz.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                warehouses.map((w) => {
                                    const warehouseStocks = stockByWarehouse.get(w.id) ?? [];
                                    const isExpanded = expandedWarehouseId === w.id;

                                    return (
                                        <React.Fragment key={w.id}>
                                            <TableRow
                                                hover
                                                sx={{
                                                    '&:last-child td': { border: 0 },
                                                    cursor: 'pointer',
                                                    bgcolor: isExpanded ? 'action.selected' : undefined,
                                                }}
                                                onClick={() => toggleExpand(w.id)}
                                            >
                                                <TableCell padding="checkbox">
                                                    <IconButton size="small">
                                                        {isExpanded ? <CollapseIcon /> : <ExpandIcon />}
                                                    </IconButton>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip label={w.code} size="small" color="primary" variant="outlined" />
                                                </TableCell>
                                                <TableCell>{w.name}</TableCell>
                                                <TableCell>{w.locationDetails || '—'}</TableCell>
                                                <TableCell align="center">
                                                    <Chip
                                                        label={`${warehouseStocks.length} ürün`}
                                                        size="small"
                                                        color={warehouseStocks.length === 0 ? 'default' : 'info'}
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell align="center" onClick={(e) => e.stopPropagation()}>
                                                    <Button
                                                        size="small"
                                                        variant="outlined"
                                                        startIcon={<AddIcon />}
                                                        onClick={() => openAddStockDialog(w.id)}
                                                    >
                                                        Stok Gir
                                                    </Button>
                                                </TableCell>
                                            </TableRow>

                                            {/* Expandable stok detay tablosu */}
                                            <TableRow>
                                                <TableCell colSpan={6} sx={{ py: 0, border: 0 }}>
                                                    <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                                                        <Box sx={{ bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
                                                            {warehouseStocks.length === 0 ? (
                                                                <Typography
                                                                    variant="body2"
                                                                    color="text.secondary"
                                                                    sx={{ py: 2, px: 4 }}
                                                                >
                                                                    Bu depoda henüz stok kaydı yok. "Stok Gir" butonuyla başlayabilirsiniz.
                                                                </Typography>
                                                            ) : (
                                                                <Table size="small">
                                                                    <TableHead>
                                                                        <TableRow sx={{ bgcolor: 'grey.100' }}>
                                                                            <TableCell sx={{ pl: 4 }}><b>SKU</b></TableCell>
                                                                            <TableCell align="center"><b>Mevcut</b></TableCell>
                                                                            <TableCell align="center"><b>Rezerve</b></TableCell>
                                                                            <TableCell align="center"><b>İşlemler</b></TableCell>
                                                                        </TableRow>
                                                                    </TableHead>
                                                                    <TableBody>
                                                                        {warehouseStocks.map((s) => (
                                                                            <TableRow
                                                                                key={`${s.warehouseId}-${s.productId}`}
                                                                                sx={{ '&:last-child td': { border: 0 } }}
                                                                            >
                                                                                <TableCell sx={{ pl: 4 }}>
                                                                                    <Typography variant="caption" fontFamily="monospace">
                                                                                        {s.sku}
                                                                                    </Typography>
                                                                                </TableCell>
                                                                                <TableCell align="center">
                                                                                    <Chip
                                                                                        label={s.availableQuantity}
                                                                                        size="small"
                                                                                        color={
                                                                                            s.availableQuantity === 0
                                                                                                ? 'error'
                                                                                                : s.availableQuantity <= 5
                                                                                                    ? 'warning'
                                                                                                    : 'success'
                                                                                        }
                                                                                        variant="outlined"
                                                                                    />
                                                                                </TableCell>
                                                                                <TableCell align="center">
                                                                                    <Chip
                                                                                        label={s.reservedQuantity}
                                                                                        size="small"
                                                                                        color={s.reservedQuantity > 0 ? 'warning' : 'default'}
                                                                                        variant="outlined"
                                                                                    />
                                                                                </TableCell>
                                                                                <TableCell align="center">
                                                                                    <Stack direction="row" spacing={0.5} justifyContent="center">
                                                                                        <Tooltip title="Stok Ekle">
                                                                                            <IconButton
                                                                                                size="small"
                                                                                                color="success"
                                                                                                onClick={() => openAddStockDialog(s.warehouseId, s.productId)}
                                                                                            >
                                                                                                <AddIcon fontSize="small" />
                                                                                            </IconButton>
                                                                                        </Tooltip>
                                                                                        <Tooltip title="Stok Düş">
                                                                                            <span>
                                                                                                <IconButton
                                                                                                    size="small"
                                                                                                    color="error"
                                                                                                    disabled={s.availableQuantity === 0}
                                                                                                    onClick={() =>
                                                                                                        setRemoveTarget({
                                                                                                            warehouseId: s.warehouseId,
                                                                                                            productId: s.productId,
                                                                                                            currentStock: s.availableQuantity,
                                                                                                            sku: s.sku,
                                                                                                            warehouseName: s.warehouseName,
                                                                                                        })
                                                                                                    }
                                                                                                >
                                                                                                    <RemoveIcon fontSize="small" />
                                                                                                </IconButton>
                                                                                            </span>
                                                                                        </Tooltip>
                                                                                    </Stack>
                                                                                </TableCell>
                                                                            </TableRow>
                                                                        ))}
                                                                    </TableBody>
                                                                </Table>
                                                            )}
                                                        </Box>
                                                    </Collapse>
                                                </TableCell>
                                            </TableRow>
                                        </React.Fragment>
                                    );
                                })
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>

            {/* Stok giriş dialog */}
            <AddStockDialog
                open={stockDialogOpen}
                onClose={closeAddStockDialog}
                tenantId={tenantId}
                preselectedWarehouseId={addStockWarehouseId}
                preselectedProductId={addStockProductId}
            />

            {/* Stok düşürme dialog */}
            {removeTarget && (
                <RemoveStockDialog
                    open={!!removeTarget}
                    onClose={() => setRemoveTarget(null)}
                    tenantId={tenantId}
                    warehouseId={removeTarget.warehouseId}
                    productId={removeTarget.productId}
                    currentStock={removeTarget.currentStock}
                    productSku={removeTarget.sku}
                    warehouseName={removeTarget.warehouseName}
                />
            )}
        </Box>
    );
};

export default MerchantWarehousePage;
