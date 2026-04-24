import React, { useState, useEffect } from 'react';
import {
    Box, Paper, TextField, Button, Typography, Avatar,
    Divider, CircularProgress, MenuItem, Stack,
    FormControlLabel, Radio, RadioGroup, Grid, IconButton
} from '@mui/material';
import { useNotification } from '../../../components/shared/NotificationProvider';
import {
    Save as SaveIcon,
    Info as InfoIcon,
    LocationOn as LocationIcon,
    Edit as EditIcon,
    Cancel as CancelIcon,
    PhotoCamera,
    VerifiedUser as VerifiedUserIcon,
    AccountBalance as AccountBalanceIcon
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { useMerchantStore } from '../../../store/useMerchantStore';
import { tenantService } from '../api/tenantService.ts';
import { userService } from '../../user/api/userService.ts';
import AddressSelectionGrid from '../../../components/shared/address/AddressSelectionGrid';
import AddressForm, { type AddressFormData } from '../../../components/shared/address/AddressForm';
import TenantAddressCard from '../../../components/shared/address/TenantAddressCard';

import type { UpdateTenantGeneralRequest, UpdateTenantCriticalRequest } from '../../../types/tenant';
import type { Address, CreateAddressRequest } from '../../../types/user';
import { BusinessType, AddressType } from '../../../types/enums';
import TeamManagementSection from "../components/TeamManagementSection.tsx";

interface MerchantFormState {
    name: string;
    businessName: string;
    description: string;
    contactEmail: string;
    contactPhone: string;
    websiteUrl: string;
}

interface CriticalFormState {
    businessType: typeof BusinessType[keyof typeof BusinessType];
    taxId: string;
    legalCompanyTitle: string;
    taxOffice: string;
    iban: string;
}

const MerchantSettings: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const queryClient = useQueryClient();

    const [isEditingAddress, setIsEditingAddress] = useState(false);
    const [isForcedAddressChange, setIsForcedAddressChange] = useState(false);
    const [addressMode, setAddressMode] = useState<'select' | 'manual'>('select');
    const [selectedAddressId, setSelectedAddressId] = useState<number | ''>('');
    const [isAddressDirty, setIsAddressDirty] = useState(false);

    const [isVerified, setIsVerified] = useState(false);
    const [isEditingVerification, setIsEditingVerification] = useState(false);
    const { notify } = useNotification();

    const [manualAddress, setManualAddress] = useState<AddressFormData>({
        recipientName: '', phoneNumber: '', country: 'Turkey', city: '',
        stateProvince: '', zipCode: '', line1: '', addressType: AddressType.SHIPPING
    });

    const [formData, setFormData] = useState<MerchantFormState>({
        name: '', businessName: '', description: '', contactEmail: '',
        contactPhone: '', websiteUrl: ''
    });

    const [criticalData, setCriticalData] = useState<CriticalFormState>({
        businessType: BusinessType.CORPORATE, taxId: '', legalCompanyTitle: '', taxOffice: '', iban: ''
    });

    const [criticalErrors, setCriticalErrors] = useState<Record<string, string>>({});

    const { data: tenantData, isLoading } = useQuery({
        queryKey: ['tenant', activeTenant?.id],
        queryFn: async () => {
            if (!activeTenant) throw new Error("No tenant");
            const [tenant, userAddresses] = await Promise.all([
                tenantService.getTenantById(activeTenant.id),
                userService.getAddresses()
            ]);
            return { tenant, userAddresses };
        },
        enabled: !!activeTenant
    });

    useEffect(() => {
        if (tenantData?.tenant) {
            const t = tenantData.tenant;
            setIsVerified(t.isVerified || false);
            setIsEditingVerification(false);
            setCriticalErrors({});

            setFormData({
                name: t.name || '',
                businessName: t.businessName || '',
                description: t.description || '',
                contactEmail: t.contactEmail || '',
                contactPhone: t.contactPhone || '',
                websiteUrl: t.websiteUrl || ''
            });

            setCriticalData({
                businessType: t.businessType || BusinessType.CORPORATE,
                taxId: t.taxId || '',
                legalCompanyTitle: t.legalCompanyTitle || '',
                taxOffice: t.taxOffice || '',
                iban: t.iban || ''
            });

            const currentAddr = t.addresses?.[0] as any;
            if (currentAddr) {
                setManualAddress({
                    title: currentAddr.title || '',
                    recipientName: currentAddr.recipientName || '',
                    phoneNumber: currentAddr.phoneNumber || '',
                    country: currentAddr.country || 'Turkey',
                    city: currentAddr.city || '',
                    stateProvince: currentAddr.stateProvince || '',
                    zipCode: currentAddr.zipCode || '',
                    line1: currentAddr.fullAddress || currentAddr.line1 || '',
                    addressType: currentAddr.type || AddressType.SHIPPING
                });
            }
        }
    }, [tenantData]);

    const handleAddressDelete = () => {
        setIsEditingAddress(true);
        setIsForcedAddressChange(true);
        setSelectedAddressId('');
        setAddressMode('select');
        setManualAddress({
            recipientName: '', phoneNumber: '', country: 'Turkey',
            city: '', stateProvince: '', zipCode: '', line1: '', addressType: AddressType.SHIPPING
        });
    };

    const logoMutation = useMutation({
        mutationFn: async (file: File) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.uploadLogo(activeTenant.id, file);
        },
        onSuccess: (freshTenantData) => {
            queryClient.setQueryData(['tenant', activeTenant?.id], (oldData: any) => {
                if (!oldData) return oldData;
                return { ...oldData, tenant: freshTenantData };
            });
        }
    });

    const handleLogoChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            logoMutation.mutate(event.target.files[0]);
        }
    };

    const validateCriticalFields = () => {
        let valid = true;
        const newErrors: Record<string, string> = {};

        if (criticalData.businessType === BusinessType.INDIVIDUAL && criticalData.taxId.length !== 11) {
            newErrors.taxId = "TC Kimlik No 11 haneli olmalıdır.";
            valid = false;
        } else if (criticalData.businessType === BusinessType.CORPORATE && criticalData.taxId.length !== 10) {
            newErrors.taxId = "Vergi No 10 haneli olmalıdır.";
            valid = false;
        }

        if (!criticalData.legalCompanyTitle.trim()) {
            newErrors.legalCompanyTitle = "Bu alan zorunludur.";
            valid = false;
        }

        if (!criticalData.taxOffice.trim()) {
            newErrors.taxOffice = "Bu alan zorunludur.";
            valid = false;
        }

        if (criticalData.iban.length !== 26 || !criticalData.iban.startsWith('TR')) {
            newErrors.iban = "IBAN 'TR' ile başlamalı ve 26 karakter olmalıdır.";
            valid = false;
        }

        setCriticalErrors(newErrors);
        return valid;
    };

    const criticalMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");

            if (!validateCriticalFields()) {
                throw new Error("VALIDATION_ERROR");
            }

            if (!isVerified) {
                await tenantService.verifyTenant(activeTenant.id, {
                    legalCompanyTitle: criticalData.legalCompanyTitle,
                    taxOffice: criticalData.taxOffice,
                    iban: criticalData.iban
                });
            } else {
                const fullPayload: UpdateTenantCriticalRequest = {
                    businessType: criticalData.businessType,
                    taxId: criticalData.taxId,
                    legalCompanyTitle: criticalData.legalCompanyTitle,
                    taxOffice: criticalData.taxOffice,
                    iban: criticalData.iban
                };

                await tenantService.updateCriticalInfo(activeTenant.id, fullPayload);
            }
        },
        onSuccess: () => {
            setIsVerified(true);
            setIsEditingVerification(false);
            setCriticalErrors({});
            queryClient.invalidateQueries({ queryKey: ['tenant', activeTenant?.id] });

            if (!isVerified) {
                notify('Doğrulama işleminiz başarıyla tamamlandı. Artık ödeme alabilirsiniz.', 'success');
            } else {
                notify('Kritik bilgileriniz başarıyla güncellendi.', 'success');
            }
        },
        onError: (error: any) => {
            if (error.message !== "VALIDATION_ERROR") {
                notify(error?.response?.data?.message || error.message || 'İşlem sırasında bir hata oluştu.', 'error');
            }
        }
    });

    const hasChanged = (val1: string | null | undefined, val2: string | null | undefined): boolean => {
        const v1 = val1 ? String(val1).trim() : '';
        const v2 = val2 ? String(val2).trim() : '';
        return v1 !== v2;
    };

    const updateMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant || !tenantData) return;
            const tId = activeTenant.id;
            const original = tenantData.tenant as any;
            const promises: Promise<void>[] = [];

            const isGeneralDirty = (
                hasChanged(formData.name, original.name) ||
                hasChanged(formData.businessName, original.businessName) ||
                hasChanged(formData.description, original.description) ||
                hasChanged(formData.contactEmail, original.contactEmail) ||
                hasChanged(formData.contactPhone, original.contactPhone) ||
                hasChanged(formData.websiteUrl, original.websiteUrl)
            );

            if (isGeneralDirty) {
                const payload: UpdateTenantGeneralRequest = {
                    name: formData.name,
                    businessName: formData.businessName,
                    description: formData.description,
                    contactEmail: formData.contactEmail,
                    contactPhone: formData.contactPhone,
                    websiteUrl: formData.websiteUrl
                };
                promises.push(tenantService.updateGeneralInfo(tId, payload));
            }

            if (isAddressDirty || isForcedAddressChange) {
                const currentAddress = original.addresses?.[0];
                let payloadData: CreateAddressRequest | null = null;

                if (addressMode === 'select' && selectedAddressId) {
                    const selectedAddr = tenantData.userAddresses.find(a => a.id === selectedAddressId);
                    if (selectedAddr) {
                        payloadData = {
                            addressType: selectedAddr.addressType, label: selectedAddr.label, recipientName: selectedAddr.recipientName,
                            phoneNumber: selectedAddr.phoneNumber, country: selectedAddr.country, city: selectedAddr.city,
                            stateProvince: selectedAddr.stateProvince || '', zipCode: selectedAddr.zipCode || '',
                            line1: selectedAddr.line1, line2: selectedAddr.line2 || '', isDefault: true
                        };
                    }
                } else if (addressMode === 'manual') {
                    payloadData = {
                        recipientName: manualAddress.recipientName, phoneNumber: manualAddress.phoneNumber, country: manualAddress.country,
                        city: manualAddress.city, zipCode: manualAddress.zipCode, line1: manualAddress.line1,
                        addressType: manualAddress.addressType || AddressType.SHIPPING, label: manualAddress.title || 'Mağaza Adresi',
                        stateProvince: manualAddress.stateProvince || '', line2: manualAddress.line2 || '', isDefault: true
                    };
                }

                if (payloadData) {
                    if (isForcedAddressChange && currentAddress) {
                        await tenantService.deleteAddress(tId, currentAddress.id);
                        await tenantService.addAddress(tId, payloadData);
                    } else if (currentAddress) {
                        await tenantService.updateAddress(tId, currentAddress.id, payloadData);
                    } else {
                        await tenantService.addAddress(tId, payloadData);
                    }
                }
            }

            await Promise.all(promises);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant', activeTenant?.id] });
            setIsEditingAddress(false);
            setIsForcedAddressChange(false);
            setIsAddressDirty(false);
            notify('Mağaza bilgileri başarıyla güncellendi.', 'success');
        },
        onError: (error: any) => {
            notify(error?.response?.data?.message || error?.message || 'Mağaza bilgileri güncellenirken hata oluştu.', 'error');
        }
    });

    if (isLoading || !tenantData) return <Box sx={{ p: 5, textAlign: 'center' }}><CircularProgress /></Box>;

    const activeAddress = tenantData.tenant.addresses?.[0] as unknown as Address | undefined;
    const isVerificationFieldsDisabled = isVerified && !isEditingVerification;

    return (
        <Box sx={{ maxWidth: 1100, mx: 'auto', p: 4 }}>

            {!isVerified && (
                <Box sx={{ mb: 4, p: 2, borderRadius: 2, bgcolor: '#fff3e0', borderLeft: '4px solid #e65100' }}>
                    <Typography variant="subtitle2" color="#e65100" fontWeight="bold">
                        Satış yapabilmek ve ödeme alabilmek için "Ödeme ve Doğrulama" bilgilerinizi eksiksiz doldurup kaydetmeniz gerekmektedir.
                    </Typography>
                </Box>
            )}

            <Typography variant="h4" fontWeight="800" gutterBottom>Mağaza Yönetimi</Typography>

            <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 8 }}>
                    <Stack spacing={3}>

                        <Paper sx={{ p: 3, borderRadius: 4 }} variant="outlined">
                            <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                                <InfoIcon color="primary" /> Genel Bilgiler
                            </Typography>
                            <Grid container spacing={2}>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField label="Mağaza Vitrin Adı" fullWidth value={formData.name} onChange={(e) => setFormData(p => ({...p, name: e.target.value}))} />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField label="Resmi Şirket Ünvanı" fullWidth value={formData.businessName} onChange={(e) => setFormData(p => ({...p, businessName: e.target.value}))} />
                                </Grid>
                                <Grid size={12}>
                                    <TextField label="Açıklama" fullWidth multiline rows={2} value={formData.description} onChange={(e) => setFormData(p => ({...p, description: e.target.value}))} />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField label="Email" fullWidth value={formData.contactEmail} onChange={(e) => setFormData(p => ({...p, contactEmail: e.target.value}))} />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField label="Telefon" fullWidth value={formData.contactPhone} onChange={(e) => setFormData(p => ({...p, contactPhone: e.target.value}))} />
                                </Grid>
                                <Grid size={12}>
                                    <TextField label="Web Sitesi" fullWidth value={formData.websiteUrl} onChange={(e) => setFormData(p => ({...p, websiteUrl: e.target.value}))} />
                                </Grid>
                            </Grid>
                        </Paper>

                        <Paper sx={{ p: 3, borderRadius: 4, borderColor: isVerified && !isEditingVerification ? '#68d391' : '#e2e8f0', bgcolor: isVerified && !isEditingVerification ? '#f0fff4' : 'rgba(230,126,126,0.6)' }} variant="outlined">

                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 3 }}>
                                <Box>
                                    <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1, color: isVerified && !isEditingVerification ? '#2f855a' : 'text.primary' }}>
                                        <AccountBalanceIcon /> Ödeme ve Doğrulama Bilgileri
                                    </Typography>
                                    {isEditingVerification && (
                                        <Typography variant="caption" color="error.main" fontWeight="bold">
                                            Not: Kayıtlı bilgilerinizi değiştirmek mevcut onayınızı sıfırlar.
                                        </Typography>
                                    )}
                                </Box>

                                {isVerified && !isEditingVerification && (
                                    <Button variant="outlined" startIcon={<EditIcon />} size="small" onClick={() => setIsEditingVerification(true)}>
                                        Bilgileri Güncelle
                                    </Button>
                                )}
                            </Box>

                            <Grid container spacing={2}>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField
                                        select fullWidth label="Şirket Tipi" disabled={isVerificationFieldsDisabled}
                                        value={criticalData.businessType}
                                        onChange={(e) => {
                                            setCriticalData(p => ({...p, businessType: e.target.value as any}));
                                            setCriticalErrors(p => ({...p, taxId: ''}));
                                        }}
                                    >
                                        <MenuItem value={BusinessType.CORPORATE}>Anonim/Limited Şirket</MenuItem>
                                        <MenuItem value={BusinessType.INDIVIDUAL}>Şahıs Şirketi</MenuItem>
                                    </TextField>
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField
                                        label={criticalData.businessType === BusinessType.INDIVIDUAL ? "TC Kimlik No" : "Vergi No"}
                                        fullWidth required disabled={isVerificationFieldsDisabled}
                                        value={criticalData.taxId}
                                        onChange={(e) => setCriticalData(p => ({...p, taxId: e.target.value.replace(/[^0-9]/g, '')}))}
                                        error={!!criticalErrors.taxId}
                                        helperText={criticalErrors.taxId}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField
                                        label="Yasal Şirket Ünvanı / Ad Soyad" fullWidth required disabled={isVerificationFieldsDisabled}
                                        value={criticalData.legalCompanyTitle}
                                        onChange={(e) => setCriticalData(p => ({...p, legalCompanyTitle: e.target.value}))}
                                        error={!!criticalErrors.legalCompanyTitle}
                                        helperText={criticalErrors.legalCompanyTitle}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField
                                        label="Vergi Dairesi" fullWidth required disabled={isVerificationFieldsDisabled}
                                        value={criticalData.taxOffice}
                                        onChange={(e) => setCriticalData(p => ({...p, taxOffice: e.target.value}))}
                                        error={!!criticalErrors.taxOffice}
                                        helperText={criticalErrors.taxOffice}
                                    />
                                </Grid>
                                <Grid size={12}>
                                    <TextField
                                        label="IBAN" fullWidth required disabled={isVerificationFieldsDisabled}
                                        value={criticalData.iban}
                                        placeholder="TR ile başlayarak 26 haneli giriniz"
                                        onChange={(e) => {
                                            let val = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
                                            if (val.length > 26) val = val.slice(0, 26);
                                            setCriticalData(p => ({...p, iban: val}));
                                        }}
                                        error={!!criticalErrors.iban}
                                        helperText={criticalErrors.iban}
                                    />
                                </Grid>
                            </Grid>

                            <Box sx={{ mt: 3 }}>
                                {isVerified && !isEditingVerification ? (
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: '#2e7d32', bgcolor: '#edf7ed', p: 1.5, borderRadius: 1 }}>
                                        <VerifiedUserIcon fontSize="small" />
                                        <Typography variant="body2" fontWeight="bold">
                                            Bilgileriniz doğrulandı. Ödeme almaya hazırsınız.
                                        </Typography>
                                    </Box>
                                ) : (
                                    <Stack direction="row" spacing={2} justifyContent="flex-end">
                                        {isEditingVerification && (
                                            <Button
                                                variant="text"
                                                color="inherit"
                                                onClick={() => {
                                                    setIsEditingVerification(false);
                                                    setCriticalErrors({});
                                                    const t = tenantData.tenant as any;
                                                    setCriticalData({
                                                        businessType: t.businessType || BusinessType.CORPORATE,
                                                        taxId: t.taxId || '',
                                                        legalCompanyTitle: t.legalCompanyTitle || '',
                                                        taxOffice: t.taxOffice || '',
                                                        iban: t.iban || ''
                                                    });
                                                }}
                                            >
                                                İptal
                                            </Button>
                                        )}
                                        <Button
                                            variant="contained" color="primary" fullWidth={!isVerified && !isEditingVerification}
                                            disabled={criticalMutation.isPending}
                                            onClick={() => criticalMutation.mutate()}
                                            startIcon={criticalMutation.isPending ? <CircularProgress size={20} color="inherit" /> : <VerifiedUserIcon />}
                                        >
                                            Bilgileri Doğrula ve Kaydet
                                        </Button>
                                    </Stack>
                                )}
                            </Box>
                        </Paper>

                        <Paper sx={{ p: 3, borderRadius: 4 }} variant="outlined">
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
                                <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <LocationIcon color="primary" /> Mağaza Adresi
                                </Typography>

                                {!isEditingAddress && (
                                    <Button variant="outlined" startIcon={<EditIcon />} size="small" onClick={() => setIsEditingAddress(true)} sx={{ borderRadius: 2 }}>
                                        Adresi Değiştir
                                    </Button>
                                )}
                            </Box>

                            {!isEditingAddress ? (
                                activeAddress ? (
                                    <TenantAddressCard address={activeAddress} onDelete={handleAddressDelete} />
                                ) : (
                                    <Box sx={{ p: 2, borderRadius: 2, bgcolor: '#fff8e1', borderLeft: '4px solid #ffc107' }}>
                                        <Typography variant="body2" color="#f57f17">Henüz tanımlı bir adres yok. Lütfen ekleyin.</Typography>
                                    </Box>
                                )
                            ) : (
                                <Box sx={{ p: 2, border: '1px dashed', borderColor: '#cbd5e1', borderRadius: 3, bgcolor: '#f8fafc' }}>

                                    <RadioGroup row value={addressMode} onChange={(e) => setAddressMode(e.target.value as 'select' | 'manual')} sx={{ mb: 2 }}>
                                        <FormControlLabel value="select" control={<Radio />} label="Adreslerimden Seç" />
                                        <FormControlLabel value="manual" control={<Radio />} label="Yeni Adres Gir" />
                                    </RadioGroup>

                                    {addressMode === 'select' ? (
                                        <AddressSelectionGrid
                                            addresses={tenantData?.userAddresses || []}
                                            selectedId={selectedAddressId}
                                            onSelect={(id) => setSelectedAddressId(id)}
                                        />
                                    ) : (
                                        <AddressForm
                                            data={manualAddress}
                                            onChange={(field, value) => setManualAddress(p => ({...p, [field]: value}))}
                                            showTitle={true}
                                        />
                                    )}

                                    <Stack direction="row" spacing={2} sx={{ mt: 3, justifyContent: 'flex-end' }}>
                                        {!isForcedAddressChange && (
                                            <Button
                                                color="inherit" variant="text" startIcon={<CancelIcon />}
                                                onClick={() => {
                                                    setIsEditingAddress(false);
                                                    setIsAddressDirty(false);
                                                }}
                                            >
                                                Vazgeç
                                            </Button>
                                        )}

                                        <Button
                                            variant="contained"
                                            color={isForcedAddressChange ? "error" : "primary"}
                                            onClick={() => {
                                                if(addressMode === 'select' && !selectedAddressId) return;
                                                setIsAddressDirty(true);
                                                setIsEditingAddress(false);
                                            }}
                                        >
                                            {isForcedAddressChange ? "Yeni Adresi Onayla" : "Uygula"}
                                        </Button>
                                    </Stack>
                                </Box>
                            )}
                        </Paper>

                        {tenantData?.tenant && (
                            <TeamManagementSection
                                tenantId={activeTenant!.id}
                                members={tenantData.tenant.members}
                            />
                        )}

                    </Stack>
                </Grid>

                <Grid size={{ xs: 12, md: 4 }}>
                    <Paper sx={{ p: 3, textAlign: 'center', borderRadius: 4, position: 'sticky', top: 20 }} variant="outlined">

                        <Box sx={{ position: 'relative', display: 'inline-block', mb: 2 }}>
                            <Avatar
                                src={(tenantData.tenant as any).logoUrl || undefined}
                                sx={{
                                    width: 120, height: 120, mx: 'auto', fontSize: '3rem',
                                    border: '4px solid white', boxShadow: 3
                                }}
                            >
                                {tenantData.tenant.name.charAt(0)}
                            </Avatar>

                            <Box sx={{ position: 'absolute', bottom: 0, right: 0 }}>
                                <input
                                    accept="image/*"
                                    style={{ display: 'none' }}
                                    id="icon-button-file"
                                    type="file"
                                    onChange={handleLogoChange}
                                />
                                <label htmlFor="icon-button-file">
                                    <IconButton
                                        color="primary"
                                        aria-label="upload picture"
                                        component="span"
                                        disabled={logoMutation.isPending}
                                        sx={{
                                            bgcolor: 'background.paper',
                                            boxShadow: 2,
                                            '&:hover': { bgcolor: 'grey.100' }
                                        }}
                                    >
                                        {logoMutation.isPending ? <CircularProgress size={20} /> : <PhotoCamera />}
                                    </IconButton>
                                </label>
                            </Box>
                        </Box>

                        <Typography variant="h6" fontWeight="bold">{formData.name}</Typography>
                        <Typography variant="body2" color="text.secondary">ID: #{activeTenant?.id}</Typography>

                        <Divider sx={{ my: 2 }} />

                        <Button
                            variant="contained" fullWidth size="large"
                            startIcon={updateMutation.isPending ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                            onClick={() => updateMutation.mutate()}
                            disabled={updateMutation.isPending}
                        >
                            Genel Bilgileri Kaydet
                        </Button>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default MerchantSettings;