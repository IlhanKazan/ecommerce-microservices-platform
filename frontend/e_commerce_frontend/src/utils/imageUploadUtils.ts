export interface ImagePreview {
    id: string;
    url: string;        // MinIO URL — submit'te bu gider. Upload öncesi boş.
    previewUrl: string; // Gösterim için — objectURL veya MinIO URL
    uploading: boolean;
    error?: string;
}

export function createImagePreviewFromUrl(url: string): ImagePreview {
    return { id: crypto.randomUUID(), url, previewUrl: url, uploading: false };
}

export function createImagePreviewPlaceholder(file: File): ImagePreview {
    return { id: crypto.randomUUID(), url: '', previewUrl: URL.createObjectURL(file), uploading: true };
}

export const ACCEPTED_IMAGE_TYPES = 'image/jpeg,image/png,image/webp';
export const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

export function isValidImageType(file: File): boolean {
    return ACCEPTED_IMAGE_TYPES.split(',').includes(file.type);
}

export function isValidImageSize(file: File): boolean {
    return file.size <= MAX_IMAGE_SIZE_BYTES;
}