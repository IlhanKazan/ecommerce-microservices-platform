import { api } from '../../../lib/axios.ts';
import { API_ENDPOINTS } from '../../../config/apiEndpoints.ts';
import type {
    CreateTenantRequest, SubscriptionPlan, TenantDetail, TenantSummary,
    UpdateTenantCriticalRequest,
    UpdateTenantGeneralRequest, TenantAddress, TenantRole, PaymentCardInfo, SubscriptionDetail, PaymentHistoryResponse,
    PageResponse,  AddMemberRequest
} from '../../../types/tenant.ts';
import type {CreateAddressRequest, Address} from "../../../types/user.ts";
import { asRecord, getString, getNumber, getBoolean } from '../../../utils/normalizers.ts';
import type { AddressType as EnumAddressType } from '../../../types/enums.ts';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';

const normalizeTenantAddress = (raw: unknown): TenantAddress => {
    const r = asRecord(raw);
    return {
        id: getNumber(r, 'id') ?? 0,
        label: getString(r, 'title', 'label') ?? '',
        recipientName: getString(r, 'recipientName', 'contactName') ?? '',
        line1: getString(r, 'line1') ?? '',
        line2: getString(r, 'line2') ?? '',
        city: getString(r, 'city') ?? '',
        country: getString(r, 'country') ?? '',
        zipCode: getString(r, 'zipCode', 'postalCode') ?? '',
        type: (getString(r, 'type', 'addressType') ?? 'SHIPPING') as EnumAddressType
    } as TenantAddress;
};

const normalizeTenantDetail = (raw: unknown): TenantDetail => {
    const r = asRecord(raw);
    const membersRaw = Array.isArray(r['members']) ? (r['members'] as unknown[]) : [];

    return {
        id: getNumber(r, 'id') ?? 0,
        name: getString(r, 'name') ?? '',
        status: getString(r, 'status') ?? 'PENDING',
        businessName: getString(r, 'businessName') ?? '',
        taxId: (getString(r, 'taxId') ?? null),
        businessType: getString(r, 'businessType') ?? 'INDIVIDUAL',
        contactEmail: getString(r, 'contactEmail') ?? null,
        contactPhone: getString(r, 'contactPhone') ?? null,
        description: getString(r, 'description') ?? null,
        logoUrl: getString(r, 'logoUrl') ?? null,
        websiteUrl: getString(r, 'websiteUrl') ?? null,
        isVerified: getBoolean(r, 'isVerified'),
        createdAt: getString(r, 'createdAt') ?? new Date().toISOString(),
        members: membersRaw.map((m) => {
            const mm = asRecord(m);
            return {
                memberId: getNumber(mm, 'memberId') ?? 0,
                userId: getNumber(mm, 'userId') ?? getNumber(mm, 'id') ?? 0,
                email: getString(mm, 'email') ?? '',
                firstName: getString(mm, 'firstName', 'first_name') ?? '',
                lastName: getString(mm, 'lastName', 'last_name') ?? '',
                profileImageUrl: getString(mm, 'profileImageUrl', 'profile_image_url') ?? null,
                role: getString(mm, 'role') ?? 'OWNER',
                isActive: getBoolean(mm, 'isActive'),
                joinedAt: getString(mm, 'joinedAt', 'joined_at') ?? new Date().toISOString()
            };
        }),
        addresses: Array.isArray(r['addresses']) ? (r['addresses'] as unknown[]).map(normalizeTenantAddress) : [],
        iban: (getString(r, 'iban') ?? null),
        taxOffice: (getString(r, 'taxOffice') ?? null),
        legalCompanyTitle: (getString(r, 'legalCompanyTitle') ?? null)
    } as TenantDetail;
};

