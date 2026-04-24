import React, { useEffect, useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, TextField, Grid, CircularProgress, Typography,
    IconButton, Stack, Divider, MenuItem,
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useGetCategories } from '../../../query/useProductQueries';
import { flattenCategories } from '../../../utils/categoryUtils';
import { useToastStore } from '../../../store/useToastStore';
import type { ProductCreateRequest, TenantProductResponse } from '../../../types/product';
import {
    type ProductFormMode,
    type ProductFormValues,
    EMPTY_PRODUCT_FORM,
    productToFormValues,
    formValuesToRequest,
    isProductFormValid,
} from '../../../utils/merchantProductUtils';
import type { ImagePreview } from '../../../utils/imageUploadUtils';
import { SingleImageUpload, MultiImageUpload } from '../../../components/shared/ImageUploadField';

interface MerchantProductFormProps {
    open: boolean;
    mode: ProductFormMode;
    initialData?: TenantProductResponse | null;
    isPending: boolean;
    onClose: () => void;
    onSubmit: (payload: ProductCreateRequest) => void;
}

const MerchantProductForm: React.FC<MerchantProductFormProps> = ({
                                                                     open,
                                                                     mode,
                                                                     initialData,
                                                                     isPending,
                                                                     onClose,
                                                                     onSubmit,
                                                                 }) => {
    const toast = useToastStore();
    const [values, setValues] = useState<ProductFormValues>(EMPTY_PRODUCT_FORM);

    const { data: categoryData } = useGetCategories();
    const flatCategories = flattenCategories(categoryData ?? []);

    useEffect(() => {
        if (mode === 'edit' && initialData) {
            setValues(productToFormValues(initialData));
        } else if (mode === 'create') {
            setValues(EMPTY_PRODUCT_FORM);
        }
    }, [mode, initialData, open]);

    // ─── Field setters ────────────────────────────────────────────────────────

    const set = (field: keyof ProductFormValues, value: unknown) =>
        setValues((prev) => ({ ...prev, [field]: value }));

    const setMainImage   = (img: ImagePreview | null) => set('mainImage', img);
    const setExtraImages = (imgs: ImagePreview[])     => set('extraImages', imgs);

    // ─── Attribute helpers ────────────────────────────────────────────────────

    const addAttribute = () =>
        setValues((prev) => ({
            ...prev,
            attributes: [...prev.attributes, { key: '', value: '' }],
        }));

    const updateAttribute = (index: number, field: 'key' | 'value', val: string) =>
        setValues((prev) => {
            const updated = [...prev.attributes];
            updated[index] = { ...updated[index], [field]: val };
            return { ...prev, attributes: updated };
        });

    const removeAttribute = (index: number) =>
        setValues((prev) => ({
            ...prev,
            attributes: prev.attributes.filter((_, i) => i !== index),
        }));

    const handleSubmit = () => {
        if (!isProductFormValid(values)) return;
        onSubmit(formValuesToRequest(values));
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle fontWeight="bold">
                {mode === 'create' ? 'Yeni Ürün Ekle' : 'Ürünü Düzenle'}
            </DialogTitle>

            <DialogContent dividers>
                <Grid container spacing={2.5} sx={{ pt: 0.5 }}>

                    {/* ── Temel Bilgiler ─────────────────────────────── */}
                    <Grid size={{ xs: 12 }}>
                        <Typography variant="overline" color="text.secondary" fontWeight="bold">
                            Temel Bilgiler
                        </Typography>
                    </Grid>

                    <Grid size={{ xs: 12, sm: 8 }}>
                        <TextField
                            label="Ürün Adı" fullWidth required
                            value={values.name}
                            onChange={(e) => set('name', e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="SKU" fullWidth required
                            value={values.sku}
                            onChange={(e) => set('sku', e.target.value)}
                            helperText="Benzersiz stok kodu"
                        />
                    </Grid>

                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            label="Marka" fullWidth
                            value={values.brand}
                            onChange={(e) => set('brand', e.target.value)}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <TextField
                            select fullWidth required
                            label="Kategori"
                            value={values.categoryId}
                            onChange={(e) => set('categoryId', e.target.value)}
                        >
                            <MenuItem value="" disabled>Kategori seçin</MenuItem>
                            {flatCategories.map((cat) => (
                                <MenuItem
                                    key={cat.id}
                                    value={String(cat.id)}
                                    sx={{ pl: 2 + cat.depth * 1.5 }}
                                >
                                    {cat.depth > 0 && (
                                        <Typography
                                            component="span"
                                            sx={{ color: 'text.disabled', mr: 0.5, fontSize: '0.75rem' }}
                                        >
                                            {'└ '.repeat(cat.depth)}
                                        </Typography>
                                    )}
                                    {cat.name}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>

                    <Grid size={{ xs: 12 }}>
                        <TextField
                            label="Açıklama" fullWidth multiline rows={3}
                            value={values.description}
                            onChange={(e) => set('description', e.target.value)}
                        />
                    </Grid>

                    {/* ── Fiyat ─────────────────────────────────────── */}
                    <Grid size={{ xs: 12 }}>
                        <Divider sx={{ mt: 1, mb: 0.5 }} />
                        <Typography variant="overline" color="text.secondary" fontWeight="bold">
                            Fiyat
                        </Typography>
                    </Grid>

                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="Fiyat" fullWidth required type="number"
                            value={values.price}
                            onChange={(e) => set('price', e.target.value)}
                            slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            label="İndirimli Fiyat (opsiyonel)" fullWidth type="number"
                            value={values.discountedPrice}
                            onChange={(e) => set('discountedPrice', e.target.value)}
                            slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
                            helperText="Boş bırakılırsa indirim gösterilmez"
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            select fullWidth label="Para Birimi"
                            value={values.currency}
                            onChange={(e) => set('currency', e.target.value)}
                        >
                            <MenuItem value="TRY">TRY — Türk Lirası</MenuItem>
                            <MenuItem value="USD">USD — Amerikan Doları</MenuItem>
                            <MenuItem value="EUR">EUR — Euro</MenuItem>
                        </TextField>
                    </Grid>

                    {/* ── Görseller ─────────────────────────────────── */}
                    <Grid size={{ xs: 12 }}>
                        <Divider sx={{ mt: 1, mb: 0.5 }} />
                        <Typography variant="overline" color="text.secondary" fontWeight="bold">
                            Görseller
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                            JPEG, PNG veya WebP · Maks. 5 MB
                        </Typography>
                    </Grid>

                    <Grid size={{ xs: 12, sm: 4 }}>
                        <SingleImageUpload
                            label="Ana Görsel"
                            value={values.mainImage}
                            onChange={setMainImage}
                            onError={(msg) => toast.error(msg)}
                        />
                    </Grid>

                    <Grid size={{ xs: 12, sm: 8 }}>
                        <MultiImageUpload
                            label="Ek Görseller"
                            values={values.extraImages}
                            onChange={setExtraImages}
                            onError={(msg) => toast.error(msg)}
                            max={8}
                        />
                    </Grid>

                    {/* ── Özellikler (Key-Value) ─────────────────────── */}
                    <Grid size={{ xs: 12 }}>
                        <Divider sx={{ mt: 1, mb: 0.5 }} />
                        <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="overline" color="text.secondary" fontWeight="bold">
                                Özellikler
                            </Typography>
                            <Button size="small" startIcon={<AddIcon />} onClick={addAttribute}>
                                Ekle
                            </Button>
                        </Stack>
                    </Grid>

                    {values.attributes.length === 0 && (
                        <Grid size={{ xs: 12 }}>
                            <Typography variant="body2" color="text.disabled" sx={{ pl: 0.5 }}>
                                Henüz özellik eklenmedi. (Örn: Renk → Mavi, Beden → XL)
                            </Typography>
                        </Grid>
                    )}

                    {values.attributes.map((attr, i) => (
                        <Grid size={{ xs: 12 }} key={i}>
                            <Stack direction="row" spacing={1} alignItems="center">
                                <TextField
                                    label="Özellik Adı" size="small" sx={{ flex: 1 }}
                                    value={attr.key}
                                    placeholder="Örn: Renk"
                                    onChange={(e) => updateAttribute(i, 'key', e.target.value)}
                                />
                                <TextField
                                    label="Değer" size="small" sx={{ flex: 1 }}
                                    value={attr.value}
                                    placeholder="Örn: Kırmızı"
                                    onChange={(e) => updateAttribute(i, 'value', e.target.value)}
                                />
                                <IconButton size="small" color="error" onClick={() => removeAttribute(i)}>
                                    <DeleteIcon fontSize="small" />
                                </IconButton>
                            </Stack>
                        </Grid>
                    ))}

                </Grid>
            </DialogContent>

            <DialogActions sx={{ px: 3, py: 2 }}>
                <Button onClick={onClose} color="inherit" disabled={isPending}>
                    İptal
                </Button>
                <Button
                    variant="contained"
                    onClick={handleSubmit}
                    disabled={!isProductFormValid(values) || isPending}
                    startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : undefined}
                >
                    {isPending
                        ? mode === 'create' ? 'Ekleniyor...' : 'Kaydediliyor...'
                        : mode === 'create' ? 'Ürünü Ekle' : 'Değişiklikleri Kaydet'}
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default MerchantProductForm;