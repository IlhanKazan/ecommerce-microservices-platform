import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
    Box, Typography, IconButton, CircularProgress,
    Paper, Stack,
} from '@mui/material';
import { CloudUpload, Delete, AddPhotoAlternate } from '@mui/icons-material';
import {
    type ImagePreview,
    createImagePreviewFromUrl,
    createImagePreviewPlaceholder,
    isValidImageType,
    isValidImageSize,
    ACCEPTED_IMAGE_TYPES,
} from '../../utils/imageUploadUtils';
import { productService } from '../../features/catalog/api/productService';

// ─── Single ───────────────────────────────────────────────────────────────────

interface SingleImageUploadProps {
    label: string;
    value: ImagePreview | null;
    onChange: (img: ImagePreview | null) => void;
    onError: (msg: string) => void;
}

export function SingleImageUpload({ label, value, onChange, onError }: SingleImageUploadProps) {
    const inputRef = useRef<HTMLInputElement>(null);

    const handleFile = useCallback(async (file: File) => {
        if (!isValidImageType(file)) { onError('Geçersiz dosya türü. JPEG, PNG veya WebP yükleyin.'); return; }
        if (!isValidImageSize(file)) { onError('Dosya 5 MB sınırını aşıyor.'); return; }

        const placeholder = createImagePreviewPlaceholder(file);
        onChange(placeholder);

        try {
            const url = await productService.uploadProductImage(file);
            URL.revokeObjectURL(placeholder.previewUrl);
            onChange(createImagePreviewFromUrl(url));
        } catch {
            URL.revokeObjectURL(placeholder.previewUrl);
            onChange(null);
            onError('Görsel yüklenemedi. Tekrar deneyin.');
        }
    }, [onChange, onError]);

    return (
        <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={500}>{label}</Typography>
            <Paper
                variant="outlined"
                onDrop={(e) => { e.preventDefault(); const f = e.dataTransfer.files[0]; if (f) handleFile(f); }}
                onDragOver={(e) => e.preventDefault()}
                onClick={() => !value && inputRef.current?.click()}
                sx={{
                    mt: 0.5, height: 160,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    cursor: value ? 'default' : 'pointer',
                    overflow: 'hidden', position: 'relative',
                    borderStyle: 'dashed',
                    transition: 'border-color .2s',
                    '&:hover': { borderColor: 'primary.main' },
                }}
            >
                {value ? (
                    <>
                        <Box component="img" src={value.previewUrl}
                             sx={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        {value.uploading ? (
                            <Box sx={{ position: 'absolute', inset: 0, bgcolor: 'rgba(0,0,0,.45)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <CircularProgress size={32} sx={{ color: 'white' }} />
                            </Box>
                        ) : (
                            <Stack direction="row" spacing={0.5} sx={{ position: 'absolute', top: 6, right: 6 }}>
                                <IconButton size="small" sx={{ bgcolor: 'rgba(0,0,0,.5)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,.7)' } }}
                                            onClick={(e) => { e.stopPropagation(); inputRef.current?.click(); }}>
                                    <AddPhotoAlternate fontSize="small" />
                                </IconButton>
                                <IconButton size="small" sx={{ bgcolor: 'rgba(0,0,0,.5)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,.7)' } }}
                                            onClick={(e) => { e.stopPropagation(); onChange(null); }}>
                                    <Delete fontSize="small" />
                                </IconButton>
                            </Stack>
                        )}
                    </>
                ) : (
                    <Stack alignItems="center" spacing={1} color="text.disabled">
                        <CloudUpload sx={{ fontSize: 40 }} />
                        <Typography variant="caption">Sürükle bırak veya tıkla</Typography>
                    </Stack>
                )}
            </Paper>
            <input ref={inputRef} type="file" hidden accept={ACCEPTED_IMAGE_TYPES}
                   onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f); e.target.value = ''; }} />
        </Box>
    );
}

