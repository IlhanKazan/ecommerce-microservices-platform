import React, { useRef, useState } from 'react';
import {
    Box, Paper, Typography, Stack, Button, IconButton, Chip, Switch, Divider, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, FormControlLabel, Tooltip, Avatar,
} from '@mui/material';
import {
    Add as AddIcon,
    SubdirectoryArrowRight as SubIcon,
    Edit as EditIcon,
    Delete as DeleteIcon,
    CloudUpload as UploadIcon,
} from '@mui/icons-material';
import type { AdminCategory } from '../../../types/admin';
import {
    useAdminCategories, useCreateCategory, useUpdateCategory, useSetCategoryStatus, useDeleteCategory,
    useUploadCategoryImage,
} from '../../../query/useAdminQueries';
import { useNotification } from '../../../components/shared/NotificationContext';
import EmptyState from '../../../components/shared/EmptyState';

interface FormState {
    mode: 'create' | 'edit';
    parentId: number | null;
    parentName?: string;
    id?: number;
    name: string;
    description: string;
    icon: string;
    imageUrl: string | null;
    displayOrder: string;
    isActive: boolean;
}

const emptyForm = (parentId: number | null, parentName?: string): FormState => ({
    mode: 'create', parentId, parentName, name: '', description: '', icon: '', imageUrl: null, displayOrder: '', isActive: true,
});

