import React, { useRef } from 'react';
import {
    Box, Typography, IconButton, Stack,
    Tooltip, Paper, CircularProgress,
} from '@mui/material';
import {
    CloudUpload as UploadIcon,
    Delete as DeleteIcon,
    AddPhotoAlternate as AddPhotoIcon,
} from '@mui/icons-material';
import {
    type ImagePreview,
    readFileAsDataUrl,
    readFilesAsDataUrls,
    formatFileSize,
    ACCEPTED_IMAGE_TYPES,
    MAX_IMAGE_SIZE_BYTES,
    isValidImageType,
    isValidImageSize,
} from '../../utils/imageUploadUtils';

// ─── Single Image ─────────────────────────────────────────────────────────────

interface SingleImageUploadProps {
    label?: string;
    value: ImagePreview | null;
    onChange: (image: ImagePreview | null) => void;
    onError?: (msg: string) => void;
}

export const SingleImageUpload: React.FC<SingleImageUploadProps> = ({
                                                                        label = 'Ana Görsel',
                                                                        value,
                                                                        onChange,
                                                                        onError,
                                                                    }) => {
    const inputRef = useRef<HTMLInputElement>(null);

    const handleFile = async (file: File) => {
        if (!isValidImageType(file)) {
            onError?.('Geçersiz dosya türü. JPEG, PNG, WebP veya GIF yükleyin.');
            return;
        }
        if (!isValidImageSize(file)) {
            onError?.(`Dosya boyutu ${MAX_IMAGE_SIZE_BYTES / 1024 / 1024} MB'ı geçemez.`);
            return;
        }
        try {
            const preview = await readFileAsDataUrl(file);
            onChange(preview);
        } catch {
            onError?.('Dosya okunurken hata oluştu.');
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) handleFile(file);
        e.target.value = '';
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        const file = e.dataTransfer.files?.[0];
        if (file) handleFile(file);
    };

    return (
        <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={600} display="block" mb={1}>
                {label}
            </Typography>

            {value ? (
                <Box sx={{ position: 'relative', display: 'inline-block' }}>
                    <Box
                        component="img"
                        src={value.dataUrl}
                        alt="Ana görsel önizleme"
                        sx={{
                            width: 140, height: 140,
                            objectFit: 'cover',
                            borderRadius: 2,
                            border: '2px solid',
                            borderColor: 'primary.main',
                            display: 'block',
                        }}
                    />
                    <Stack
                        direction="row"
                        spacing={0.5}
                        sx={{ position: 'absolute', top: 4, right: 4 }}
                    >
                        <Tooltip title="Değiştir">
                            <IconButton
                                size="small"
                                sx={{ bgcolor: 'rgba(255,255,255,0.9)', '&:hover': { bgcolor: 'white' } }}
                                onClick={() => inputRef.current?.click()}
                            >
                                <UploadIcon fontSize="small" />
                            </IconButton>
                        </Tooltip>
                        <Tooltip title="Kaldır">
                            <IconButton
                                size="small"
                                sx={{ bgcolor: 'rgba(255,255,255,0.9)', '&:hover': { bgcolor: 'white', color: 'error.main' } }}
                                onClick={() => onChange(null)}
                            >
                                <DeleteIcon fontSize="small" />
                            </IconButton>
                        </Tooltip>
                    </Stack>
                    <Typography variant="caption" color="text.secondary" display="block" mt={0.5}>
                        {value.fileName} · {formatFileSize(value.size)}
                    </Typography>
                </Box>
            ) : (
                <Paper
                    variant="outlined"
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={handleDrop}
                    onClick={() => inputRef.current?.click()}
                    sx={{
                        width: 140, height: 140,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: 1,
                        cursor: 'pointer',
                        borderRadius: 2,
                        borderStyle: 'dashed',
                        transition: 'border-color 0.2s, background 0.2s',
                        '&:hover': { borderColor: 'primary.main', bgcolor: 'primary.50' },
                    }}
                >
                    <UploadIcon color="disabled" />
                    <Typography variant="caption" color="text.secondary" textAlign="center" px={1}>
                        Tıkla veya sürükle
                    </Typography>
                </Paper>
            )}

            <input
                ref={inputRef}
                type="file"
                accept={ACCEPTED_IMAGE_TYPES}
                style={{ display: 'none' }}
                onChange={handleChange}
            />
        </Box>
    );
};

