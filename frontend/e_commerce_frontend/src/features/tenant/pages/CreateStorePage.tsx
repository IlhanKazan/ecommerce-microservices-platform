import React, { useState, useEffect } from 'react';
import {
    Box, Stepper, Step, StepLabel, Button, Typography, TextField,
    Paper, MenuItem, Grid, Radio, RadioGroup, FormControlLabel,
    CircularProgress, Alert, Stack, Divider
} from '@mui/material';
import {
    CreditCard as CardIcon,
    CheckCircle as CheckIcon,
    Business as BusinessIcon,
    Check as CheckMarkIcon,
    Person as PersonIcon
} from '@mui/icons-material';
import { userService } from '../../../service/userService';
import { tenantService } from "../../../service/tenantService";
import type { Address } from '../../../types/user';
import type {
    CreateTenantRequest,
    PaymentCardInfo,
    SubscriptionPlan
} from '../../../types/tenant';
import type { BusinessType as BusinessTypeType } from '../../../types/tenant';
import { useNavigate } from 'react-router-dom';
import { AppRoutes } from '../../../utils/routes.ts';
import AddressForm from '../../../components/shared/address/AddressForm';
import AddressSelectionGrid from '../../../components/shared/address/AddressSelectionGrid';
import { AddressType, BusinessType } from '../../../types/enums';
import type { CreateAddressRequest } from '../../../types/user';
import type {ApiErrorResponse} from "../../../types/common.ts";
import {useMerchantStore} from "../../../store/useMerchantStore.ts";

const steps = ['Plan Seçimi', 'Mağaza Bilgileri', 'Adres Bilgileri', 'Ödeme & Onay'];

interface StoreDataState {
    name: string;
    businessName: string;
    taxId: string;
    businessType: BusinessTypeType;
    contactEmail: string;
    contactPhone: string;
    description: string;
    websiteUrl: string;
}

