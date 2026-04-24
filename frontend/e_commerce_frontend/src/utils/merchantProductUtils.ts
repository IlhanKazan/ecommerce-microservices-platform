import type { ProductCreateRequest, TenantProductResponse } from '../types/product';
import type { ImagePreview } from './imageUploadUtils';
import { createImagePreviewFromUrl } from './imageUploadUtils';

export type ProductFormMode = 'create' | 'edit';

export interface ProductFormValues {
    name: string;
    sku: string;
    brand: string;
    price: string;
    discountedPrice: string;
    currency: string;
    categoryId: string;
    description: string;
    mainImage: ImagePreview | null;
    extraImages: ImagePreview[];
    attributes: Array<{ key: string; value: string }>;
}

export const EMPTY_PRODUCT_FORM: ProductFormValues = {
    name: '', sku: '', brand: '', price: '', discountedPrice: '',
    currency: 'TRY', categoryId: '', description: '',
    mainImage: null, extraImages: [], attributes: [],
};

export function productToFormValues(p: TenantProductResponse): ProductFormValues {
    return {
        name:            p.name,
        sku:             p.sku,
        brand:           p.brand ?? '',
        price:           String(p.price),
        discountedPrice: p.discountedPrice != null ? String(p.discountedPrice) : '',
        currency:        p.currency,
        categoryId:      String(p.categoryId),
        description:     p.description ?? '',
        mainImage:       p.mainImageUrl ? createImagePreviewFromUrl(p.mainImageUrl) : null,
        extraImages:     (p.imageUrls ?? []).map(createImagePreviewFromUrl),
        attributes:      Object.entries(p.attributes ?? {}).map(([key, value]) => ({ key, value })),
    };
}

export function formValuesToRequest(values: ProductFormValues): ProductCreateRequest {
    const attributes: Record<string, string> = {};
    values.attributes.forEach(({ key, value }) => {
        if (key.trim()) attributes[key.trim()] = value;
    });

    return {
        name:         values.name,
        sku:          values.sku,
        brand:        values.brand || undefined,
        description:  values.description || undefined,
        price:        parseFloat(values.price),
        currency:     values.currency,
        categoryId:   parseInt(values.categoryId, 10),
        mainImageUrl: values.mainImage?.url || undefined,
        imageUrls:    values.extraImages.length > 0
            ? values.extraImages.map((i) => i.url).filter(Boolean)
            : undefined,
        attributes:   Object.keys(attributes).length > 0 ? attributes : undefined,
    };
}

export function isProductFormValid(values: ProductFormValues): boolean {
    const isUploading =
        values.mainImage?.uploading === true ||
        values.extraImages.some((i) => i.uploading);

    return (
        !isUploading &&
        values.name.trim()     !== '' &&
        values.sku.trim()      !== '' &&
        values.price           !== '' &&
        !isNaN(parseFloat(values.price)) &&
        parseFloat(values.price) > 0    &&
        values.categoryId      !== ''
    );
}