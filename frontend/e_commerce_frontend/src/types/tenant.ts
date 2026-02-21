import type {Address, CreateAddressRequest} from './user';
import { BusinessType as EnumBusinessType, AddressType as EnumAddressType, TenantStatus as EnumTenantStatus, TenantRole as EnumTenantRole } from './enums';

export type BusinessType = EnumBusinessType;

export type BillingCycle = 'MONTHLY' | 'YEARLY';

export type TenantStatus = EnumTenantStatus;
export type TenantRole = EnumTenantRole;

export type AddressType = EnumAddressType;

export const TENANT_ROLES: { value: TenantRole; label: string }[] = [
    { value: 'OWNER', label: 'Sahip (Owner)' },
    { value: 'ADMIN', label: 'Yönetici (Admin)' },
    { value: 'STAFF', label: 'Personel (Staff)' },
    { value: 'ACCOUNTANT', label: 'Muhasebeci (Accountant)' },
];

export interface PaymentCardInfo {
    holderName: string;
    number: string;
    expireMonth: string;
    expireYear: string;
    cvc: string;
}

export interface CreateTenantRequest {
    name: string;
    businessName: string;
    taxId?: string;
    businessType: BusinessType;
    contactEmail: string;
    contactPhone: string;
    description?: string;
    websiteUrl?: string;
    planId: number;
    selectedAddressId?: number | null;
    newAddress?: Address | CreateAddressRequest | null;
    cardInfo: PaymentCardInfo;
}

export interface AddMemberRequest {
    email: string;
    role: TenantRole;
}


export interface SubscriptionPlan {
    id: number;
    name: string;
    price: number;
    currency: string;
    billingCycle: BillingCycle;
    features: string;
}

export type SubscriptionStatus = 'ACTIVE' | 'PAYMENT_FAILED' | 'CANCELED' | 'TRIALING';

export interface SubscriptionDetail {
    planName: string;
    feeAmount: number;
    cycleUnit: 'MONTHLY' | 'YEARLY';
    nextBillingDate: string;
    status: SubscriptionStatus;
    startedAt: string;
    canceledAt: string | null;
    lastSuccessfulPaymentDate: string;
    failedPaymentCount: number;
    autoRenew: boolean;
    cancellationReason: string | null;
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILURE' | 'REFUNDED';
export type PaymentType = 'SUBSCRIPTION' | 'PRODUCT_ORDER';

export interface PaymentHistoryResponse {
    paymentId: number;
    paymentType: PaymentType;
    amount: number;
    currency: string;
    paymentStatus: PaymentStatus;
    transactionDate: string;
    description: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface TenantSummary {
    id: number;
    name: string;
    businessName: string;
    logoUrl: string | null;
    status: TenantStatus;
    myRole: TenantRole;
}

export interface TenantMember {
    memberId: number;
    userId: number;
    email: string;
    firstName: string;
    lastName: string;
    profileImageUrl: string | null;
    role: TenantRole;
    isActive: boolean;
    joinedAt: string;
}

export interface TenantAddress {
    id: number;
    label: string;
    recipientName: string;
    line1: string;
    line2: string;
    city: string;
    country: string;
    zipCode: string;
    type: AddressType;
}

export interface TenantDetail {
    id: number;
    name: string;
    status: TenantStatus;
    businessName: string;
    taxId: string | null;
    businessType: BusinessType;
    contactEmail: string | null;
    contactPhone: string | null;
    description: string | null;
    logoUrl: string | null;
    websiteUrl: string | null;
    isVerified: boolean;
    createdAt: string;
    members: TenantMember[];
    addresses: TenantAddress[];
    iban: string | null;
    taxOffice: string | null;
    legalCompanyTitle: string | null;
}

export interface UpdateTenantRequest {
    name: string;
    description?: string;
    contactEmail?: string;
    contactPhone?: string;
    websiteUrl?: string;
}

export interface UpdateTenantGeneralRequest {
    name: string;
    businessName: string;
    description: string;
    contactEmail: string;
    contactPhone: string;
    websiteUrl: string;
}

export interface UpdateTenantCriticalRequest {
    businessType: BusinessType;
    taxId: string;
    iban: string;
    taxOffice: string;
    legalCompanyTitle: string;
}

export interface UpdateTenantAddressRequest {
    selectedAddressId?: number;
    manualAddress?: {
        recipientName: string;
        phoneNumber: string;
        country: string;
        city: string;
        stateProvince: string;
        zipCode: string;
        line1: string;
        addressType: AddressType;
    };
}