import React, { useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, MenuItem,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
    IconButton, Stack, Typography, Box, Chip, Tooltip, CircularProgress, Divider, Avatar, Alert,
} from '@mui/material';
import {
    Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon,
    Inventory2 as InventoryIcon, Close as CloseIcon, AutoFixHigh as GenerateIcon,
} from '@mui/icons-material';
import {
    useGetVariants, useUpdateVariant, useDeleteVariant,
    useCreateVariantsBatch, useAddManualStockBatch, useGetWarehouses,
    useGetTenantStocks,
} from '../../../query/useProductQueries';
import { productService } from '../../../features/catalog/api/productService';
import { useNotification } from '../../../components/shared/NotificationContext';
import { AddStockModal } from './AddStockModal';
import type { TenantProductResponse, VariantSummary, VariantRequest } from '../../../types/product';

interface Props {
    open: boolean;
    onClose: () => void;
    tenantId: number;
    product: TenantProductResponse | null;
}

interface AttrRow { key: string; value: string }
interface EditFormState {
    id: number;
    attributes: AttrRow[];
    sku: string;
    price: string;
    discountedPrice: string;
    mainImageUrl: string;
}

const formatPrice = (p: number) =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(p);

const attrsToText = (a: Record<string, string>) =>
    Object.entries(a).map(([k, v]) => `${k}: ${v}`).join(' · ');