const CreateStorePage: React.FC = () => {
    const navigate = useNavigate();
    const { fetchMyTenants, setActiveTenant } = useMerchantStore();
    const [activeStep, setActiveStep] = useState(0);
    const [isLoading, setIsLoading] = useState(false);
    const [myAddresses, setMyAddresses] = useState<Address[]>([]);

    const [storeData, setStoreData] = useState<StoreDataState>({
        name: '',
        businessName: '',
        taxId: '',
        businessType: BusinessType.INDIVIDUAL,
        contactEmail: '',
        contactPhone: '',
        description: '',
        websiteUrl: ''
    });

    const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
    const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);

    const [addressMode, setAddressMode] = useState<'EXISTING' | 'NEW'>('EXISTING');
    const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
    const [newAddress, setNewAddress] = useState<CreateAddressRequest>({
        recipientName: '', phoneNumber: '', city: '', country: 'Turkey',
        stateProvince: '', zipCode: '', line1: '', addressType: AddressType.SHIPPING, line2: '', label: '', isDefault: false
    });

    const [cardInfo, setCardInfo] = useState<PaymentCardInfo>({
        holderName: '', number: '', expireMonth: '', expireYear: '', cvc: ''
    });

    useEffect(() => {
        const initData = async () => {
            try {
                const [addresses, plansData] = await Promise.all([
                    userService.getAddresses(),
                    tenantService.getSubscriptionPlans()
                ]);
                console.log(plansData);
                setMyAddresses(addresses);
                if (addresses.length === 0) setAddressMode('NEW');

                setPlans(plansData);
            } catch {
                console.error("Veriler yüklenemedi");
            }
        };
        initData();
    }, []);

    const handleNext = async () => {
        if (activeStep === 0 && !selectedPlanId) {
            alert("Lütfen devam etmeden önce bir abonelik planı seçiniz.");
            return;
        }

        if (activeStep === steps.length - 1) {
            await handleSubmit();
        } else {
            setActiveStep((prev) => prev + 1);
        }
    };

    const handleBack = () => setActiveStep((prev) => prev - 1);

    const handleSubmit = async () => {
        if (!selectedPlanId) return;

        setIsLoading(true);
        try {
            let finalSelectedAddressId: number | null = addressMode === 'EXISTING' ? selectedAddressId : null;
            if (addressMode === 'NEW') {
                const created = await userService.addAddress(newAddress);
                finalSelectedAddressId = created.id;
            }

            const payload: CreateTenantRequest = {
                ...storeData,
                planId: selectedPlanId,
                cardInfo: cardInfo,
                selectedAddressId: finalSelectedAddressId,
                newAddress: null
            };

            await tenantService.createTenant(payload);

            alert("Tebrikler! Mağazanız başarıyla oluşturuldu.");

            await fetchMyTenants();
            navigate(AppRoutes.MERCHANT_SELECT);

        } catch (error: any) {
            console.error(error);

            if (error.response && error.response.data) {
                const apiError = error.response.data as ApiErrorResponse;

                if (apiError.errorCode === 'PAYMENT_FAILED') {
                    const createdTenantId = apiError.details?.tenantId;
                    console.log("Ödeme başarısız ama mağaza açıldı. ID:", createdTenantId);

                    alert("Mağazanız oluşturuldu ancak ödeme işlemi başarısız oldu. Kontrol panelinize yönlendiriliyorsunuz.");

                    if (createdTenantId) {
                        await fetchMyTenants();

                        const freshTenants = useMerchantStore.getState().myTenants;
                        const newTenant = freshTenants.find(t => t.id === createdTenantId);

                        if (newTenant) {
                            setActiveTenant(newTenant);
                        }
                    }

                    navigate(AppRoutes.MERCHANT_DASHBOARD);
                    return;
                }

                alert(`Hata: ${apiError.message}`);
            } else {
                alert("Beklenmeyen bir hata oluştu. Lütfen bağlantınızı kontrol ediniz.");
            }
        } finally {
            setIsLoading(false);
        }
    };

    const renderFeatures = (featuresString: string) => {
        let content;
        let isList = false;

        try {
            const parsed = JSON.parse(featuresString);
            if (Array.isArray(parsed)) {
                content = parsed;
                isList = true;
            } else {
                content = featuresString;
            }
        } catch {
            content = featuresString;
        }

        if (isList) {
            return (
                <Stack spacing={1.5} sx={{ flexGrow: 1 }}>
                    {(content as string[]).map((feature, idx) => (
                        <Stack direction="row" alignItems="center" gap={1} key={idx}>
                            <CheckMarkIcon color="success" fontSize="small" />
                            <Typography variant="body2">{feature}</Typography>
                        </Stack>
                    ))}
                </Stack>
            );
        }

        return (
            <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-line' }}>
                    {String(content)}
                </Typography>
            </Box>
        );
    };

    const renderStep0_PlanSelection = () => (
        <Grid container spacing={3} justifyContent="center">
            <Grid size={12}>
                <Typography variant="h6" fontWeight="bold" textAlign="center" sx={{ mb: 3 }}>
                    İşletmenize Uygun Paketi Seçin
                </Typography>
            </Grid>

            {plans.map((plan) => (
                <Grid size={{ xs: 12, md: 4 }} key={plan.id}>
                    <Paper
                        elevation={selectedPlanId === plan.id ? 8 : 1}
                        sx={{
                            p: 3,
                            borderRadius: 4,
                            border: '2px solid',
                            borderColor: selectedPlanId === plan.id ? 'primary.main' : 'transparent',
                            bgcolor: selectedPlanId === plan.id ? 'primary.50' : 'background.paper',
                            cursor: 'pointer',
                            transition: 'all 0.3s',
                            height: '100%',
                            display: 'flex',
                            flexDirection: 'column',
                            '&:hover': { transform: 'translateY(-5px)', borderColor: 'primary.light' }
                        }}
                        onClick={() => setSelectedPlanId(plan.id)}
                    >
                        <Box sx={{ textAlign: 'center', mb: 2 }}>
                            <Typography variant="h5" fontWeight="800" color="primary.main">
                                {plan.name}
                            </Typography>
                            <Typography variant="h4" fontWeight="bold" sx={{ mt: 2 }}>
                                {plan.price} {plan.currency}
                                <Typography component="span" variant="body2" color="text.secondary">
                                    / {plan.billingCycle === 'MONTHLY' ? 'Ay' : 'Yıl'}
                                </Typography>
                            </Typography>
                        </Box>

                        <Divider sx={{ my: 2 }} />

                        {renderFeatures(plan.features)}

                        <Button
                            variant={selectedPlanId === plan.id ? "contained" : "outlined"}
                            fullWidth
                            sx={{ mt: 3, borderRadius: 3 }}
                        >
                            {selectedPlanId === plan.id ? 'Seçildi' : 'Seç'}
                        </Button>
                    </Paper>
                </Grid>
            ))}
        </Grid>
    );

    const renderStep1_StoreInfo = () => (
        <Grid container spacing={3}>
            <Grid size={12}>
                <Typography variant="h6" fontWeight="bold">Mağaza Detayları</Typography>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <TextField label="Mağaza Adı (Görünen Ad)" fullWidth required
                           value={storeData.name} onChange={e => setStoreData({...storeData, name: e.target.value})} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <TextField label="Resmi Şirket/Şahıs Adı" fullWidth required
                           value={storeData.businessName} onChange={e => setStoreData({...storeData, businessName: e.target.value})} />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
                <TextField select label="İşletme Tipi" fullWidth value={storeData.businessType}
                           onChange={e => setStoreData({...storeData, businessType: e.target.value as BusinessType})}>
                    <MenuItem value={BusinessType.INDIVIDUAL}>Şahıs Şirketi / Bireysel</MenuItem>
                    <MenuItem value={BusinessType.CORPORATE}>Limited / Anonim Şirketi</MenuItem>
                </TextField>
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <TextField label="Vergi Numarası / T.C." fullWidth
                           value={storeData.taxId} onChange={e => setStoreData({...storeData, taxId: e.target.value})} />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
                <TextField label="İletişim Email" fullWidth required type="email"
                           value={storeData.contactEmail} onChange={e => setStoreData({...storeData, contactEmail: e.target.value})} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
                <TextField label="İletişim Telefon" fullWidth required
                           value={storeData.contactPhone} onChange={e => setStoreData({...storeData, contactPhone: e.target.value})} />
            </Grid>

            <Grid size={12}>
                <TextField label="Mağaza Açıklaması" fullWidth multiline rows={3}
                           value={storeData.description} onChange={e => setStoreData({...storeData, description: e.target.value})} />
            </Grid>
        </Grid>
    );

    const renderStep2_Address = () => (
        <Box>
            <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>Fatura ve Operasyon Adresi</Typography>

            {myAddresses.length > 0 && (
                <RadioGroup
                    row
                    value={addressMode}
                    onChange={(e) => setAddressMode(e.target.value as 'EXISTING' | 'NEW')}
                    sx={{ mb: 3 }}
                >
                    <FormControlLabel value="EXISTING" control={<Radio />} label="Kayıtlı Adreslerimden Seç" />
                    <FormControlLabel value="NEW" control={<Radio />} label="Yeni Adres Gir" />
                </RadioGroup>
            )}

            {addressMode === 'EXISTING' ? (
                <AddressSelectionGrid
                    addresses={myAddresses}
                    selectedId={selectedAddressId}
                    onSelect={(id) => setSelectedAddressId(id)}
                />
            ) : (
                <AddressForm
                    data={newAddress}
                    onChange={(field, value) => setNewAddress(prev => ({ ...prev, [field]: value }))}
                    showTitle={false}
                />
            )}
        </Box>
    );

    const renderStep3_Payment = () => (
        <Grid container spacing={3}>
            <Grid size={12}>
                <Alert severity="info" icon={<BusinessIcon />}>
                    Mağaza aktivasyonu ve abonelik ücreti için kart bilgilerinizi giriniz. Güvenli ödeme altyapısı kullanılmaktadır.
                </Alert>
            </Grid>

            <Grid size={12}>
                <Paper variant="outlined" sx={{ p: 3, bgcolor: '#fafafa' }}>
                    <Grid container spacing={2}>
                        <Grid size={12}>
                            <TextField label="Kart Üzerindeki İsim" fullWidth required
                                       value={cardInfo.holderName} onChange={e => setCardInfo({...cardInfo, holderName: e.target.value})}
                                       InputProps={{ startAdornment: <PersonIcon sx={{mr:1, color:'text.secondary'}} /> }}
                            />
                        </Grid>
                        <Grid size={12}>
                            <TextField label="Kart Numarası" fullWidth required placeholder="0000 0000 0000 0000"
                                       value={cardInfo.number} onChange={e => setCardInfo({...cardInfo, number: e.target.value})}
                                       InputProps={{ startAdornment: <CardIcon sx={{mr:1, color:'text.secondary'}} /> }}
                            />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="Ay (MM)" fullWidth required placeholder="01"
                                       value={cardInfo.expireMonth} onChange={e => setCardInfo({...cardInfo, expireMonth: e.target.value})} />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="Yıl (YY)" fullWidth required placeholder="28"
                                       value={cardInfo.expireYear} onChange={e => setCardInfo({...cardInfo, expireYear: e.target.value})} />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="CVC" fullWidth required type="password" placeholder="123"
                                       value={cardInfo.cvc} onChange={e => setCardInfo({...cardInfo, cvc: e.target.value})} />
                        </Grid>
                    </Grid>
                </Paper>
            </Grid>
        </Grid>
    );

    return (
        <Box sx={{ maxWidth: 900, mx: 'auto', py: 4, px: 2 }}>
            <Paper elevation={3} sx={{ p: 4, borderRadius: 4 }}>
                <Typography variant="h4" fontWeight="800" textAlign="center" gutterBottom color="primary.main">
                    Mağazanı Oluştur & Satışa Başla
                </Typography>
                <Typography textAlign="center" color="text.secondary" sx={{ mb: 4 }}>
                    Sadece birkaç adımda kendi e-ticaret mağazanızı açın.
                </Typography>

                <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 5 }}>
                    {steps.map((label) => (
                        <Step key={label}>
                            <StepLabel>{label}</StepLabel>
                        </Step>
                    ))}
                </Stepper>

                <Box sx={{ minHeight: 300 }}>
                    {activeStep === 0 && renderStep0_PlanSelection()}
                    {activeStep === 1 && renderStep1_StoreInfo()}
                    {activeStep === 2 && renderStep2_Address()}
                    {activeStep === 3 && renderStep3_Payment()}
                </Box>

                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 5, pt: 2, borderTop: '1px solid #eee' }}>
                    <Button
                        disabled={activeStep === 0}
                        onClick={handleBack}
                        variant="outlined"
                        sx={{ borderRadius: 2, px: 4 }}
                    >
                        Geri
                    </Button>
                    <Button
                        variant="contained"
                        onClick={handleNext}
                        disabled={isLoading}
                        startIcon={isLoading ? <CircularProgress size={20} color="inherit" /> : (activeStep === steps.length - 1 ? <CheckIcon /> : null)}
                        sx={{ borderRadius: 2, px: 5, py: 1.2, fontWeight: 'bold' }}
                    >
                        {activeStep === steps.length - 1 ? 'Tamamla ve Mağazayı Aç' : 'Devam Et'}
                    </Button>
                </Box>
            </Paper>
        </Box>
    );

};

export default CreateStorePage;