// ─── Multi ────────────────────────────────────────────────────────────────────

interface MultiImageUploadProps {
    label: string;
    values: ImagePreview[];
    onChange: (imgs: ImagePreview[]) => void;
    onError: (msg: string) => void;
    max?: number;
}

export function MultiImageUpload({ label, values, onChange, onError, max = 8 }: MultiImageUploadProps) {
    // Internal state — upload süresince parent closure stale kalmaması için
    const [items, setItems] = useState<ImagePreview[]>(values);
    const inputRef = useRef<HTMLInputElement>(null);

    // Edit mode açılışında ya da parent reset'te sync et
    useEffect(() => { setItems(values); }, [values]);

    // Uploading olmayan hazır URL'leri parent'a bildir
    useEffect(() => {
        onChange(items.filter((i) => !i.uploading && !!i.url));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [items]);

    const handleFiles = useCallback(async (files: File[]) => {
        const remaining = max - items.length;
        if (remaining <= 0) { onError(`En fazla ${max} görsel eklenebilir.`); return; }

        const toProcess = files.slice(0, remaining).filter((f) => {
            if (!isValidImageType(f)) { onError('Geçersiz dosya türü.'); return false; }
            if (!isValidImageSize(f)) { onError('Dosya 5 MB sınırını aşıyor.'); return false; }
            return true;
        });
        if (!toProcess.length) return;

        const placeholders = toProcess.map(createImagePreviewPlaceholder);
        setItems((prev) => [...prev, ...placeholders]);

        toProcess.forEach(async (file, i) => {
            const ph = placeholders[i];
            try {
                const url = await productService.uploadProductImage(file);
                URL.revokeObjectURL(ph.previewUrl);
                setItems((prev) => prev.map((item) =>
                    item.id === ph.id ? createImagePreviewFromUrl(url) : item,
                ));
            } catch {
                URL.revokeObjectURL(ph.previewUrl);
                onError(`"${file.name}" yüklenemedi.`);
                setItems((prev) => prev.filter((item) => item.id !== ph.id));
            }
        });
    }, [items, max, onError]);

    const removeItem = (id: string) => setItems((prev) => prev.filter((i) => i.id !== id));

    return (
        <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={500}>{label}</Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 1, mt: 0.5 }}>
                {items.map((img) => (
                    <Paper key={img.id} variant="outlined"
                           sx={{ aspectRatio: '1', position: 'relative', overflow: 'hidden' }}>
                        <Box component="img" src={img.previewUrl}
                             sx={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                        {img.uploading ? (
                            <Box sx={{ position: 'absolute', inset: 0, bgcolor: 'rgba(0,0,0,.45)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <CircularProgress size={22} sx={{ color: 'white' }} />
                            </Box>
                        ) : (
                            <IconButton size="small"
                                        sx={{ position: 'absolute', top: 4, right: 4, bgcolor: 'rgba(0,0,0,.5)', color: 'white', '&:hover': { bgcolor: 'rgba(0,0,0,.7)' } }}
                                        onClick={() => removeItem(img.id)}>
                                <Delete sx={{ fontSize: 16 }} />
                            </IconButton>
                        )}
                    </Paper>
                ))}
                {items.length < max && (
                    <Paper variant="outlined" onClick={() => inputRef.current?.click()}
                           sx={{ aspectRatio: '1', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', borderStyle: 'dashed', '&:hover': { borderColor: 'primary.main' } }}>
                        <Stack alignItems="center" color="text.disabled" spacing={0.5}>
                            <AddPhotoAlternate />
                            <Typography variant="caption">{items.length}/{max}</Typography>
                        </Stack>
                    </Paper>
                )}
            </Box>
            <input ref={inputRef} type="file" hidden multiple accept={ACCEPTED_IMAGE_TYPES}
                   onChange={(e) => { if (e.target.files) handleFiles(Array.from(e.target.files)); e.target.value = ''; }} />
        </Box>
    );
}