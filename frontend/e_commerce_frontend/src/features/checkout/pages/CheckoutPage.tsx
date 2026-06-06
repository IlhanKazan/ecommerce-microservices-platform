import React, { useEffect, useState } from 'react';
import {
    Box, Typography, Stepper, Step, StepLabel, Button, Paper, Alert,
    Stack, TextField, Divider, CircularProgress, Chip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import { Link as RouterLink } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { useBasket } from '../../../query/useBasketQueries';
import { useGetProductDetail } from '../../../query/useProductQueries';
import { useMe } from '../../../query/useUserQueries';
import { useCreateOrder } from '../../../query/useOrderQueries';
import { userService } from '../../user/api/userService';
import AddressSelectionGrid from '../../../components/shared/address/AddressSelectionGrid';
import AddressFormModal from '../../../components/customer/AddressFormModal';
import { formatPrice } from '../../../utils/formatPrice';
import type { Address, CreateAddressRequest } from '../../../types/user';
import type { CheckoutRequest } from '../../../types/order';

const STEPS = ['Teslimat Adresi', 'Kart Bilgileri', 'Sipariş Onayı'];

const CheckoutPage: React.FC = () => {
    const auth = useAuth();

    const [activeStep, setActiveStep] = useState(0);
    const [selectedAddress, setSelectedAddress] = useState<Address | null>(null);
    const [showAddressForm, setShowAddressForm] = useState(false);
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [isAddressSubmitting, setIsAddressSubmitting] = useState(false);
    const [cardForm, setCardForm] = useState({
        holderName: '',
        number: '',
        expireMonth: '',
        expireYear: '',
        cvc: '',
        gsmNumber: '',
    });
    const [submitError, setSubmitError] = useState<string | null>(null);

    const { data: basket, isLoading: isBasketLoading } = useBasket();
    const firstProductId = basket?.items[0]?.productId ?? 0;
    const { data: productDetail } = useGetProductDetail(firstProductId);
    const tenantId = productDetail?.tenantId ?? null;

    const { data: userData } = useMe(true);
    const createOrder = useCreateOrder();

    useEffect(() => {
        userService.getAddresses().then((addrs) => {
            setAddresses(addrs);
            const defaultAddr = addrs.find((a) => a.isDefault) ?? null;
            if (defaultAddr && !selectedAddress) setSelectedAddress(defaultAddr);
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleNext = () => setActiveStep((s) => s + 1);
    const handleBack = () => setActiveStep((s) => s - 1);

    const handleAddressSelect = (id: number) => {
        setSelectedAddress(addresses.find((a) => a.id === id) ?? null);
    };

    const handleAddressFormSubmit = async (data: CreateAddressRequest) => {
        setIsAddressSubmitting(true);
        try {
            const newAddr = await userService.addAddress(data);
            setAddresses((prev) => [...prev, newAddr]);
            setSelectedAddress(newAddr);
            setShowAddressForm(false);
        } finally {
            setIsAddressSubmitting(false);
        }
    };

    const handleSubmit = () => {
        if (!selectedAddress || !tenantId) return;

        // buyer.id: Keycloak sub her zaman mevcut; userData.id fallback
        const buyerId =
            auth.user?.profile.sub ??
            String(userData?.id ?? 'guest-' + Date.now());

        const payload: CheckoutRequest = {
            shippingAddressJson: JSON.stringify({
                contactName: selectedAddress.recipientName,
                city: selectedAddress.city,
                country: selectedAddress.country,
                fullAddress: selectedAddress.line1,
                zipCode: selectedAddress.zipCode ?? '',
            }),
            cardInfo: {
                holderName: cardForm.holderName,
                number: cardForm.number.replace(/\s/g, ''),
                expireMonth: cardForm.expireMonth,
                expireYear: cardForm.expireYear,
                cvc: cardForm.cvc,
            },
            buyer: {
                id: buyerId,
                name:
                    userData?.firstName ??
                    (auth.user?.profile.given_name as string | undefined) ??
                    'Müşteri',
                surname:
                    userData?.lastName ??
                    (auth.user?.profile.family_name as string | undefined) ??
                    'Kullanıcı',
                email:
                    userData?.email ??
                    (auth.user?.profile.email as string | undefined) ??
                    '',
                gsmNumber: cardForm.gsmNumber,
                identityNumber: '11111111111',
                ip: '127.0.0.1',
                city: selectedAddress.city,
                country: selectedAddress.country,
                zipCode: selectedAddress.zipCode ?? '',
                fullAddress: selectedAddress.line1,
            },
            billingAddress: {
                contactName: selectedAddress.recipientName,
                city: selectedAddress.city,
                country: selectedAddress.country,
                fullAddress: selectedAddress.line1,
                zipCode: selectedAddress.zipCode ?? '',
            },
        };

        setSubmitError(null);
        createOrder.mutate(
            { tenantId, payload },
            {
                onError: (err: unknown) => {
                    const axiosErr = err as { response?: { data?: { errorCode?: string; message?: string } } };
                    const errorCode = axiosErr?.response?.data?.errorCode;
                    if (errorCode === 'INSUFFICIENT_STOCK') {
                        setSubmitError('Yetersiz stok. Lütfen sepetinizi güncelleyin.');
                    } else if (errorCode === 'PAYMENT_FAILED') {
                        setSubmitError(
                            'Ödeme başarısız: ' + (axiosErr?.response?.data?.message ?? ''),
                        );
                    } else if (errorCode === 'EMPTY_BASKET') {
                        setSubmitError('Sepetiniz boş.');
                    } else {
                        setSubmitError('Bir sorun oluştu, lütfen tekrar deneyin.');
                    }
                },
            },
        );
    };

    if (isBasketLoading) {
        return (
            <Box sx={{ py: 8, display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
            </Box>
        );
    }

    if (!basket || basket.items.length === 0) {
        return (
            <Box sx={{ py: 8, textAlign: 'center' }}>
                <Typography variant="h6" gutterBottom>Sepetiniz boş.</Typography>
                <Button component={RouterLink} to="/cart" variant="outlined" sx={{ mt: 2 }}>
                    Sepete Dön
                </Button>
            </Box>
        );
    }

    return (
        <Box sx={{ py: { xs: 3, md: 6 } }}>
            <Typography variant="h5" component="h1" fontWeight="bold" textAlign="center" gutterBottom>
                Sipariş Tamamla
            </Typography>
            <Divider sx={{ mb: 4 }} />

            <Paper elevation={2} sx={{ p: { xs: 2, md: 4 }, maxWidth: 760, mx: 'auto', borderRadius: 3 }}>
                <Stepper activeStep={activeStep} alternativeLabel sx={{ mb: 4 }}>
                    {STEPS.map((label) => (
                        <Step key={label}>
                            <StepLabel>{label}</StepLabel>
                        </Step>
                    ))}
                </Stepper>

                {/* ─── Adım 0: Teslimat Adresi ─── */}
                {activeStep === 0 && (
                    <Box>
                        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
                            <LocationOnIcon color="primary" />
                            <Typography variant="h6">Teslimat Adresi</Typography>
                        </Stack>

                        {addresses.length > 0 ? (
                            <AddressSelectionGrid
                                addresses={addresses}
                                selectedId={selectedAddress?.id ?? null}
                                onSelect={handleAddressSelect}
                            />
                        ) : (
                            <Alert severity="info" sx={{ mb: 2 }}>
                                Kayıtlı adresiniz yok. Lütfen yeni bir adres ekleyin.
                            </Alert>
                        )}

                        <Button
                            startIcon={<AddIcon />}
                            size="small"
                            onClick={() => setShowAddressForm(true)}
                            sx={{ mt: 1.5, mb: 2 }}
                        >
                            Yeni Adres Ekle
                        </Button>

                        {showAddressForm && (
                            <AddressFormModal
                                open={showAddressForm}
                                onClose={() => setShowAddressForm(false)}
                                onSubmit={handleAddressFormSubmit}
                                isSubmitting={isAddressSubmitting}
                                hasOtherAddresses={addresses.length > 0}
                            />
                        )}

                        <Stack direction="row" justifyContent="flex-end" sx={{ mt: 2 }}>
                            <Button
                                variant="contained"
                                disabled={!selectedAddress}
                                onClick={handleNext}
                                size="large"
                                sx={{ px: 4 }}
                            >
                                Devam Et
                            </Button>
                        </Stack>
                    </Box>
                )}

                {/* ─── Adım 1: Kart Bilgileri ─── */}
                {activeStep === 1 && (
                    <Box>
                        <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
                            <CreditCardIcon color="primary" />
                            <Typography variant="h6">Kart Bilgileri</Typography>
                        </Stack>

                        <Alert severity="info" sx={{ mb: 3, fontSize: '0.8rem' }}>
                            Test kartı: <strong>5528790000000008</strong> — Ay: <strong>12</strong> — Yıl: <strong>30</strong> — CVC: <strong>123</strong>
                        </Alert>

                        <Stack spacing={2.5}>
                            <TextField
                                label="Kart Sahibi Adı Soyadı"
                                value={cardForm.holderName}
                                onChange={(e) => setCardForm((p) => ({ ...p, holderName: e.target.value }))}
                                inputProps={{ autoComplete: 'cc-name' }}
                                fullWidth
                                placeholder="AD SOYAD"
                            />

                            <TextField
                                label="Kart Numarası"
                                value={cardForm.number}
                                inputProps={{ maxLength: 19, autoComplete: 'cc-number', inputMode: 'numeric' }}
                                onChange={(e) => {
                                    const raw = e.target.value.replace(/\D/g, '').slice(0, 16);
                                    const formatted = raw.replace(/(.{4})/g, '$1 ').trim();
                                    setCardForm((p) => ({ ...p, number: formatted }));
                                }}
                                fullWidth
                                placeholder="0000 0000 0000 0000"
                            />

                            <Stack direction="row" spacing={2}>
                                <TextField
                                    label="Ay"
                                    value={cardForm.expireMonth}
                                    type="text"
                                    inputProps={{ maxLength: 2, autoComplete: 'cc-exp-month', inputMode: 'numeric' }}
                                    onChange={(e) => {
                                        const v = e.target.value.replace(/\D/g, '').slice(0, 2);
                                        setCardForm((p) => ({ ...p, expireMonth: v }));
                                    }}
                                    fullWidth
                                    placeholder="MM"
                                    helperText="01–12"
                                />
                                <TextField
                                    label="Yıl"
                                    value={cardForm.expireYear}
                                    type="text"
                                    inputProps={{ maxLength: 2, autoComplete: 'cc-exp-year', inputMode: 'numeric' }}
                                    onChange={(e) => {
                                        const v = e.target.value.replace(/\D/g, '').slice(0, 2);
                                        setCardForm((p) => ({ ...p, expireYear: v }));
                                    }}
                                    fullWidth
                                    placeholder="YY"
                                    helperText="Son 2 hane (örn: 30)"
                                />
                                <TextField
                                    label="CVC"
                                    value={cardForm.cvc}
                                    type="password"
                                    inputProps={{ maxLength: 3, autoComplete: 'cc-csc', inputMode: 'numeric' }}
                                    onChange={(e) => setCardForm((p) => ({ ...p, cvc: e.target.value.replace(/\D/g, '') }))}
                                    fullWidth
                                    placeholder="•••"
                                    helperText="Kart arkası"
                                />
                            </Stack>

                            <TextField
                                label="GSM Numarası"
                                value={cardForm.gsmNumber}
                                onChange={(e) => setCardForm((p) => ({ ...p, gsmNumber: e.target.value }))}
                                inputProps={{ autoComplete: 'tel', inputMode: 'tel' }}
                                fullWidth
                                placeholder="05XX XXX XX XX"
                                helperText="Ödeme bildirimi için"
                            />
                        </Stack>

                        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3 }}>
                            <Button onClick={handleBack} variant="outlined">Geri</Button>
                            <Button
                                variant="contained"
                                size="large"
                                sx={{ px: 4 }}
                                disabled={
                                    !cardForm.holderName ||
                                    cardForm.number.replace(/\s/g, '').length < 16 ||
                                    cardForm.expireMonth.length < 1 ||
                                    cardForm.expireYear.length < 2 ||
                                    cardForm.cvc.length < 3 ||
                                    !cardForm.gsmNumber
                                }
                                onClick={handleNext}
                            >
                                Devam Et
                            </Button>
                        </Stack>
                    </Box>
                )}

                {/* ─── Adım 2: Sipariş Özeti ─── */}
                {activeStep === 2 && (
                    <Box>
                        <Typography variant="h6" sx={{ mb: 2 }}>Sipariş Özeti</Typography>

                        <Stack spacing={2} sx={{ mb: 3 }}>
                            {/* Teslimat Adresi */}
                            <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">Teslimat Adresi</Typography>
                                        <Typography variant="body2" fontWeight="bold">
                                            {selectedAddress?.recipientName}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary">
                                            {selectedAddress?.line1}, {selectedAddress?.city}
                                        </Typography>
                                    </Box>
                                    <Chip label={selectedAddress?.label} size="small" variant="outlined" />
                                </Stack>
                            </Paper>

                            {/* Ödeme Kartı */}
                            <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                                <Typography variant="caption" color="text.secondary">Ödeme Kartı</Typography>
                                <Stack direction="row" alignItems="center" spacing={1} sx={{ mt: 0.5 }}>
                                    <CreditCardIcon fontSize="small" color="action" />
                                    <Typography variant="body2" fontWeight="bold" sx={{ fontFamily: 'monospace' }}>
                                        •••• •••• •••• {cardForm.number.replace(/\s/g, '').slice(-4)}
                                    </Typography>
                                    <Typography variant="caption" color="text.secondary">
                                        {cardForm.expireMonth}/{cardForm.expireYear}
                                    </Typography>
                                </Stack>
                                <Typography variant="caption" color="text.secondary">{cardForm.holderName}</Typography>
                            </Paper>
                        </Stack>

                        {/* Ürün Listesi */}
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            Ürünler ({basket.items.length})
                        </Typography>
                        <Stack spacing={1} sx={{ mb: 2 }}>
                            {basket.items.map((item) => (
                                <Stack
                                    key={item.productId}
                                    direction="row"
                                    justifyContent="space-between"
                                    alignItems="center"
                                >
                                    <Typography variant="body2">
                                        {item.productName}
                                        <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
                                            ×{item.quantity}
                                        </Typography>
                                    </Typography>
                                    <Typography variant="body2" fontWeight="bold">
                                        {formatPrice(item.price * item.quantity)}
                                    </Typography>
                                </Stack>
                            ))}
                        </Stack>

                        <Divider sx={{ my: 1.5 }} />
                        <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography fontWeight="bold">Toplam</Typography>
                            <Typography variant="h6" color="primary.main" fontWeight="bold">
                                {formatPrice(basket.totalPrice)}
                            </Typography>
                        </Stack>

                        {submitError && (
                            <Alert severity="error" sx={{ mt: 2 }}>{submitError}</Alert>
                        )}

                        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3 }}>
                            <Button onClick={handleBack} variant="outlined">Geri</Button>
                            <Button
                                variant="contained"
                                color="success"
                                size="large"
                                sx={{ px: 4 }}
                                disabled={createOrder.isPending || !tenantId}
                                onClick={handleSubmit}
                                startIcon={
                                    createOrder.isPending
                                        ? <CircularProgress size={18} color="inherit" />
                                        : null
                                }
                            >
                                {createOrder.isPending ? 'İşleniyor...' : 'Siparişi Onayla'}
                            </Button>
                        </Stack>
                    </Box>
                )}
            </Paper>
        </Box>
    );
};

export default CheckoutPage;
