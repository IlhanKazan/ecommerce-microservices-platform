/**
 * Görsel yükleme yardımcı fonksiyonları.
 * Component dışında tutulur — react-refresh/only-export-components kuralı gereği.
 */

export interface ImagePreview {
    /** Gösterim ve gönderim için base64 data URL */
    dataUrl: string;
    /** Orijinal dosya adı */
    fileName: string;
    /** Dosya boyutu (byte) */
    size: number;
}

/** Tek bir File → ImagePreview dönüşümü */
export function readFileAsDataUrl(file: File): Promise<ImagePreview> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload  = () =>
            resolve({
                dataUrl:  reader.result as string,
                fileName: file.name,
                size:     file.size,
            });
        reader.onerror = () => reject(new Error(`Dosya okunamadı: ${file.name}`));
        reader.readAsDataURL(file);
    });
}

/** FileList → ImagePreview[] */
export async function readFilesAsDataUrls(files: FileList): Promise<ImagePreview[]> {
    return Promise.all(Array.from(files).map(readFileAsDataUrl));
}

/** Boyutu okunabilir stringe çevirir */
export function formatFileSize(bytes: number): string {
    if (bytes < 1024)       return `${bytes} B`;
    if (bytes < 1024 ** 2)  return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
}

/** Dosya türü validasyonu */
export const ACCEPTED_IMAGE_TYPES = 'image/jpeg,image/png,image/webp,image/gif';

export function isValidImageType(file: File): boolean {
    return ACCEPTED_IMAGE_TYPES.split(',').includes(file.type);
}

/** Maksimum dosya boyutu: 5 MB */
export const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

export function isValidImageSize(file: File): boolean {
    return file.size <= MAX_IMAGE_SIZE_BYTES;
}