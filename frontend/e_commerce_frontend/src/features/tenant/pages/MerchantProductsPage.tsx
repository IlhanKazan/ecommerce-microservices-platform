import React, { useState } from 'react';
import {
    Box, Typography, Button, Paper, Stack, Chip, Alert,
    Table, TableBody, TableCell, TableContainer, TableHead,
    TableRow, TablePagination, IconButton, Tooltip,
    CircularProgress, Dialog, DialogTitle, DialogContent,
    DialogContentText, DialogActions, Avatar,
} from '@mui/material';
import {
    Add as AddIcon,
    Edit as EditIcon,
    Delete as DeleteIcon,
    ToggleOn as ToggleOnIcon,
    ToggleOff as ToggleOffIcon,
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useNotification } from '../../../components/shared/NotificationProvider';
import {
    useGetTenantProducts,
    useCreateTenantProduct,
    useUpdateTenantProduct,
    useDeleteTenantProduct,
    useUpdateSalesStatus,
} from '../../../query/useProductQueries';
import type { TenantProductResponse, ProductCreateRequest } from '../../../types/product';
import MerchantProductForm from '../components/MerchantProductForm';

// ─── Constants ────────────────────────────────────────────────────────────────

type SalesStatus = 'ON_SALE' | 'OUT_OF_STOCK' | 'COMING_SOON';

const STATUS_LABELS: Record<SalesStatus, string> = {
    ON_SALE: 'Satışta',
    OUT_OF_STOCK: 'Stok Yok',
    COMING_SOON: 'Yakında',
};

const STATUS_COLORS: Record<SalesStatus, 'success' | 'error' | 'warning'> = {
    ON_SALE: 'success',
    OUT_OF_STOCK: 'error',
    COMING_SOON: 'warning',
};

const formatPrice = (price: number) =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(price);

// ─── Component ───────────────────────────────────────────────────────────────