const AdminCategoriesPage: React.FC = () => {
    const { notify } = useNotification();
    const { data: categories, isLoading, isError } = useAdminCategories();
    const createMut = useCreateCategory();
    const updateMut = useUpdateCategory();
    const statusMut = useSetCategoryStatus();
    const deleteMut = useDeleteCategory();
    const imageMut = useUploadCategoryImage();

    const [form, setForm] = useState<FormState | null>(null);
    const [toDelete, setToDelete] = useState<AdminCategory | null>(null);
    const fileRef = useRef<HTMLInputElement>(null);

    const saving = createMut.isPending || updateMut.isPending;

    const onError = (e: unknown, fallback: string) => {
        const err = e as { response?: { data?: { message?: string } }; message?: string };
        notify(err?.response?.data?.message || err?.message || fallback, 'error');
    };

    const openCreate = (parent: AdminCategory | null) =>
        setForm(emptyForm(parent?.id ?? null, parent?.name));

    const openEdit = (cat: AdminCategory) =>
        setForm({
            mode: 'edit', parentId: cat.parentId, id: cat.id, name: cat.name,
            description: cat.description ?? '', icon: cat.icon ?? '', imageUrl: cat.imageUrl,
            displayOrder: cat.displayOrder != null ? String(cat.displayOrder) : '', isActive: cat.isActive,
        });

    const handleImagePick = (file: File) => {
        if (!form?.id) return;
        imageMut.mutate({ id: form.id, file }, {
            onSuccess: (updated) => {
                notify('Kategori görseli güncellendi.', 'success');
                setForm((f) => (f ? { ...f, imageUrl: updated.imageUrl } : f));
            },
            onError: (e) => onError(e, 'Görsel yüklenemedi.'),
        });
    };

    const submitForm = () => {
        if (!form || !form.name.trim()) { notify('Kategori adı zorunludur.', 'warning'); return; }
        const displayOrder = form.displayOrder.trim() === '' ? null : Number(form.displayOrder);
        if (form.mode === 'create') {
            createMut.mutate(
                { name: form.name.trim(), description: form.description || null, icon: form.icon || null, displayOrder, isActive: form.isActive, parentId: form.parentId },
                { onSuccess: () => { notify('Kategori oluşturuldu.', 'success'); setForm(null); }, onError: (e) => onError(e, 'Kategori oluşturulamadı.') },
            );
        } else {
            updateMut.mutate(
                { id: form.id!, payload: { name: form.name.trim(), description: form.description || null, icon: form.icon || null, displayOrder, isActive: form.isActive } },
                { onSuccess: () => { notify('Kategori güncellendi.', 'success'); setForm(null); }, onError: (e) => onError(e, 'Kategori güncellenemedi.') },
            );
        }
    };

    const toggleStatus = (cat: AdminCategory) =>
        statusMut.mutate(
            { id: cat.id, active: !cat.isActive },
            { onError: (e) => onError(e, 'Durum değiştirilemedi.') },
        );

    const confirmDelete = () => {
        if (!toDelete) return;
        deleteMut.mutate(toDelete.id, {
            onSuccess: () => { notify('Kategori silindi.', 'success'); setToDelete(null); },
            onError: (e) => { onError(e, 'Kategori silinemedi.'); setToDelete(null); },
        });
    };

    const renderNode = (cat: AdminCategory): React.ReactNode => (
        <Box key={cat.id}>
            <Stack
                direction="row" alignItems="center" spacing={1}
                sx={{ py: 1, pl: cat.level * 3, opacity: cat.isActive ? 1 : 0.55 }}
            >
                {cat.level > 0 && <SubIcon fontSize="small" sx={{ color: 'text.disabled' }} />}
                {cat.icon && <Typography component="span" sx={{ fontSize: 18 }}>{cat.icon}</Typography>}
                <Typography variant="body2" fontWeight={cat.level === 0 ? 700 : 500}>{cat.name}</Typography>
                <Chip size="small" label={cat.slug} variant="outlined" sx={{ height: 20, fontSize: 11 }} />
                {!cat.isActive && <Chip size="small" color="default" label="Pasif" sx={{ height: 20, fontSize: 11 }} />}

                <Box sx={{ flexGrow: 1 }} />

                <Tooltip title={cat.isActive ? 'Pasifleştir' : 'Aktifleştir'}>
                    <Switch size="small" checked={cat.isActive} onChange={() => toggleStatus(cat)} />
                </Tooltip>
                <Tooltip title="Alt kategori ekle">
                    <IconButton size="small" onClick={() => openCreate(cat)}><AddIcon fontSize="small" /></IconButton>
                </Tooltip>
                <Tooltip title="Düzenle">
                    <IconButton size="small" onClick={() => openEdit(cat)}><EditIcon fontSize="small" /></IconButton>
                </Tooltip>
                <Tooltip title="Sil">
                    <IconButton size="small" color="error" onClick={() => setToDelete(cat)}><DeleteIcon fontSize="small" /></IconButton>
                </Tooltip>
            </Stack>
            <Divider />
            {cat.subCategories?.map(renderNode)}
        </Box>
    );

    return (
        <Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 3 }}>
                <Typography variant="h5" fontWeight="bold">Kategori Yönetimi</Typography>
                <Button variant="contained" startIcon={<AddIcon />} onClick={() => openCreate(null)}>
                    Yeni Ana Kategori
                </Button>
            </Stack>

            <Paper variant="outlined" sx={{ borderRadius: 3, p: 2 }}>
                {isLoading ? (
                    <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>
                ) : isError ? (
                    <EmptyState title="Kategoriler yüklenemedi" description="Lütfen daha sonra tekrar deneyin." />
                ) : !categories || categories.length === 0 ? (
                    <EmptyState title="Kategori yok" description="İlk kategoriyi eklemek için yukarıdaki butonu kullanın." />
                ) : (
                    <Box>{categories.map(renderNode)}</Box>
                )}
            </Paper>

            {/* Create / Edit dialog */}
            <Dialog open={form != null} onClose={() => !saving && setForm(null)} maxWidth="xs" fullWidth>
                <DialogTitle>
                    {form?.mode === 'create'
                        ? (form?.parentName ? `Alt Kategori Ekle — ${form.parentName}` : 'Yeni Ana Kategori')
                        : 'Kategoriyi Düzenle'}
                </DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ mt: 1 }}>
                        <TextField
                            label="Kategori Adı" size="small" fullWidth autoFocus required
                            value={form?.name ?? ''}
                            onChange={(e) => form && setForm({ ...form, name: e.target.value })}
                            helperText={form?.mode === 'create' ? 'URL (slug) addan otomatik üretilir.' : 'Slug değişmez.'}
                        />
                        <TextField
                            label="Açıklama" size="small" fullWidth multiline minRows={2}
                            value={form?.description ?? ''}
                            onChange={(e) => form && setForm({ ...form, description: e.target.value })}
                        />
                        <TextField
                            label="İkon (emoji veya isim)" size="small" fullWidth
                            value={form?.icon ?? ''}
                            onChange={(e) => form && setForm({ ...form, icon: e.target.value })}
                        />
                        <TextField
                            label="Sıralama (displayOrder)" size="small" fullWidth type="number"
                            value={form?.displayOrder ?? ''}
                            onChange={(e) => form && setForm({ ...form, displayOrder: e.target.value })}
                        />
                        <FormControlLabel
                            control={<Switch checked={form?.isActive ?? true} onChange={(e) => form && setForm({ ...form, isActive: e.target.checked })} />}
                            label="Aktif"
                        />

                        {form?.mode === 'edit' && (
                            <Box>
                                <Typography variant="caption" color="text.secondary" fontWeight={500}>Kategori Görseli</Typography>
                                <Stack direction="row" spacing={2} alignItems="center" sx={{ mt: 1 }}>
                                    <Avatar variant="rounded" src={form?.imageUrl ?? undefined} sx={{ width: 56, height: 56, bgcolor: 'grey.100' }} />
                                    <Button
                                        size="small" variant="outlined" startIcon={imageMut.isPending ? <CircularProgress size={16} /> : <UploadIcon />}
                                        disabled={imageMut.isPending}
                                        onClick={() => fileRef.current?.click()}
                                    >
                                        {form?.imageUrl ? 'Görseli Değiştir' : 'Görsel Yükle'}
                                    </Button>
                                    <input
                                        ref={fileRef} type="file" accept="image/*" hidden
                                        onChange={(e) => { const f = e.target.files?.[0]; if (f) handleImagePick(f); e.target.value = ''; }}
                                    />
                                </Stack>
                            </Box>
                        )}
                    </Stack>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setForm(null)} disabled={saving} color="inherit">Vazgeç</Button>
                    <Button
                        variant="contained" onClick={submitForm} disabled={saving}
                        startIcon={saving ? <CircularProgress size={18} color="inherit" /> : undefined}
                    >Kaydet</Button>
                </DialogActions>
            </Dialog>

            {/* Delete confirm */}
            <Dialog open={toDelete != null} onClose={() => !deleteMut.isPending && setToDelete(null)} maxWidth="xs" fullWidth>
                <DialogTitle>Kategoriyi Sil</DialogTitle>
                <DialogContent>
                    <Typography variant="body2">
                        <strong>{toDelete?.name}</strong> kategorisini silmek istediğinize emin misiniz?
                        Alt kategorisi veya bağlı ürünü varsa silinemez.
                    </Typography>
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                    <Button onClick={() => setToDelete(null)} disabled={deleteMut.isPending} color="inherit">Vazgeç</Button>
                    <Button
                        variant="contained" color="error" onClick={confirmDelete} disabled={deleteMut.isPending}
                        startIcon={deleteMut.isPending ? <CircularProgress size={18} color="inherit" /> : <DeleteIcon />}
                    >Sil</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default AdminCategoriesPage;
