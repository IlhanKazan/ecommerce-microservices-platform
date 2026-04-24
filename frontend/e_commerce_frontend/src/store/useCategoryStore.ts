import { create } from 'zustand';
import type { CategoryResponse } from '../types/product';

interface CategoryState {
    categories: CategoryResponse[];
    isLoaded: boolean;
    setCategories: (cats: CategoryResponse[]) => void;
}

/**
 * Kategori ağacını global tutmak için Zustand store.
 * App.tsx'te useGetCategories() sonucu buraya yazılır,
 * her sayfada tekrar fetch yapılmaz.
 */
export const useCategoryStore = create<CategoryState>((set) => ({
    categories: [],
    isLoaded: false,
    setCategories: (cats) => set({ categories: cats, isLoaded: true }),
}));