function errMsg(e: unknown, fallback: string): string {
    return (e as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback;
}

// ─── Matris yardımcıları ───────────────────────────────────────────────────────

interface Axis { name: string; values: string[]; draft: string }

interface GenRow {
    key: string;
    attributes: Record<string, string>;
    combo: string;
    sku: string;
    price: string;
    discountedPrice: string;
    stock: string;
}

// Eksenlerin kartezyen çarpımı: Renk×Numara → tüm kombinasyonlar
function cartesian(axes: Axis[]): Record<string, string>[] {
    const valid = axes.filter((a) => a.name.trim() && a.values.length > 0);
    if (valid.length === 0) return [];
    return valid.reduce<Record<string, string>[]>((acc, axis) => {
        const next: Record<string, string>[] = [];
        acc.forEach((combo) => {
            axis.values.forEach((val) => next.push({ ...combo, [axis.name.trim()]: val }));
        });
        return next;
    }, [{}]);
}

const skuSlug = (s: string) => String(s).trim().toUpperCase().replace(/\s+/g, '-');
const autoSku = (parentSku: string, attrs: Record<string, string>) =>
    [parentSku, ...Object.values(attrs)].map(skuSlug).filter(Boolean).join('-');

const sameAttrs = (a: Record<string, string>, b: Record<string, string>) => {
    const ak = Object.keys(a);
    return ak.length === Object.keys(b).length && ak.every((k) => a[k] === b[k]);
};

// ─── Matris Üretici ────────────────────────────────────────────────────────────

interface BuilderProps {
    tenantId: number;
    parentId: number;
    parentSku: string;
    parentPrice: number;
    existing: VariantSummary[];
}

const VariantMatrixBuilder: React.FC<BuilderProps> = ({ tenantId, parentId, parentSku, parentPrice, existing }) => {
    const { notify } = useNotification();
    const { data: warehouses } = useGetWarehouses(tenantId);
    const { mutateAsync: createBatch, isPending: isCreating } = useCreateVariantsBatch(tenantId, parentId);
    const { mutateAsync: addStockBatch, isPending: isSeeding } = useAddManualStockBatch(tenantId);

    const activeWarehouses = (warehouses ?? []).filter((w) => w.isActive);

    const [axes, setAxes] = useState<Axis[]>([{ name: '', values: [], draft: '' }]);
    const [rows, setRows] = useState<GenRow[] | null>(null);
    const [warehouseId, setWarehouseId] = useState<number | ''>('');
    const [bulkPrice, setBulkPrice] = useState('');
    const [bulkStock, setBulkStock] = useState('');

    // ── Eksen editörü ──
    const setAxisName = (i: number, name: string) =>
        setAxes((p) => p.map((a, idx) => (idx === i ? { ...a, name } : a)));
    const setAxisDraft = (i: number, draft: string) =>
        setAxes((p) => p.map((a, idx) => (idx === i ? { ...a, draft } : a)));
    const addAxisValue = (i: number) =>
        setAxes((p) => p.map((a, idx) => {
            if (idx !== i) return a;
            const v = a.draft.trim();
            if (!v || a.values.includes(v)) return { ...a, draft: '' };
            return { ...a, values: [...a.values, v], draft: '' };
        }));
    const removeAxisValue = (i: number, val: string) =>
        setAxes((p) => p.map((a, idx) => (idx === i ? { ...a, values: a.values.filter((x) => x !== val) } : a)));
    const addAxis = () => setAxes((p) => [...p, { name: '', values: [], draft: '' }]);
    const removeAxis = (i: number) => setAxes((p) => p.filter((_, idx) => idx !== i));

    // ── Kombinasyonları üret ──
    const handleGenerate = () => {
        const combos = cartesian(axes);
        if (combos.length === 0) {
            notify('En az bir eksen adı + bir değer girin (ör. Numara → 40, 41).', 'warning');
            return;
        }
        const fresh = combos.filter((c) => !existing.some((v) => sameAttrs(v.attributes ?? {}, c)));
        if (fresh.length === 0) {
            notify('Üretilen tüm kombinasyonlar zaten mevcut.', 'info');
            return;
        }
        setRows(fresh.map((attributes) => ({
            key: Object.entries(attributes).map(([k, v]) => `${k}=${v}`).join('|'),
            attributes,
            combo: attrsToText(attributes),
            sku: autoSku(parentSku, attributes),
            price: String(parentPrice ?? ''),
            discountedPrice: '',
            stock: '',
        })));
        const skipped = combos.length - fresh.length;
        if (skipped > 0) notify(`${skipped} mevcut kombinasyon atlandı.`, 'info');
    };

    const setRowField = (key: string, field: keyof GenRow, val: string) =>
        setRows((p) => p?.map((r) => (r.key === key ? { ...r, [field]: val } : r)) ?? null);
    const removeRow = (key: string) => setRows((p) => p?.filter((r) => r.key !== key) ?? null);

    const applyBulkPrice = () =>
        setRows((p) => p?.map((r) => ({ ...r, price: bulkPrice })) ?? null);
    const applyBulkStock = () =>
        setRows((p) => p?.map((r) => ({ ...r, stock: bulkStock })) ?? null);

    const resetBuilder = () => {
        setAxes([{ name: '', values: [], draft: '' }]);
        setRows(null);
        setBulkPrice('');
        setBulkStock('');
    };

    // ── Oluştur: önce varyantlar, sonra (varsa) ilk stok ──
    const handleCreate = async () => {
        if (!rows || rows.length === 0) return;

        for (const r of rows) {
            if (!r.sku.trim() || !Number(r.price) || Number(r.price) <= 0) {
                notify('Her satırda SKU ve geçerli fiyat olmalı.', 'warning');
                return;
            }
        }
        const wantsStock = rows.some((r) => Number(r.stock) > 0);
        if (wantsStock && !warehouseId) {
            notify('Stok gireceğiniz için bir depo seçin.', 'warning');
            return;
        }

        const variants: VariantRequest[] = rows.map((r) => ({
            attributes: r.attributes,
            sku: r.sku.trim(),
            price: Number(r.price),
            discountedPrice: r.discountedPrice ? Number(r.discountedPrice) : null,
            mainImageUrl: null,
        }));

        try {
            const created = await createBatch(variants);

            const items = created
                .map((cv) => {
                    const row = rows.find((r) => r.sku.trim() === cv.sku);
                    return { productId: cv.id, amount: row ? Number(row.stock) || 0 : 0 };
                })
                .filter((it) => it.amount > 0);

            if (items.length > 0 && warehouseId) {
                await addStockBatch({ warehouseId: Number(warehouseId), items });
                notify(`${created.length} varyant oluşturuldu, stok girildi.`, 'success');
            } else {
                notify(`${created.length} varyant oluşturuldu.`, 'success');
            }
            resetBuilder();
        } catch (e) {
            notify(errMsg(e, 'Varyantlar oluşturulamadı.'), 'error');
        }
    };

    const busy = isCreating || isSeeding;

    return (
        <Box>
            <Typography variant="overline" color="text.secondary" fontWeight="bold">
                Toplu Varyant Üret
            </Typography>

            {/* ── Eksen editörü ── */}
            <Stack spacing={1.5} sx={{ mt: 1 }}>
                {axes.map((axis, i) => (
                    <Box key={i} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 1.5 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <TextField
                                label="Eksen adı (ör. Renk, Numara, Beden)"
                                size="small"
                                value={axis.name}
                                onChange={(e) => setAxisName(i, e.target.value)}
                                sx={{ flex: 1 }}
                            />
                            <TextField
                                label="Değer ekle + Enter"
                                size="small"
                                value={axis.draft}
                                onChange={(e) => setAxisDraft(i, e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter') { e.preventDefault(); addAxisValue(i); }
                                }}
                                sx={{ flex: 1 }}
                            />
                            <IconButton size="small" color="error" onClick={() => removeAxis(i)} disabled={axes.length === 1}>
                                <DeleteIcon fontSize="small" />
                            </IconButton>
                        </Stack>
                        {axis.values.length > 0 && (
                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                                {axis.values.map((v) => (
                                    <Chip key={v} label={v} size="small" onDelete={() => removeAxisValue(i, v)} />
                                ))}
                            </Stack>
                        )}
                    </Box>
                ))}
                <Stack direction="row" spacing={1}>
                    <Button size="small" startIcon={<AddIcon />} onClick={addAxis}>Eksen ekle</Button>
                    <Button size="small" variant="contained" startIcon={<GenerateIcon />} onClick={handleGenerate}>
                        Kombinasyonları Üret
                    </Button>
                </Stack>
            </Stack>

            {/* ── Üretilen tablo ── */}
            {rows && rows.length > 0 && (
                <Box sx={{ mt: 2 }}>
                    <Divider sx={{ mb: 1.5 }} />
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mb: 1.5 }} alignItems={{ sm: 'center' }}>
                        <TextField
                            select size="small" label="Depo (ilk stok için)" value={warehouseId}
                            onChange={(e) => setWarehouseId(Number(e.target.value))} sx={{ minWidth: 200 }}
                        >
                            {activeWarehouses.length === 0 && (
                                <MenuItem disabled value="">Önce aktif bir depo oluşturun</MenuItem>
                            )}
                            {activeWarehouses.map((w) => (
                                <MenuItem key={w.id} value={w.id}>{w.name} ({w.code})</MenuItem>
                            ))}
                        </TextField>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <TextField size="small" type="number" label="Fiyat" value={bulkPrice}
                                onChange={(e) => setBulkPrice(e.target.value)} sx={{ width: 110 }} />
                            <Button size="small" onClick={applyBulkPrice}>Tümüne</Button>
                        </Stack>
                        <Stack direction="row" spacing={0.5} alignItems="center">
                            <TextField size="small" type="number" label="Stok" value={bulkStock}
                                onChange={(e) => setBulkStock(e.target.value)} sx={{ width: 110 }} />
                            <Button size="small" onClick={applyBulkStock}>Tümüne</Button>
                        </Stack>
                    </Stack>

                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Kombinasyon</TableCell>
                                    <TableCell>SKU</TableCell>
                                    <TableCell align="right">Fiyat (₺)</TableCell>
                                    <TableCell align="right">İndirimli</TableCell>
                                    <TableCell align="right">Stok</TableCell>
                                    <TableCell />
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {rows.map((r) => (
                                    <TableRow key={r.key} hover>
                                        <TableCell sx={{ fontWeight: 600 }}>{r.combo}</TableCell>
                                        <TableCell>
                                            <TextField size="small" variant="standard" value={r.sku}
                                                onChange={(e) => setRowField(r.key, 'sku', e.target.value)} sx={{ minWidth: 130 }} />
                                        </TableCell>
                                        <TableCell align="right">
                                            <TextField size="small" variant="standard" type="number" value={r.price}
                                                onChange={(e) => setRowField(r.key, 'price', e.target.value)} sx={{ width: 80 }} />
                                        </TableCell>
                                        <TableCell align="right">
                                            <TextField size="small" variant="standard" type="number" value={r.discountedPrice}
                                                onChange={(e) => setRowField(r.key, 'discountedPrice', e.target.value)} sx={{ width: 80 }} />
                                        </TableCell>
                                        <TableCell align="right">
                                            <TextField size="small" variant="standard" type="number" value={r.stock}
                                                onChange={(e) => setRowField(r.key, 'stock', e.target.value)} sx={{ width: 70 }} />
                                        </TableCell>
                                        <TableCell align="right">
                                            <IconButton size="small" color="error" onClick={() => removeRow(r.key)}>
                                                <DeleteIcon fontSize="small" />
                                            </IconButton>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>

                    <Stack direction="row" justifyContent="flex-end" spacing={1} sx={{ mt: 2 }}>
                        <Button color="inherit" onClick={resetBuilder} disabled={busy}>Temizle</Button>
                        <Button variant="contained" onClick={handleCreate} disabled={busy}>
                            {busy ? <CircularProgress size={22} color="inherit" /> : `${rows.length} Varyant Oluştur`}
                        </Button>
                    </Stack>
                </Box>
            )}
        </Box>
    );
};

// ─── Ana Modal ─────────────────────────────────────────────────────────────────

export const MerchantVariantsModal: React.FC<Props> = ({ open, onClose, tenantId, product }) => {
    const { notify } = useNotification();
    const productId = product?.id ?? null;

    const { data: variants, isLoading } = useGetVariants(tenantId, productId);
    const { mutate: updateVariant, isPending: isUpdating } = useUpdateVariant(tenantId, productId ?? 0);
    const { mutate: deleteVariant } = useDeleteVariant(tenantId, productId ?? 0);
    const { data: stockSummary } = useGetTenantStocks(tenantId);

    // productId → toplam available quantity (tüm depolar). Varyant başına stok göstermek için.
    const stockByProduct = new Map<number, number>();
    stockSummary?.forEach((s) => {
        stockByProduct.set(s.productId, (stockByProduct.get(s.productId) ?? 0) + s.availableQuantity);
    });

    const [edit, setEdit] = useState<EditFormState | null>(null);
    const [uploading, setUploading] = useState(false);
    const [stockTargetId, setStockTargetId] = useState<number | null>(null);

    const startEdit = (v: VariantSummary) => {
        setEdit({
            id: v.id,
            attributes: Object.entries(v.attributes ?? {}).map(([key, value]) => ({ key, value })),
            sku: v.sku,
            price: String(v.price),
            discountedPrice: v.discountedPrice != null ? String(v.discountedPrice) : '',
            mainImageUrl: v.mainImageUrl ?? '',
        });
    };

    const setAttr = (i: number, field: 'key' | 'value', val: string) =>
        setEdit((p) => {
            if (!p) return p;
            const attributes = [...p.attributes];
            attributes[i] = { ...attributes[i], [field]: val };
            return { ...p, attributes };
        });
    const addAttrRow = () => setEdit((p) => (p ? { ...p, attributes: [...p.attributes, { key: '', value: '' }] } : p));
    const removeAttrRow = (i: number) =>
        setEdit((p) => (p ? { ...p, attributes: p.attributes.filter((_, idx) => idx !== i) } : p));

    const handleImage = async (file: File) => {
        if (!product) return;
        setUploading(true);
        try {
            const url = await productService.uploadProductImage(tenantId, file);
            setEdit((p) => (p ? { ...p, mainImageUrl: url } : p));
        } catch {
            notify('Görsel yüklenemedi.', 'error');
        } finally {
            setUploading(false);
        }
    };

    const handleUpdate = () => {
        if (!edit) return;
        const attributes: Record<string, string> = {};
        edit.attributes.forEach((a) => {
            if (a.key.trim() && a.value.trim()) attributes[a.key.trim()] = a.value.trim();
        });
        if (Object.keys(attributes).length === 0) {
            notify('En az bir özellik (ör. Renk / Numara) girin.', 'warning');
            return;
        }
        const price = Number(edit.price);
        if (!edit.sku.trim() || !price || price <= 0) {
            notify('SKU ve geçerli bir fiyat girin.', 'warning');
            return;
        }
        const body: VariantRequest = {
            attributes,
            sku: edit.sku.trim(),
            price,
            discountedPrice: edit.discountedPrice ? Number(edit.discountedPrice) : null,
            mainImageUrl: edit.mainImageUrl || null,
        };
        updateVariant({ variantId: edit.id, body }, {
            onSuccess: () => { notify('Varyant güncellendi.', 'success'); setEdit(null); },
            onError: (e) => notify(errMsg(e, 'Varyant güncellenemedi.'), 'error'),
        });
    };

    const handleDelete = (id: number) => {
        deleteVariant(id, {
            onSuccess: () => notify('Varyant silindi.', 'success'),
            onError: () => notify('Varyant silinemedi.', 'error'),
        });
    };

    return (
        <>
            <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
                <DialogTitle>
                    Varyantlar — {product?.name}
                    <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
                        <CloseIcon />
                    </IconButton>
                </DialogTitle>
                <DialogContent dividers>
                    {/* Mevcut varyantlar */}
                    {isLoading ? (
                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}><CircularProgress /></Box>
                    ) : (variants && variants.length > 0) ? (
                        <TableContainer sx={{ mb: 2 }}>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Görsel</TableCell>
                                        <TableCell>Kombinasyon</TableCell>
                                        <TableCell>SKU</TableCell>
                                        <TableCell align="right">Fiyat</TableCell>
                                        <TableCell align="center">Stok</TableCell>
                                        <TableCell align="right">İşlem</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {variants.map((v) => (
                                        <TableRow key={v.id} hover>
                                            <TableCell>
                                                <Avatar variant="rounded" src={v.mainImageUrl ?? undefined} sx={{ width: 36, height: 36 }} />
                                            </TableCell>
                                            <TableCell>{attrsToText(v.attributes)}</TableCell>
                                            <TableCell><Chip size="small" label={v.sku} /></TableCell>
                                            <TableCell align="right">
                                                {v.discountedPrice != null
                                                    ? <Stack alignItems="flex-end"><Typography variant="caption" sx={{ textDecoration: 'line-through' }} color="text.secondary">{formatPrice(v.price)}</Typography><Typography color="error.main" fontWeight={700}>{formatPrice(v.discountedPrice)}</Typography></Stack>
                                                    : formatPrice(v.price)}
                                            </TableCell>
                                            <TableCell align="center">
                                                {(() => {
                                                    const qty = stockByProduct.get(v.id);
                                                    if (qty === undefined) {
                                                        return <Typography variant="caption" color="text.disabled">—</Typography>;
                                                    }
                                                    return (
                                                        <Chip
                                                            label={qty}
                                                            size="small"
                                                            color={qty === 0 ? 'error' : qty <= 5 ? 'warning' : 'success'}
                                                            variant="outlined"
                                                        />
                                                    );
                                                })()}
                                            </TableCell>
                                            <TableCell align="right">
                                                <Tooltip title="Stok Ekle"><IconButton size="small" onClick={() => setStockTargetId(v.id)}><InventoryIcon fontSize="small" /></IconButton></Tooltip>
                                                <Tooltip title="Düzenle"><IconButton size="small" onClick={() => startEdit(v)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                                                <Tooltip title="Sil"><IconButton size="small" color="error" onClick={() => handleDelete(v.id)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    ) : (
                        <Typography color="text.secondary" sx={{ py: 2 }}>
                            Henüz varyant yok. Aşağıdan eksenleri (ör. Renk, Numara) tanımlayıp toplu üretin.
                        </Typography>
                    )}

                    <Divider sx={{ my: 2 }} />

                    {/* Düzenleme modu (tek varyant) — yalnız bir varyantı düzenlerken */}
                    {edit ? (
                        <Box>
                            <Typography variant="overline" color="text.secondary" fontWeight="bold">Varyant Düzenle</Typography>
                            <Stack spacing={1.5} sx={{ mt: 1 }}>
                                {edit.attributes.map((attr, i) => (
                                    <Stack key={i} direction="row" spacing={1} alignItems="center">
                                        <TextField label="Özellik (ör. Numara)" size="small" value={attr.key}
                                            onChange={(e) => setAttr(i, 'key', e.target.value)} sx={{ flex: 1 }} />
                                        <TextField label="Değer (ör. 42)" size="small" value={attr.value}
                                            onChange={(e) => setAttr(i, 'value', e.target.value)} sx={{ flex: 1 }} />
                                        <IconButton size="small" color="error" onClick={() => removeAttrRow(i)} disabled={edit.attributes.length === 1}>
                                            <DeleteIcon fontSize="small" />
                                        </IconButton>
                                    </Stack>
                                ))}
                                <Button size="small" startIcon={<AddIcon />} onClick={addAttrRow} sx={{ alignSelf: 'flex-start' }}>
                                    Özellik ekle
                                </Button>
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                                    <TextField label="SKU" size="small" value={edit.sku}
                                        onChange={(e) => setEdit((p) => (p ? { ...p, sku: e.target.value } : p))} sx={{ flex: 1 }} />
                                    <TextField label="Fiyat (₺)" size="small" type="number" value={edit.price}
                                        onChange={(e) => setEdit((p) => (p ? { ...p, price: e.target.value } : p))} sx={{ flex: 1 }} />
                                    <TextField label="İndirimli Fiyat (ops.)" size="small" type="number" value={edit.discountedPrice}
                                        onChange={(e) => setEdit((p) => (p ? { ...p, discountedPrice: e.target.value } : p))} sx={{ flex: 1 }} />
                                </Stack>
                                <Stack direction="row" spacing={2} alignItems="center">
                                    {edit.mainImageUrl && (
                                        <Avatar variant="rounded" src={edit.mainImageUrl} sx={{ width: 48, height: 48 }} />
                                    )}
                                    <Button component="label" variant="outlined" size="small" disabled={uploading}>
                                        {uploading ? 'Yükleniyor...' : 'Görsel Yükle (ops.)'}
                                        <input hidden type="file" accept="image/*"
                                            onChange={(e) => e.target.files?.[0] && handleImage(e.target.files[0])} />
                                    </Button>
                                </Stack>
                            </Stack>
                        </Box>
                    ) : (
                        /* Ekleme: matris üretici */
                        product && (
                            <VariantMatrixBuilder
                                tenantId={tenantId}
                                parentId={product.id}
                                parentSku={product.sku}
                                parentPrice={product.price}
                                existing={variants ?? []}
                            />
                        )
                    )}

                    {!edit && (variants ?? []).length === 0 && (
                        <Alert severity="info" sx={{ mt: 2 }}>
                            İpucu: Renk ve Numara eksenlerini girip "Kombinasyonları Üret"e basın — SKU otomatik, fiyat ana üründen gelir, stoğu satır satır girip tek seferde oluşturun.
                        </Alert>
                    )}
                </DialogContent>
                <DialogActions>
                    {edit && <Button onClick={() => setEdit(null)} color="inherit">Vazgeç</Button>}
                    {edit && (
                        <Button onClick={handleUpdate} variant="contained" disabled={isUpdating}>
                            {isUpdating ? <CircularProgress size={22} color="inherit" /> : 'Güncelle'}
                        </Button>
                    )}
                    {!edit && <Button onClick={onClose} color="inherit">Kapat</Button>}
                </DialogActions>
            </Dialog>

            {stockTargetId != null && (
                <AddStockModal
                    open={stockTargetId != null}
                    onClose={() => setStockTargetId(null)}
                    tenantId={tenantId}
                    productId={stockTargetId}
                />
            )}
        </>
    );
};

export default MerchantVariantsModal;