// ─── Multi Image ──────────────────────────────────────────────────────────────

interface MultiImageUploadProps {
    label?: string;
    values: ImagePreview[];
    onChange: (images: ImagePreview[]) => void;
    onError?: (msg: string) => void;
    max?: number;
}

export const MultiImageUpload: React.FC<MultiImageUploadProps> = ({
                                                                      label = 'Ek Görseller',
                                                                      values,
                                                                      onChange,
                                                                      onError,
                                                                      max = 8,
                                                                  }) => {
    const inputRef = useRef<HTMLInputElement>(null);
    const [loading, setLoading] = React.useState(false);

    const handleFiles = async (files: FileList) => {
        const available = max - values.length;
        if (available <= 0) {
            onError?.(`En fazla ${max} ek görsel yükleyebilirsiniz.`);
            return;
        }

        const fileArray = Array.from(files).slice(0, available);

        for (const file of fileArray) {
            if (!isValidImageType(file)) {
                onError?.(`"${file.name}" geçersiz dosya türü.`);
                return;
            }
            if (!isValidImageSize(file)) {
                onError?.(`"${file.name}" boyutu ${MAX_IMAGE_SIZE_BYTES / 1024 / 1024} MB'ı geçiyor.`);
                return;
            }
        }

        setLoading(true);
        try {
            const previews = await readFilesAsDataUrls(
                // DataTransfer hack — native FileList oluşturamayız, bu yüzden FileList cast
                files,
            );
            onChange([...values, ...previews.slice(0, available)]);
        } catch {
            onError?.('Dosyalar okunurken hata oluştu.');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files?.length) handleFiles(e.target.files);
        e.target.value = '';
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        if (e.dataTransfer.files?.length) handleFiles(e.dataTransfer.files);
    };

    const remove = (idx: number) =>
        onChange(values.filter((_, i) => i !== idx));

    return (
        <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={600} display="block" mb={1}>
                {label} ({values.length}/{max})
            </Typography>

            <Stack direction="row" flexWrap="wrap" gap={1.5}>
                {values.map((img, idx) => (
                    <Box key={idx} sx={{ position: 'relative' }}>
                        <Box
                            component="img"
                            src={img.dataUrl}
                            alt={`Görsel ${idx + 1}`}
                            sx={{
                                width: 90, height: 90,
                                objectFit: 'cover',
                                borderRadius: 1.5,
                                border: '1px solid',
                                borderColor: 'divider',
                                display: 'block',
                            }}
                        />
                        <IconButton
                            size="small"
                            onClick={() => remove(idx)}
                            sx={{
                                position: 'absolute', top: -6, right: -6,
                                bgcolor: 'error.main', color: 'white',
                                width: 20, height: 20, fontSize: 12,
                                '&:hover': { bgcolor: 'error.dark' },
                            }}
                        >
                            ×
                        </IconButton>
                    </Box>
                ))}

                {/* Ekle butonu — max dolmadıysa göster */}
                {values.length < max && (
                    <Paper
                        variant="outlined"
                        onDragOver={(e) => e.preventDefault()}
                        onDrop={handleDrop}
                        onClick={() => !loading && inputRef.current?.click()}
                        sx={{
                            width: 90, height: 90,
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 0.5,
                            cursor: loading ? 'wait' : 'pointer',
                            borderRadius: 1.5,
                            borderStyle: 'dashed',
                            '&:hover': { borderColor: 'primary.main', bgcolor: 'primary.50' },
                        }}
                    >
                        {loading
                            ? <CircularProgress size={20} />
                            : <>
                                <AddPhotoIcon color="disabled" fontSize="small" />
                                <Typography variant="caption" color="text.secondary" fontSize="0.65rem">
                                    Ekle
                                </Typography>
                            </>
                        }
                    </Paper>
                )}
            </Stack>

            <input
                ref={inputRef}
                type="file"
                accept={ACCEPTED_IMAGE_TYPES}
                multiple
                style={{ display: 'none' }}
                onChange={handleChange}
            />
        </Box>
    );
};