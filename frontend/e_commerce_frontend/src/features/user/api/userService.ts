import { api } from '../../../lib/axios.ts';
import { API_ENDPOINTS } from '../../../config/apiEndpoints.ts';
import type { User, UpdateProfileRequest, Address, CreateAddressRequest, AddressType } from "../../../types/user.ts";
import { asRecord, getString, getNumber, getBoolean } from '../../../utils/normalizers.ts';

const mapAddress = (raw: unknown): Address => {
    const r = asRecord(raw);
    return {
        id: getNumber(r, 'id') ?? 0,
        addressType: (getString(r, 'addressType', 'type') ?? 'SHIPPING') as AddressType,
        label: getString(r, 'label', 'title') ?? '',
        recipientName: getString(r, 'recipientName', 'contactName') ?? '',
        phoneNumber: getString(r, 'phoneNumber', 'phone') ?? '',
        country: getString(r, 'country') ?? '',
        city: getString(r, 'city') ?? '',
        stateProvince: getString(r, 'stateProvince', 'province') ?? '',
        zipCode: getString(r, 'zipCode', 'postalCode') ?? '',
        line1: getString(r, 'line1', 'fullAddress') ?? '',
        line2: getString(r, 'line2') ?? '',
        isDefault: getBoolean(r, 'isDefault')
    };
};

export const userService = {

    getMe: async (): Promise<User> => {
        const response = await api.get<User>(API_ENDPOINTS.USER.ME);
        return response.data;
    },

    // Best-effort gezinme kaydı — öneri/AI verisi. Hata yutulur, UX'i bozmaz.
    recordProductView: async (productId: number, tenantId?: number): Promise<void> => {
        try {
            await api.post(API_ENDPOINTS.USER.ACTIVITY_VIEWS, { productId, tenantId });
        } catch {
            /* sessizce geç */
        }
    },

    // Son gezilen ürün id'leri (en yeni önce). "Son Gezdiklerin" rail'i için.
    getRecentlyViewed: async (limit = 12): Promise<number[]> => {
        const response = await api.get<number[]>(
            API_ENDPOINTS.USER.ACTIVITY_RECENTLY_VIEWED,
            { params: { limit } },
        );
        return response.data ?? [];
    },

    // Best-effort arama kaydı (son aramalar + AI sinyali). Hata yutulur.
    recordSearch: async (term: string): Promise<void> => {
        if (!term || !term.trim()) return;
        try {
            await api.post(API_ENDPOINTS.USER.ACTIVITY_SEARCHES, { term: term.trim() });
        } catch {
            /* sessizce geç */
        }
    },

    getRecentSearches: async (limit = 10): Promise<string[]> => {
        const response = await api.get<string[]>(
            API_ENDPOINTS.USER.ACTIVITY_RECENT_SEARCHES,
            { params: { limit } },
        );
        return response.data ?? [];
    },

    updateProfile: async (data: UpdateProfileRequest): Promise<User> => {
        const response = await api.put<User>(API_ENDPOINTS.USER.UPDATE, data);
        return response.data;
    },

    getUserById: async (id: number): Promise<User> => {
        const response = await api.get<User>(API_ENDPOINTS.USER.BY_ID(id));
        return response.data;
    },

    getAddresses: async (): Promise<Address[]> => {
        const response = await api.get<unknown[]>(API_ENDPOINTS.USER.ADDRESSES);
        return response.data.map(mapAddress);
    },

    addAddress: async (data: CreateAddressRequest): Promise<Address> => {
        const response = await api.post<unknown>(API_ENDPOINTS.USER.ADDRESSES, data);
        return mapAddress(response.data);
    },

    updateAddress: async (id: number, data: CreateAddressRequest): Promise<Address> => {
        const response = await api.put<unknown>(API_ENDPOINTS.USER.ADDRESS_BY_ID(id), data);
        return mapAddress(response.data);
    },

    deleteAddress: async (id: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.USER.ADDRESS_BY_ID(id));
    },

    setDefaultAddress: async (id: number): Promise<void> => {
        await api.put(API_ENDPOINTS.USER.DEFAULT_ADDRESS(id));
    },

    uploadAvatar: async (file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await api.post<{ profileImageUrl: string }>(API_ENDPOINTS.USER.UPLOAD_PROFILE_IMAGE, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        return response.data.profileImageUrl || "";
    },
};