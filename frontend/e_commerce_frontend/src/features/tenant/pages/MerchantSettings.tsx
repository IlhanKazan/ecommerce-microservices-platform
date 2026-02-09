import React, { useState, useEffect } from 'react';
import {
    Box, Paper, TextField, Button, Typography, Avatar,
    Divider, Alert, CircularProgress, MenuItem, Stack,
    FormControlLabel, Radio, RadioGroup, Grid, IconButton
} from '@mui/material';
import {
    Save as SaveIcon,
    Business as BusinessIcon,
    Info as InfoIcon,
    LocationOn as LocationIcon,
    Edit as EditIcon,
    Cancel as CancelIcon, PhotoCamera
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { useMerchantStore } from '../../../store/useMerchantStore';
import { tenantService } from '../../../service/tenantService';
import { userService } from '../../../service/userService';
import AddressSelectionGrid from '../../../components/shared/address/AddressSelectionGrid';
import AddressForm, {type AddressFormData } from '../../../components/shared/address/AddressForm';
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
    businessType: typeof BusinessType[keyof typeof BusinessType];
    taxId: string;
}

const MerchantSettings: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const queryClient = useQueryClient();

    const [isEditingAddress, setIsEditingAddress] = useState(false);
    const [isForcedAddressChange, setIsForcedAddressChange] = useState(false);

    const [addressMode, setAddressMode] = useState<'select' | 'manual'>('select');
    const [selectedAddressId, setSelectedAddressId] = useState<number | ''>('');
    const [isAddressDirty, setIsAddressDirty] = useState(false);

    const [manualAddress, setManualAddress] = useState<AddressFormData>({
        recipientName: '', phoneNumber: '', country: 'Turkey', city: '',
        stateProvince: '', zipCode: '', line1: '', addressType: AddressType.SHIPPING
    });

    const [formData, setFormData] = useState<MerchantFormState>({
        name: '', businessName: '', description: '', contactEmail: '',
        contactPhone: '', websiteUrl: '', businessType: BusinessType.CORPORATE, taxId: ''
    });

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
            setFormData({
                name: t.name || '',
                businessName: t.businessName || '',
                description: t.description || '',
                contactEmail: t.contactEmail || '',
                contactPhone: t.contactPhone || '',
                websiteUrl: t.websiteUrl || '',
                businessType: (t.businessType as typeof BusinessType[keyof typeof BusinessType]) || BusinessType.CORPORATE,
                taxId: t.taxId || ''
            });

            const currentAddr = t.addresses?.[0] as unknown as Address | undefined;
            if (currentAddr) {
                setManualAddress({
                    title: (currentAddr as any).title || '',
                    recipientName: (currentAddr as any).recipientName || '',
                    phoneNumber: (currentAddr as any).phoneNumber || '',
                    country: currentAddr.country || 'Turkey',
                    city: currentAddr.city || '',
                    stateProvince: (currentAddr as any).stateProvince || '',
                    zipCode: currentAddr.zipCode || '',
                    line1: (currentAddr as any).fullAddress || (currentAddr as any).line1 || '',
                    addressType: ((currentAddr as any).type as AddressType) || AddressType.SHIPPING
                });
            }
        }
    }, [tenantData]);

    const handleAddressDelete = () => {
        if (window.confirm('Mevcut adresi silmek üzeresiniz. İşleme devam etmek için yeni bir adres belirlemeniz gerekecek.')) {
            setIsEditingAddress(true);
            setIsForcedAddressChange(true);
            setSelectedAddressId('');
            setAddressMode('select');
            setManualAddress({
                recipientName: '', phoneNumber: '', country: 'Turkey',
                city: '', stateProvince: '', zipCode: '', line1: '', addressType: AddressType.SHIPPING
            });
        }
    };

    const logoMutation = useMutation({
        mutationFn: async (file: File) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.uploadLogo(activeTenant.id, file);
        },
        onSuccess: (freshTenantData) => {
            queryClient.setQueryData(['tenant', activeTenant?.id], (oldData: { tenant: unknown; userAddresses: unknown[] } | undefined) => {
                if (!oldData) return oldData;

                return {
                    ...oldData,
                    tenant: freshTenantData
                };
            });

            alert("Logo ve mağaza bilgileri güncellendi!");
        },
        onError: () => {
            alert("Logo yüklenirken hata oluştu.");
        }
    });

    const handleLogoChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            const file = event.target.files[0];
            logoMutation.mutate(file);
        }
    };

    const hasChanged = (
        val1: string | number | null | undefined,
        val2: string | number | null | undefined
    ): boolean => {
        const v1 = (val1 === null || val1 === undefined) ? '' : String(val1).trim();
        const v2 = (val2 === null || val2 === undefined) ? '' : String(val2).trim();
        return v1 !== v2;
    };

    const updateMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant || !tenantData) return;
            const tId = activeTenant.id;
            const original = tenantData.tenant;
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

            const isCriticalDirty = (
                hasChanged(formData.businessType, original.businessType) ||
                hasChanged(formData.taxId, original.taxId)
            );

            if (isCriticalDirty) {
                const payload: UpdateTenantCriticalRequest = {
                    businessType: formData.businessType,
                    taxId: formData.taxId
                };
                promises.push(tenantService.updateCriticalInfo(tId, payload));
            }

            if (isAddressDirty || isForcedAddressChange) {
                const currentAddress = original.addresses?.[0];
                let payloadData: CreateAddressRequest | null = null;

                if (addressMode === 'select' && selectedAddressId) {
                    const selectedAddr = tenantData.userAddresses.find(a => a.id === selectedAddressId);
                    if (selectedAddr) {
                        payloadData = {
                            addressType: selectedAddr.addressType,
                            label: selectedAddr.label,
                            recipientName: selectedAddr.recipientName,
                            phoneNumber: selectedAddr.phoneNumber,
                            country: selectedAddr.country,
                            city: selectedAddr.city,
                            stateProvince: selectedAddr.stateProvince || '',
                            zipCode: selectedAddr.zipCode || '',
                            line1: selectedAddr.line1,
                            line2: selectedAddr.line2 || '',
                            isDefault: true
                        };
                    }
                } else if (addressMode === 'manual') {
                    payloadData = {
                        recipientName: manualAddress.recipientName,
                        phoneNumber: manualAddress.phoneNumber,
                        country: manualAddress.country,
                        city: manualAddress.city,
                        zipCode: manualAddress.zipCode,
                        line1: manualAddress.line1,
                        addressType: manualAddress.addressType || AddressType.SHIPPING,
                        label: manualAddress.title || 'Mağaza Adresi',
                        stateProvince: manualAddress.stateProvince || '',
                        line2: manualAddress.line2 || '',
                        isDefault: true
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
            alert("Mağaza bilgileri başarıyla güncellendi.");
        },
        onError: (err) => {
            console.error(err);
            alert("Güncelleme başarısız oldu.");
        }
    });

    if (isLoading || !tenantData) return <Box sx={{ p: 5, textAlign: 'center' }}><CircularProgress /></Box>;

    const activeAddress = tenantData.tenant.addresses?.[0] as unknown as Address | undefined;

    return (
        <Box sx={{ maxWidth: 1100, mx: 'auto', p: 4 }}>
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

                        <Paper sx={{ p: 3, borderRadius: 4, bgcolor: '#fff5f5', borderColor: '#feb2b2' }} variant="outlined">
                            <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1, color: '#c53030' }}>
                                <BusinessIcon /> Kritik Vergi Bilgileri
                            </Typography>
                            <Grid container spacing={2}>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField select fullWidth label="Şirket Tipi" value={formData.businessType} onChange={(e) => setFormData(p => ({...p, businessType: e.target.value as typeof BusinessType[keyof typeof BusinessType]}))}>
                                        <MenuItem value={BusinessType.CORPORATE}>Anonim/Limited Şirket</MenuItem>
                                        <MenuItem value={BusinessType.INDIVIDUAL}>Şahıs Şirketi</MenuItem>
                                    </TextField>
                                </Grid>
                                <Grid size={{ xs: 12, md: 6 }}>
                                    <TextField label="Vergi Kimlik No" fullWidth value={formData.taxId} onChange={(e) => setFormData(p => ({...p, taxId: e.target.value}))} />
                                </Grid>
                            </Grid>
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
                                    <Alert severity="warning" variant="outlined">Henüz tanımlı bir adres yok. Lütfen ekleyin.</Alert>
                                )
                            ) : (
                                <Box sx={{ p: 2, border: '1px dashed', borderColor: isForcedAddressChange ? 'error.main' : '#cbd5e1', borderRadius: 3, bgcolor: isForcedAddressChange ? '#fff5f5' : '#f8fafc' }}>

                                    {isForcedAddressChange && (
                                        <Alert severity="error" sx={{ mb: 2 }}>
                                            Mevcut adres silindi. Lütfen devam etmek için yeni bir adres seçin veya ekleyin.
                                        </Alert>
                                    )}

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
                                                if(addressMode === 'select' && !selectedAddressId) return alert("Lütfen bir adres seçiniz.");
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
                                src={tenantData.tenant.logoUrl || undefined}
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
                            Değişiklikleri Kaydet
                        </Button>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default MerchantSettings;
