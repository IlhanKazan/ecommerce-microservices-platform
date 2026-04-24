import { api } from '../../../lib/axios.ts';
import { API_ENDPOINTS } from '../../../config/apiEndpoints.ts';
import type { User, UpdateProfileRequest, Address, CreateAddressRequest } from "../../../types/user.ts";
import { asRecord, getString, getNumber, getBoolean } from '../../../utils/normalizers.ts';

const mapAddress = (raw: unknown): Address => {
    const r = asRecord(raw);
    return {
        id: getNumber(r, 'id') ?? 0,
        addressType: (getString(r, 'addressType', 'type') ?? 'SHIPPING') as any,
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

    updateProfile: async (data: UpdateProfileRequest): Promise<User> => {
        const response = await api.put<User>(API_ENDPOINTS.USER.UPDATE, data);
        return response.data;
    },

    getUserById: async (id: number): Promise<User> => {
        const response = await api.get<User>(API_ENDPOINTS.USER.BY_ID(id));
        return response.data;
    },

    getAddresses: async (): Promise<Address[]> => {
        const response = await api.get<any[]>(API_ENDPOINTS.USER.ADDRESSES);
        return response.data.map(mapAddress);
    },

    addAddress: async (data: CreateAddressRequest): Promise<Address> => {
        const response = await api.post<any>(API_ENDPOINTS.USER.ADDRESSES, data);
        return mapAddress(response.data);
    },

    updateAddress: async (id: number, data: CreateAddressRequest): Promise<Address> => {
        const response = await api.put<any>(API_ENDPOINTS.USER.ADDRESS_BY_ID(id), data);
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

        const response = await api.post<any>(API_ENDPOINTS.USER.UPLOAD_PROFILE_IMAGE, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });

        return response.data.profileImageUrl || "";
    },
};