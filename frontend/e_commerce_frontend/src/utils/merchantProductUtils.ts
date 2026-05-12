import type { ProductCreateRequest, ProductDetailResponse } from '../types/product';
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
    weightGrams: string;
    dimensionsCm: string;
    minOrderQty: string;
    maxOrderQty: string;
    tags: string;
    seoTitle: string;
    seoDescription: string;
    seoKeywords: string;
}

export const EMPTY_PRODUCT_FORM: ProductFormValues = {
    name: '', sku: '', brand: '', price: '', discountedPrice: '',
    currency: 'TRY', categoryId: '', description: '',
    mainImage: null, extraImages: [], attributes: [],
    weightGrams: '', dimensionsCm: '',
    minOrderQty: '', maxOrderQty: '',
    tags: '',
    seoTitle: '', seoDescription: '', seoKeywords: '',
};

export function productToFormValues(p: ProductDetailResponse): ProductFormValues {
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
        weightGrams:     p.weightGrams != null ? String(p.weightGrams) : '',
        dimensionsCm:    p.dimensionsCm ?? '',
        minOrderQty:     p.minOrderQty != null ? String(p.minOrderQty) : '',
        maxOrderQty:     p.maxOrderQty != null ? String(p.maxOrderQty) : '',
        tags:            (p.tags ?? []).join(', '),
        seoTitle:        p.seoTitle ?? '',
        seoDescription:  p.seoDescription ?? '',
        seoKeywords:     p.seoKeywords ?? '',
    };
}

export function formValuesToRequest(values: ProductFormValues): ProductCreateRequest {
    const attributes: Record<string, string> = {};
    values.attributes.forEach(({ key, value }) => {
        if (key.trim()) attributes[key.trim()] = value;
    });

    const tags = values.tags
        ? values.tags.split(',').map((t) => t.trim()).filter(Boolean)
        : undefined;

    return {
        name:           values.name,
        sku:            values.sku,
        brand:          values.brand || undefined,
        description:    values.description || undefined,
        price:          parseFloat(values.price),
        currency:       values.currency,
        categoryId:     parseInt(values.categoryId, 10),
        mainImageUrl:   values.mainImage?.url || undefined,
        imageUrls:      values.extraImages.length > 0
            ? values.extraImages.map((i) => i.url).filter(Boolean)
            : undefined,
        attributes:     Object.keys(attributes).length > 0 ? attributes : undefined,
        weightGrams:    values.weightGrams ? parseInt(values.weightGrams, 10) : undefined,
        dimensionsCm:   values.dimensionsCm || undefined,
        minOrderQty:    values.minOrderQty ? parseInt(values.minOrderQty, 10) : undefined,
        maxOrderQty:    values.maxOrderQty ? parseInt(values.maxOrderQty, 10) : undefined,
        tags:           tags && tags.length > 0 ? tags : undefined,
        seoTitle:       values.seoTitle || undefined,
        seoDescription: values.seoDescription || undefined,
        seoKeywords:    values.seoKeywords || undefined,
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