export const tenantService = {
    createTenant: async (data: CreateTenantRequest): Promise<void> => {
        await api.post(API_ENDPOINTS.TENANT.CREATE, data);
    },

    getSubscriptionPlans: async (): Promise<SubscriptionPlan[]> => {
        const response = await api.get<SubscriptionPlan[]>(API_ENDPOINTS.SUBSCRIPTION.PLANS);
        return response.data;
    },

    getMyTenants: async (): Promise<TenantSummary[]> => {
        const response = await api.get<TenantSummary[]>(API_ENDPOINTS.TENANT.ME);
        return response.data;
    },

    getTenantById: async (id: number): Promise<TenantDetail> => {
        const response = await api.get<unknown>(API_ENDPOINTS.TENANT.BY_ID(id));
        return normalizeTenantDetail(response.data);
    },

    uploadLogo: async (id: number, file: File): Promise<TenantDetail> => {
        const formData = new FormData();
        formData.append('file', file);

        const response = await api.post<unknown>(API_ENDPOINTS.TENANT.UPLOAD_LOGO(id), formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return normalizeTenantDetail(response.data);
    },

    changeSubscriptionPlan: async (tenantId: number, planId: number): Promise<void> => {
        await api.post(API_ENDPOINTS.SUBSCRIPTION.CHANGE_PLAN, { tenantId, planId });
    },

    updateGeneralInfo: async (id: number, data: UpdateTenantGeneralRequest): Promise<void> => {
        await api.put(API_ENDPOINTS.TENANT.UPDATE_GENERAL(id), data);
    },

    updateCriticalInfo: async (id: number, data: UpdateTenantCriticalRequest): Promise<void> => {
        await api.put(API_ENDPOINTS.TENANT.UPDATE_CRITICAL(id), data);
    },

    addAddress: async (tenantId: number, data: CreateAddressRequest): Promise<Address> => {
        const response = await api.post<unknown>(API_ENDPOINTS.TENANT.ADDRESSES(tenantId), data);
        const raw = asRecord(response.data);
        return {
            id: getNumber(raw, 'id') ?? 0,
            addressType: (getString(raw, 'addressType', 'type') ?? 'SHIPPING') as EnumAddressType,
            label: getString(raw, 'label', 'title') ?? '',
            recipientName: getString(raw, 'recipientName', 'contactName') ?? '',
            phoneNumber: getString(raw, 'phoneNumber', 'phone') ?? '',
            country: getString(raw, 'country') ?? '',
            city: getString(raw, 'city') ?? '',
            stateProvince: getString(raw, 'stateProvince', 'province') ?? '',
            zipCode: getString(raw, 'zipCode', 'postalCode') ?? '',
            line1: getString(raw, 'line1', 'fullAddress') ?? '',
            line2: getString(raw, 'line2') ?? '',
            isDefault: getBoolean(raw, 'isDefault')
        } as Address;
    },

    updateAddress: async (tenantId: number, addressId: number, data: CreateAddressRequest): Promise<Address> => {
        const response = await api.put<unknown>(API_ENDPOINTS.TENANT.ADDRESS_BY_ID(tenantId, addressId), data);
        const raw = asRecord(response.data);
        return {
            id: getNumber(raw, 'id') ?? 0,
            addressType: (getString(raw, 'addressType', 'type') ?? 'SHIPPING') as EnumAddressType,
            label: getString(raw, 'label', 'title') ?? '',
            recipientName: getString(raw, 'recipientName', 'contactName') ?? '',
            phoneNumber: getString(raw, 'phoneNumber', 'phone') ?? '',
            country: getString(raw, 'country') ?? '',
            city: getString(raw, 'city') ?? '',
            stateProvince: getString(raw, 'stateProvince', 'province') ?? '',
            zipCode: getString(raw, 'zipCode', 'postalCode') ?? '',
            line1: getString(raw, 'line1', 'fullAddress') ?? '',
            line2: getString(raw, 'line2') ?? '',
            isDefault: getBoolean(raw, 'isDefault')
        } as Address;
    },

    deleteAddress: async (tenantId: number, addressId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.TENANT.ADDRESS_BY_ID(tenantId, addressId));
    },

    addMember: async (tenantId: number, data: AddMemberRequest): Promise<void> => {
        await api.post(API_ENDPOINTS.TENANT.ADD_MEMBER(tenantId), data);
    },

    updateMemberRole: async (tenantId: number, memberId: number, newRole: TenantRole): Promise<void> => {
        await api.put(API_ENDPOINTS.TENANT.UPDATE_MEMBER_ROLE(tenantId, memberId), { newRole: newRole });
    },

    removeMember: async (tenantId: number, memberId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.TENANT.REMOVE_MEMBER(tenantId, memberId));
    },

    getSubscriptionDetails: async (tenantId: number): Promise<SubscriptionDetail> => {
        const response = await api.get<SubscriptionDetail>(API_ENDPOINTS.TENANT.SUBSCRIPTION_DETAIL(tenantId));
        return response.data;
    },

    retryPayment: async (tenantId: number, planId: number, newCardInfo: PaymentCardInfo): Promise<void> => {
        const payload = {
            planId: planId,
            newCardInfo: newCardInfo
        };
        await api.post(API_ENDPOINTS.TENANT.RETRY_PAYMENT(tenantId), payload);
    },

    getPaymentHistory: async (tenantId: number, page: number, size: number): Promise<PageResponse<PaymentHistoryResponse>> => {
        const response = await api.get<PageResponse<PaymentHistoryResponse>>(API_ENDPOINTS.TENANT.PAYMENT_HISTORY(tenantId), {
            params: { page, size }
        });
        return response.data;
    },

    verifyTenant: async (tenantId: number, data: { legalCompanyTitle: string; taxOffice: string; iban: string }) => {
        await api.put(API_ENDPOINTS.TENANT.VERIFY_TENANT(tenantId), data);
    },
   /* changeSubscriptionPlan: async (tenantId: number, planId: number): Promise<void> => {
        await api.put(`/tenants/${tenantId}/subscription/plan`, { planId });
    }*/
    getWarehouses: async (tenantId: number) => {
        const response = await api.get(API_ENDPOINTS.STOCK.WAREHOUSES(tenantId));
        return response.data;
    },

    createWarehouse: async (
        tenantId: number,
        payload: { code: string; name: string; locationDetails: string },
        idempotencyKey: string,
    ): Promise<void> => {
        await api.post(
            API_ENDPOINTS.STOCK.WAREHOUSES(tenantId),
            payload,
            { headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey } },
        );
    },

    addManualStock: async (
        tenantId: number,
        payload: { warehouseId: number; productId: number; amount: number },
        idempotencyKey: string,
    ): Promise<void> => {
        await api.post(
            API_ENDPOINTS.STOCK.MANUAL_ADD(tenantId),
            payload,
            { headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey } },
        );
    },
};