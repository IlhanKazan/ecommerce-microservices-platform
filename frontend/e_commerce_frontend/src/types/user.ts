import { AddressType as EnumAddressType } from './enums';

export type AddressType = EnumAddressType;

export interface Address {
    id: number;
    addressType: AddressType;
    label: string;
    recipientName: string;
    phoneNumber: string;
    country: string;
    city: string;
    stateProvince?: string | null;
    zipCode?: string | null;
    line1: string;
    line2?: string | null;
    isDefault: boolean;
}

export interface CreateAddressRequest {
    addressType: AddressType;
    label: string;
    recipientName: string;
    phoneNumber: string;
    country: string;
    city: string;
    stateProvince: string;
    zipCode: string;
    line1: string;
    line2?: string;
    isDefault: boolean;
}

export interface User {
    id: number;
    username: string | null;
    email: string;
    firstName: string | null;
    lastName: string | null;
    phoneNumber: string | null;
    profileImageUrl: string | null;
    language: string | null;

    isMerchant: boolean;
}

export interface UpdateProfileRequest {
    phoneNumber: string | null;
    profileImageUrl: string | null;
    language: string | null;
}