const MerchantProductsPage: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const { notify } = useNotification();

    // Sayfalama state
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(20);

    // Form & delete dialog state
    const [formOpen, setFormOpen] = useState(false);
    const [editTarget, setEditTarget] = useState<TenantProductResponse | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<TenantProductResponse | null>(null);

    // Tüm hook'lar koşulsuz çağrılmalı — Rules of Hooks
    // activeTenant yoksa tenantId=0 ile çağrılır; enabled: false koruması
    // useGetTenantProducts içinde !!tenantId kontrolü ile query devre dışı kalır
    const tenantId = activeTenant?.id ?? 0;

    const { data, isLoading, isError } = useGetTenantProducts(tenantId, page, rowsPerPage);
    const { mutate: createProduct, isPending: isCreating } = useCreateTenantProduct(tenantId);
    const { mutate: updateProduct, isPending: isUpdating } = useUpdateTenantProduct(tenantId);
    const { mutate: deleteProduct, isPending: isDeleting } = useDeleteTenantProduct(tenantId);
    const { mutate: updateStatus, isPending: isUpdatingStatus } = useUpdateSalesStatus(tenantId);

    // ─── Erken return — hook'lardan SONRA ────────────────────────────────────
    if (!activeTenant) {
        return (
            <Alert severity="warning">
                Aktif mağaza bulunamadı. Lütfen mağaza seçin.
            </Alert>
        );
    }

    // ─── Handlers ────────────────────────────────────────────────────────────

    const handleOpenCreate = () => {
        setEditTarget(null);
        setFormOpen(true);
    };

    const handleOpenEdit = (product: TenantProductResponse) => {
        setEditTarget(product);
        setFormOpen(true);
    };

    const handleFormSubmit = (payload: ProductCreateRequest) => {
        if (editTarget) {
            updateProduct(
                { productId: editTarget.id, body: payload },
                {
                    onSuccess: () => {
                        setFormOpen(false);
                        notify('Ürün başarıyla güncellendi.', 'success');
                    },
                    onError: () => notify('Ürün güncellenirken hata oluştu.', 'error'),
                },
            );
        } else {
            createProduct(payload, {
                onSuccess: () => {
                    setFormOpen(false);
                    notify('Ürün başarıyla eklendi.', 'success');
                },
                onError: () => notify('Ürün eklenirken hata oluştu.', 'error'),
            });
        }
    };

    const handleDeleteConfirm = () => {
        if (!deleteTarget) return;
        deleteProduct(deleteTarget.id, {
            onSuccess: () => {
                setDeleteTarget(null);
                notify('Ürün başarıyla silindi.', 'success');
            },
            onError: () => {
                setDeleteTarget(null);
                notify('Ürün silinirken hata oluştu.', 'error');
            },
        });
    };

    const handleToggleStatus = (product: TenantProductResponse) => {
        const next: SalesStatus =
            product.salesStatus === 'ON_SALE' ? 'OUT_OF_STOCK' : 'ON_SALE';
        updateStatus(
            { productId: product.id, status: next },
            {
                onSuccess: () =>
                    notify(
                        `"${product.name}" artık ${STATUS_LABELS[next]} olarak işaretlendi.`,
                        'success',
                    ),
                onError: () => notify('Durum güncellenirken hata oluştu.', 'error'),
            },
        );
    };

    // ─── Render ──────────────────────────────────────────────────────────────

    return (
        <Box>
            {/* Başlık satırı */}
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                justifyContent="space-between"
                alignItems={{ sm: 'center' }}
                spacing={2}
                mb={4}
            >
                <Box>
                    <Typography variant="h4" fontWeight="bold">
                        Ürün Yönetimi
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        {activeTenant.name} mağazasının ürünlerini yönetin.
                    </Typography>
                </Box>
                <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={handleOpenCreate}
                    sx={{ alignSelf: { xs: 'flex-start', sm: 'auto' } }}
                >
                    Yeni Ürün Ekle
                </Button>
            </Stack>

            {isError && (
                <Alert severity="error" sx={{ mb: 3 }}>
                    Ürünler yüklenirken bir hata oluştu.
                </Alert>
            )}

            {/* Ürün tablosu */}
            <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
                <TableContainer>
                    <Table size="small">
                        <TableHead sx={{ bgcolor: 'grey.50' }}>
                            <TableRow>
                                <TableCell sx={{ fontWeight: 'bold', width: 56 }}>Görsel</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Ürün</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>SKU</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }}>Kategori</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }} align="right">Fiyat</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }} align="center">Durum</TableCell>
                                <TableCell sx={{ fontWeight: 'bold' }} align="center">İşlemler</TableCell>
                            </TableRow>
                        </TableHead>

                        <TableBody>
                            {isLoading ? (
                                <TableRow>
                                    <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                                        <CircularProgress size={36} />
                                    </TableCell>
                                </TableRow>
                            ) : data?.content.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={7} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                                        Henüz ürün eklemediniz. "Yeni Ürün Ekle" butonuyla başlayın.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                data?.content.map((product) => {
                                    const status = product.salesStatus as SalesStatus;
                                    const isOnSale = status === 'ON_SALE';

                                    return (
                                        <TableRow
                                            key={product.id}
                                            hover
                                            sx={{ '&:last-child td': { border: 0 } }}
                                        >
                                            <TableCell>
                                                <Avatar
                                                    src={product.mainImageUrl ?? undefined}
                                                    variant="rounded"
                                                    sx={{ width: 40, height: 40, bgcolor: 'grey.100' }}
                                                >
                                                    {product.name.charAt(0)}
                                                </Avatar>
                                            </TableCell>

                                            <TableCell>
                                                <Typography
                                                    variant="body2"
                                                    fontWeight={600}
                                                    noWrap
                                                    sx={{ maxWidth: 200 }}
                                                >
                                                    {product.name}
                                                </Typography>
                                                {product.brand && (
                                                    <Typography variant="caption" color="text.secondary">
                                                        {product.brand}
                                                    </Typography>
                                                )}
                                            </TableCell>

                                            <TableCell>
                                                <Typography
                                                    variant="caption"
                                                    fontFamily="monospace"
                                                    color="text.secondary"
                                                >
                                                    {product.sku}
                                                </Typography>
                                            </TableCell>

                                            <TableCell>
                                                <Typography variant="body2" color="text.secondary" noWrap>
                                                    {product.categoryName}
                                                </Typography>
                                            </TableCell>

                                            <TableCell align="right">
                                                {product.discountedPrice !== null ? (
                                                    <Stack alignItems="flex-end" spacing={0}>
                                                        <Typography
                                                            variant="caption"
                                                            color="text.disabled"
                                                            sx={{ textDecoration: 'line-through' }}
                                                        >
                                                            {formatPrice(product.price)}
                                                        </Typography>
                                                        <Typography
                                                            variant="body2"
                                                            fontWeight="bold"
                                                            color="error.main"
                                                        >
                                                            {formatPrice(product.discountedPrice)}
                                                        </Typography>
                                                    </Stack>
                                                ) : (
                                                    <Typography variant="body2" fontWeight={600}>
                                                        {formatPrice(product.price)}
                                                    </Typography>
                                                )}
                                            </TableCell>

                                            <TableCell align="center">
                                                <Chip
                                                    label={STATUS_LABELS[status] ?? status}
                                                    color={STATUS_COLORS[status] ?? 'default'}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </TableCell>

                                            <TableCell align="center">
                                                <Stack direction="row" spacing={0.5} justifyContent="center">
                                                    <Tooltip
                                                        title={isOnSale ? 'Satıştan Kaldır' : 'Satışa Aç'}
                                                    >
                                                        {/* Tooltip disabled buton sarmaladığında span gerekir */}
                                                        <span>
                                                            <IconButton
                                                                size="small"
                                                                color={isOnSale ? 'success' : 'default'}
                                                                disabled={isUpdatingStatus}
                                                                onClick={() => handleToggleStatus(product)}
                                                            >
                                                                {isOnSale
                                                                    ? <ToggleOnIcon fontSize="small" />
                                                                    : <ToggleOffIcon fontSize="small" />
                                                                }
                                                            </IconButton>
                                                        </span>
                                                    </Tooltip>

                                                    <Tooltip title="Düzenle">
                                                        <IconButton
                                                            size="small"
                                                            color="primary"
                                                            onClick={() => handleOpenEdit(product)}
                                                        >
                                                            <EditIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>

                                                    <Tooltip title="Sil">
                                                        <IconButton
                                                            size="small"
                                                            color="error"
                                                            onClick={() => setDeleteTarget(product)}
                                                        >
                                                            <DeleteIcon fontSize="small" />
                                                        </IconButton>
                                                    </Tooltip>
                                                </Stack>
                                            </TableCell>
                                        </TableRow>
                                    );
                                })
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {data && data.totalElements > 0 && (
                    <TablePagination
                        component="div"
                        count={data.totalElements}
                        page={page}
                        rowsPerPage={rowsPerPage}
                        onPageChange={(_, newPage) => setPage(newPage)}
                        onRowsPerPageChange={(e) => {
                            setRowsPerPage(parseInt(e.target.value, 10));
                            setPage(0);
                        }}
                        rowsPerPageOptions={[10, 20, 50]}
                        labelRowsPerPage="Sayfa başına:"
                        labelDisplayedRows={({ from, to, count }) => `${from}–${to} / ${count}`}
                    />
                )}
            </Paper>

            {/* Create / Edit dialog */}
            <MerchantProductForm
                open={formOpen}
                mode={editTarget ? 'edit' : 'create'}
                initialData={editTarget}
                isPending={isCreating || isUpdating}
                onClose={() => setFormOpen(false)}
                onSubmit={handleFormSubmit}
            />

            {/* Delete confirm dialog */}
            <Dialog
                open={!!deleteTarget}
                onClose={() => setDeleteTarget(null)}
                maxWidth="xs"
                fullWidth
            >
                <DialogTitle fontWeight="bold">Ürünü Sil</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        <strong>"{deleteTarget?.name}"</strong> adlı ürünü silmek istediğinize
                        emin misiniz? Bu işlem geri alınamaz.
                    </DialogContentText>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button
                        onClick={() => setDeleteTarget(null)}
                        color="inherit"
                        disabled={isDeleting}
                    >
                        İptal
                    </Button>
                    <Button
                        variant="contained"
                        color="error"
                        disabled={isDeleting}
                        onClick={handleDeleteConfirm}
                        startIcon={
                            isDeleting
                                ? <CircularProgress size={18} color="inherit" />
                                : undefined
                        }
                    >
                        {isDeleting ? 'Siliniyor...' : 'Sil'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default MerchantProductsPage;