import React, { useState, useRef } from 'react';
import {
    Box, Typography, Paper, Button, Chip, Divider,
    Stack, CircularProgress, Grid, Alert, Dialog, DialogTitle,
    DialogContent, DialogActions, TextField, Tooltip, IconButton,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TablePagination
} from '@mui/material';
import {
    CheckCircle as CheckIcon,
    Star as StarIcon,
    StarBorder as StarBorderIcon,
    ErrorOutline as ErrorIcon,
    CreditCard as CardIcon,
    CreditCardOff as CreditCardOffIcon,
    AddCard as AddCardIcon,
    DeleteOutline as DeleteIcon,
    Person as PersonIcon,
    Autorenew as AutoRenewIcon,
    History as HistoryIcon,
    Receipt as ReceiptIcon
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useNotification } from '../../../components/shared/NotificationContext';
import { tenantService } from '../api/tenantService.ts';
import type { SubscriptionPlan, PaymentCardInfo, PaymentStatus, PaymentType, ChangePlanResult } from '../../../types/tenant';
import { PlanFeatureList } from '../../../components/shared/PlanFeatureList';
import { generateIdempotencyKey } from '../../../utils/idempotencyUtils';

/**
 * SECURITY: Sensitive card data is captured via refs at submit time,
 * never stored in React state to prevent exposure in DevTools
 */
interface CardInputRefs {
    holderName: React.RefObject<HTMLInputElement>;
    number: React.RefObject<HTMLInputElement>;
    expireMonth: React.RefObject<HTMLInputElement>;
    expireYear: React.RefObject<HTMLInputElement>;
    cvc: React.RefObject<HTMLInputElement>;
}

const MerchantSubscription: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const queryClient = useQueryClient();

    const [retryModalOpen, setRetryModalOpen] = useState(false);
    const [retryPlanId, setRetryPlanId] = useState<number | null>(null);

    // SECURITY: Use refs for sensitive payment data - never store in state
    const cardRefs: CardInputRefs = {
        holderName: useRef<HTMLInputElement>(null),
        number: useRef<HTMLInputElement>(null),
        expireMonth: useRef<HTMLInputElement>(null),
        expireYear: useRef<HTMLInputElement>(null),
        cvc: useRef<HTMLInputElement>(null),
    };

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(5);
    const { notify } = useNotification();

    const [planChangeDialog, setPlanChangeDialog] = useState<{ open: boolean; plan?: SubscriptionPlan | null}>({
        open: false,
        plan: null
    });

    const { data, isLoading, isError } = useQuery({
        queryKey: ['tenantSubscription', activeTenant?.id],
        queryFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            const [plans, subDetail] = await Promise.all([
                tenantService.getSubscriptionPlans(),
                tenantService.getSubscriptionDetails(activeTenant.id).catch((err) => {
                    console.warn("Abonelik detayı bulunamadı:", err);
                    return null;
                })
            ]);
            return { plans, subDetail };
        },
        enabled: !!activeTenant
    });

    const { data: historyData, isLoading: historyLoading } = useQuery({
        queryKey: ['tenantPaymentHistory', activeTenant?.id, page, rowsPerPage],
        queryFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.getPaymentHistory(activeTenant.id, page, rowsPerPage);
        },
        enabled: !!activeTenant
    });

    const { data: cards } = useQuery({
        queryKey: ['tenantCards', activeTenant?.id],
        queryFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.getCards(activeTenant.id);
        },
        enabled: !!activeTenant
    });

    const [addCardOpen, setAddCardOpen] = useState(false);
    const newCardRefs: Omit<CardInputRefs, 'cvc'> & { alias: React.RefObject<HTMLInputElement> } = {
        alias: useRef<HTMLInputElement>(null),
        holderName: useRef<HTMLInputElement>(null),
        number: useRef<HTMLInputElement>(null),
        expireMonth: useRef<HTMLInputElement>(null),
        expireYear: useRef<HTMLInputElement>(null),
    };

    const changePlanMutation = useMutation({
        mutationFn: async (planId: number) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.changeSubscriptionPlan(activeTenant.id, planId, generateIdempotencyKey());
        },
        onSuccess: (result: ChangePlanResult) => {
            queryClient.invalidateQueries({ queryKey: ['tenantSubscription', activeTenant?.id] });
            queryClient.invalidateQueries({ queryKey: ['tenantPaymentHistory', activeTenant?.id] });
            if (result.changeType === 'UPGRADE') {
                const charged = result.chargedAmount > 0 ? ` Kalan döneme göre ${result.chargedAmount} ₺ tahsil edildi.` : '';
                notify(`${result.newPlanName} paketine yükseltildiniz.${charged}`, 'success');
            } else if (result.changeType === 'DOWNGRADE_SCHEDULED') {
                notify(`${result.newPlanName} paketine geçiş ${formatDate(result.effectiveDate)} tarihinde uygulanacak.`, 'success');
            } else {
                notify('Bekleyen paket değişikliği iptal edildi.', 'success');
            }
        },
        onError: (error: unknown) => {
            const err = error as { response?: { data?: { errorCode?: string; message?: string } } };
            const code = err.response?.data?.errorCode;
            if (code === 'NO_DEFAULT_CARD') {
                notify('Üst plana geçmek için önce bir kart eklemelisiniz.', 'error');
                setAddCardOpen(true);
                return;
            }
            notify(err.response?.data?.message || 'Paket değiştirilirken bir hata oluştu.', 'error');
        }
    });

    const addCardMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            const cardNumber = newCardRefs.number.current?.value?.trim() || '';
            const holderName = newCardRefs.holderName.current?.value?.trim() || '';
            const expireMonth = newCardRefs.expireMonth.current?.value?.trim() || '';
            const expireYear = newCardRefs.expireYear.current?.value?.trim() || '';
            const cardAlias = newCardRefs.alias.current?.value?.trim() || '';
            if (!cardNumber || !holderName || !expireMonth || !expireYear) {
                throw new Error('Lütfen kart bilgilerini eksiksiz doldurun.');
            }
            return tenantService.addCard(
                { tenantId: activeTenant.id, cardAlias, cardHolderName: holderName, cardNumber, expireMonth, expireYear },
                generateIdempotencyKey(),
            );
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantCards', activeTenant?.id] });
            if (newCardRefs.number.current) newCardRefs.number.current.value = '';
            setAddCardOpen(false);
            notify('Kart başarıyla eklendi.', 'success');
        },
        onError: (error: unknown) => {
            const err = error as { response?: { data?: { message?: string } }; message?: string };
            notify(err.response?.data?.message || err.message || 'Kart eklenemedi.', 'error');
        }
    });

    const deleteCardMutation = useMutation({
        mutationFn: async (cardId: number) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.deleteCard(activeTenant.id, cardId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantCards', activeTenant?.id] });
            notify('Kart silindi.', 'success');
        },
        onError: (error: unknown) => {
            const err = error as { response?: { data?: { errorCode?: string; message?: string } } };
            if (err.response?.data?.errorCode === 'LAST_CARD_ON_ACTIVE_SUBSCRIPTION') {
                notify('Aktif aboneliğinizin tek kartını silemezsiniz. Önce başka bir kart ekleyin.', 'error');
                return;
            }
            notify(err.response?.data?.message || 'Kart silinemedi.', 'error');
        }
    });

    const setDefaultCardMutation = useMutation({
        mutationFn: async (cardId: number) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.setDefaultCard(activeTenant.id, cardId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantCards', activeTenant?.id] });
            notify('Varsayılan kart güncellendi.', 'success');
        },
        onError: () => notify('Varsayılan kart güncellenemedi.', 'error')
    });

    const retryPaymentMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            if (!retryPlanId) throw new Error("Lütfen önce bir paket seçin.");

            // SECURITY: Capture sensitive data from refs at submit time only
            const cardNumber = cardRefs.number.current?.value?.trim() || '';
            const cardCvc = cardRefs.cvc.current?.value?.trim() || '';
            const cardHolder = cardRefs.holderName.current?.value?.trim() || '';
            const cardMonth = cardRefs.expireMonth.current?.value?.trim() || '';
            const cardYear = cardRefs.expireYear.current?.value?.trim() || '';

            if (!cardNumber || !cardCvc || !cardHolder || !cardMonth || !cardYear) {
                throw new Error('Lütfen tüm kart bilgilerini doldurunuz.');
            }

            const cardInfo: PaymentCardInfo = {
                holderName: cardHolder,
                number: cardNumber,
                expireMonth: cardMonth,
                expireYear: cardYear,
                cvc: cardCvc,
            };

            return tenantService.retryPayment(activeTenant.id, retryPlanId, cardInfo);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantSubscription', activeTenant?.id] });
            queryClient.invalidateQueries({ queryKey: ['tenantPaymentHistory', activeTenant?.id] });
            
            // SECURITY: Clear sensitive data after sending
            if (cardRefs.number.current) cardRefs.number.current.value = '';
            if (cardRefs.cvc.current) cardRefs.cvc.current.value = '';
            
            setRetryModalOpen(false);
            setRetryPlanId(null);
            notify('İşlem başarıyla tamamlandı!', 'success');
        },
        onError: (error: unknown) => {
            const err = error as { response?: { data?: { message?: string } }; message?: string };
            const message = err.response?.data?.message || err.message || "İşlem başarısız oldu. Lütfen kart bilgilerinizi kontrol edin.";
            notify(message, 'error');
        }
    });

    const handleRetryOpen = (overridePlanId?: number) => {
        if (overridePlanId) {
            setRetryPlanId(overridePlanId);
        } else if (data?.subDetail && data?.plans) {
            const currentPlan = data.plans.find(p => p.name === data.subDetail.planName);
            setRetryPlanId(currentPlan?.id || null);
        }
        setRetryModalOpen(true);
    };

    const handleChangePlan = (plan: SubscriptionPlan) => {
        if (!data?.subDetail) {
            handleRetryOpen(plan.id);
            return;
        }

        if (plan.name !== data.subDetail.planName) {
            setPlanChangeDialog({ open: true, plan });
        }
    };

    const confirmChangePlan = () => {
        if (planChangeDialog.plan) {
            changePlanMutation.mutate(planChangeDialog.plan.id);
        }
        setPlanChangeDialog({ open: false, plan: null });
    };

    const cancelChangePlan = () => {
        setPlanChangeDialog({ open: false, plan: null });
    };

    const handleRetryClose = () => {
        setRetryModalOpen(false);
        setRetryPlanId(null);
    };

    const formatDate = (dateString?: string, includeTime: boolean = false) => {
        if (!dateString) return '-';
        const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'long', year: 'numeric' };
        if (includeTime) {
            options.hour = '2-digit';
            options.minute = '2-digit';
        }
        return new Date(dateString).toLocaleDateString('tr-TR', options);
    };

    const getStatusChip = (status: PaymentStatus) => {
        switch (status) {
            case 'SUCCESS': return <Chip label="BAŞARILI" color="success" size="small" variant="outlined" />;
            case 'FAILURE': return <Chip label="BAŞARISIZ" color="error" size="small" variant="outlined" />;
            case 'PENDING': return <Chip label="BEKLİYOR" color="warning" size="small" variant="outlined" />;
            case 'REFUNDED': return <Chip label="İADE EDİLDİ" color="default" size="small" variant="outlined" />;
            default: return <Chip label={status} size="small" />;
        }
    };

    const getTypeLabel = (type: PaymentType) => {
        return type === 'SUBSCRIPTION' ? 'Abonelik Ücreti' : 'Sipariş Ödemesi';
    };

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    if (isLoading) return <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}><CircularProgress /></Box>;
    if (isError || !data) return <Alert severity="error">Paket bilgileri yüklenemedi.</Alert>;

    const { plans, subDetail } = data;

    const isPaymentFailed = !subDetail || subDetail.status === 'PAYMENT_FAILED' || subDetail.failedPaymentCount > 0;

    return (
        <Box sx={{ maxWidth: 1200, mx: 'auto', p: 2 }}>

            {isPaymentFailed && (
                <Alert
                    severity="error"
                    icon={<ErrorIcon fontSize="inherit" />}
                    action={
                        <Button color="inherit" size="small" onClick={() => handleRetryOpen()}>
                            ÖDEMEYİ TAMAMLA
                        </Button>
                    }
                    sx={{ mb: 4, borderRadius: 2, alignItems: 'center' }}
                >
                    <Typography variant="subtitle2" fontWeight="bold">
                        Ödeme Alınamadı veya Abonelik Bulunamadı!
                    </Typography>
                    <Typography variant="body2">
                        {subDetail?.failedPaymentCount ? `${subDetail.failedPaymentCount} kez ödeme denendi ancak alınamadı. ` : ''}
                        Mağazanızın satışa açılabilmesi için kart bilgilerinizi girerek ödemenizi tamamlayın veya yeni bir paket seçin.
                    </Typography>
                </Alert>
            )}

            <Box sx={{ mb: 4 }}>
                <Typography variant="h5" fontWeight="bold">Abonelik & Paketler</Typography>
                <Typography color="text.secondary">Mağaza limitlerinizi ve ödeme planınızı yönetin.</Typography>
            </Box>

            <Paper
                sx={{
                    p: 4, mb: 5, borderRadius: 4,
                    background: isPaymentFailed
                        ? 'linear-gradient(135deg, #450a0a 0%, #7f1d1d 100%)'
                        : 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
                    color: '#fff',
                    position: 'relative',
                    overflow: 'hidden'
                }}
            >
                <StarIcon sx={{ position: 'absolute', right: -20, top: -20, fontSize: 150, opacity: 0.1, color: isPaymentFailed ? '#fca5a5' : '#fbbf24' }} />

                <Grid container alignItems="center" spacing={4}>
                    <Grid size={{ xs: 12, md: 7 }}>
                        <Typography variant="overline" sx={{ opacity: 0.7, letterSpacing: 1 }}>MEVCUT PAKETİNİZ</Typography>
                        <Typography variant="h3" fontWeight="bold" sx={{ color: isPaymentFailed ? '#f87171' : '#38bdf8', mb: 1 }}>
                            {subDetail?.planName || 'Abonelik Yok'}
                        </Typography>

                        <Stack direction="row" spacing={3} mt={3}>
                            <Stack>
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>DURUM</Typography>
                                <Chip
                                    label={!subDetail ? "YOK" : (isPaymentFailed ? "ÖDEME BEKLİYOR" : "AKTİF")}
                                    color={isPaymentFailed ? "error" : "success"}
                                    size="small"
                                    sx={{ width: 'fit-content', mt: 0.5 }}
                                />
                            </Stack>

                            <Stack>
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>YENİLEME TARİHİ</Typography>
                                <Typography variant="body1" fontWeight="bold">
                                    {subDetail ? formatDate(subDetail.nextBillingDate) : '-'}
                                </Typography>
                            </Stack>

                            <Stack>
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>TUTAR</Typography>
                                <Typography variant="body1" fontWeight="bold">
                                    {subDetail?.feeAmount || 0} {subDetail?.currency || '₺'} / {subDetail?.cycleUnit === 'MONTHLY' ? 'Ay' : 'Yıl'}
                                </Typography>
                            </Stack>
                        </Stack>

                        <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.1)' }} />

                        {subDetail && (
                            <Stack direction="row" spacing={3} alignItems="center">
                                {subDetail.autoRenew && (
                                    <Tooltip title="Bu paket dönem sonunda otomatik yenilenir">
                                        <Stack direction="row" alignItems="center" gap={0.5} sx={{ opacity: 0.8 }}>
                                            <AutoRenewIcon fontSize="small" />
                                            <Typography variant="caption">Otomatik Yenileme Açık</Typography>
                                        </Stack>
                                    </Tooltip>
                                )}
                                <Stack direction="row" alignItems="center" gap={0.5} sx={{ opacity: 0.8 }}>
                                    <HistoryIcon fontSize="small" />
                                    <Typography variant="caption">
                                        Son Başarılı Ödeme: {formatDate(subDetail.lastSuccessfulPaymentDate)}
                                    </Typography>
                                </Stack>
                            </Stack>
                        )}

                    </Grid>

                    <Grid size={{ xs: 12, md: 5 }} sx={{ textAlign: { md: 'right' } }}>
                        <Button
                            variant="contained"
                            color={isPaymentFailed ? "error" : "info"}
                            size="large"
                            startIcon={subDetail && !isPaymentFailed ? <AddCardIcon /> : <CardIcon />}
                            onClick={() => (subDetail && !isPaymentFailed) ? setAddCardOpen(true) : handleRetryOpen()}
                            sx={{ px: 4, py: 1.5, borderRadius: 3, fontWeight: 'bold' }}
                        >
                            {!subDetail ? "Ödeme Yap / Abonelik Başlat" : (isPaymentFailed ? "Ödemeyi Yenile" : "Kart Ekle / Güncelle")}
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            <Box sx={{ mb: 5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <CardIcon color="primary" />
                        <Typography variant="h6" fontWeight="bold">Kayıtlı Kartlar</Typography>
                    </Box>
                    <Button startIcon={<AddCardIcon />} variant="outlined" onClick={() => setAddCardOpen(true)}>
                        Kart Ekle
                    </Button>
                </Box>

                <Paper variant="outlined" sx={{ borderRadius: 3, p: cards && cards.length > 0 ? 1 : 4 }}>
                    {!cards || cards.length === 0 ? (
                        <Stack alignItems="center" spacing={1} sx={{ py: 2, color: 'text.secondary' }}>
                            <CreditCardOffIcon fontSize="large" />
                            <Typography variant="body2">Henüz kayıtlı kartınız yok.</Typography>
                            <Typography variant="caption">Otomatik yenileme ve plan yükseltme için bir kart ekleyin.</Typography>
                        </Stack>
                    ) : (
                        <Stack divider={<Divider flexItem />}>
                            {cards.map((card) => (
                                <Stack key={card.id} direction="row" alignItems="center" spacing={2} sx={{ px: 2, py: 1.5 }}>
                                    <CardIcon color="action" />
                                    <Box sx={{ flexGrow: 1 }}>
                                        <Typography fontWeight="bold">
                                            •••• {card.lastFour || '----'}
                                            {card.cardAssociation && (
                                                <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                                                    {card.cardAssociation}{card.cardFamily ? ` · ${card.cardFamily}` : ''}
                                                </Typography>
                                            )}
                                        </Typography>
                                        {card.cardAlias && (
                                            <Typography variant="caption" color="text.secondary">{card.cardAlias}</Typography>
                                        )}
                                    </Box>
                                    {card.isDefault ? (
                                        <Chip icon={<StarIcon sx={{ fontSize: 16 }} />} label="Varsayılan" color="primary" size="small" />
                                    ) : (
                                        <Tooltip title="Varsayılan yap">
                                            <span>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => setDefaultCardMutation.mutate(card.id)}
                                                    disabled={setDefaultCardMutation.isPending}
                                                >
                                                    <StarBorderIcon />
                                                </IconButton>
                                            </span>
                                        </Tooltip>
                                    )}
                                    <Tooltip title="Kartı sil">
                                        <span>
                                            <IconButton
                                                size="small"
                                                color="error"
                                                onClick={() => deleteCardMutation.mutate(card.id)}
                                                disabled={deleteCardMutation.isPending}
                                            >
                                                <DeleteIcon />
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                </Stack>
                            ))}
                        </Stack>
                    )}
                </Paper>
            </Box>

            <Typography variant="h6" fontWeight="bold" sx={{ mb: 3 }}>Tüm Paketler</Typography>

            <Grid container spacing={3} sx={{ mb: 6 }}>
                {plans.map((plan) => {
                    const isCurrent = subDetail ? (plan.name === subDetail.planName) : false;
                    const isChangingThis = changePlanMutation.variables === plan.id && changePlanMutation.isPending;
                    const highlight = plan.name === 'Büyüme';

                    return (
                        <Grid size={{ xs: 12, sm: 6, md: 3 }} key={plan.id}>
                            <Paper
                                sx={{
                                    p: 3, height: '100%', display: 'flex', flexDirection: 'column',
                                    borderRadius: 4,
                                    border: isCurrent ? '2px solid #38bdf8' : highlight ? '2px solid #818cf8' : '1px solid #e2e8f0',
                                    boxShadow: isCurrent ? '0 0 20px rgba(56, 189, 248, 0.2)' : highlight ? '0 0 16px rgba(129,140,248,0.15)' : 'none',
                                    transition: 'transform 0.2s',
                                    position: 'relative',
                                    overflow: 'hidden',
                                    '&:hover': { transform: 'translateY(-5px)' }
                                }}
                            >
                                {highlight && !isCurrent && (
                                    <Chip
                                        label="En Popüler"
                                        size="small"
                                        sx={{
                                            position: 'absolute', top: 12, right: 12,
                                            bgcolor: '#818cf8', color: '#fff', fontWeight: 'bold', fontSize: 11
                                        }}
                                    />
                                )}
                                {isCurrent && (
                                    <Chip
                                        icon={<CheckIcon sx={{ fontSize: 14 }} />}
                                        label="Mevcut Paket"
                                        size="small"
                                        color="info"
                                        sx={{ position: 'absolute', top: 12, right: 12, fontWeight: 'bold', fontSize: 11 }}
                                    />
                                )}

                                <Typography variant="h6" fontWeight="bold" sx={{ pr: isCurrent || highlight ? 10 : 0 }}>
                                    {plan.name}
                                </Typography>
                                <Typography variant="h4" fontWeight="800" color="primary.main" sx={{ mt: 0.5 }}>
                                    {plan.price === 0 ? 'Ücretsiz' : `${plan.price} ₺`}
                                    {plan.price > 0 && (
                                        <Typography component="span" variant="body2" color="text.secondary">
                                            /{plan.billingCycle === 'MONTHLY' ? 'ay' : 'yıl'}
                                        </Typography>
                                    )}
                                </Typography>

                                <Divider sx={{ my: 2 }} />

                                <Box sx={{ flexGrow: 1, mb: 3 }}>
                                    <PlanFeatureList
                                        featuresJson={plan.features}
                                        commissionRate={plan.commissionRate}
                                        compact
                                    />
                                </Box>

                                <Button
                                    variant={isCurrent ? "outlined" : "contained"}
                                    color={isCurrent ? "success" : highlight ? "secondary" : "primary"}
                                    disabled={isCurrent || changePlanMutation.isPending}
                                    onClick={() => handleChangePlan(plan)}
                                    fullWidth
                                    sx={{ mt: 'auto', borderRadius: 3, py: 1.5 }}
                                >
                                    {isChangingThis
                                        ? <CircularProgress size={24} color="inherit" />
                                        : (isCurrent
                                                ? 'Kullanılan Paket'
                                                : (!subDetail ? 'Seç ve Başla' : 'Pakete Geç')
                                        )
                                    }
                                </Button>
                            </Paper>
                        </Grid>
                    );
                })}
            </Grid>

            <Box sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <ReceiptIcon color="primary" />
                <Typography variant="h6" fontWeight="bold">Ödeme Geçmişi</Typography>
            </Box>

            <Paper variant="outlined" sx={{ borderRadius: 4, overflow: 'hidden' }}>
                <TableContainer>
                    <Table sx={{ minWidth: 650 }}>
                        <TableHead sx={{ bgcolor: '#f8fafc' }}>
                            <TableRow>
                                <TableCell><b>İşlem ID</b></TableCell>
                                <TableCell><b>Tarih</b></TableCell>
                                <TableCell><b>İşlem Tipi</b></TableCell>
                                <TableCell><b>Açıklama</b></TableCell>
                                <TableCell align="right"><b>Tutar</b></TableCell>
                                <TableCell align="center"><b>Durum</b></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {historyLoading ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center" sx={{ py: 5 }}>
                                        <CircularProgress size={30} />
                                    </TableCell>
                                </TableRow>
                            ) : historyData?.content.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                                        Henüz bir ödeme geçmişi bulunmamaktadır.
                                    </TableCell>
                                </TableRow>
                            ) : (
                                historyData?.content.map((row) => (
                                    <TableRow key={row.paymentId} hover>
                                        <TableCell>#{row.paymentId}</TableCell>
                                        <TableCell>{formatDate(row.transactionDate, true)}</TableCell>
                                        <TableCell>{getTypeLabel(row.paymentType)}</TableCell>
                                        <TableCell>{row.description}</TableCell>
                                        <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                                            {row.amount} {row.currency}
                                        </TableCell>
                                        <TableCell align="center">
                                            {getStatusChip(row.paymentStatus)}
                                        </TableCell>
                                    </TableRow>
                                ))
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>

                {historyData && historyData.totalElements > 0 && (
                    <TablePagination
                        component="div"
                        count={historyData.totalElements}
                        page={page}
                        onPageChange={handleChangePage}
                        rowsPerPage={rowsPerPage}
                        onRowsPerPageChange={handleChangeRowsPerPage}
                        rowsPerPageOptions={[5, 10, 25]}
                        labelRowsPerPage="Satır sayısı:"
                        labelDisplayedRows={({ from, to, count }) => `${from}-${to} / ${count}`}
                    />
                )}
            </Paper>


            <Dialog open={retryModalOpen} onClose={handleRetryClose} maxWidth="sm" fullWidth>
                <DialogTitle sx={{ fontWeight: 'bold' }}>Ödeme Yöntemini Güncelle</DialogTitle>
                <DialogContent dividers>
                    <Alert severity="warning" sx={{ mb: 3 }}>
                        <Typography variant="body2" fontWeight="bold">🔒 Güvenlik Uyarısı:</Typography>
                        <Typography variant="caption">
                            Kart numarası ve CVC güvenli şekilde işlenir. Dialog kapandığında verileriniz otomatik olarak silinir.
                        </Typography>
                    </Alert>
                    <Grid container spacing={2}>
                        <Grid size={12}>
                            <TextField label="Kart Üzerindeki İsim" fullWidth required
                                       defaultValue=""
                                       inputRef={cardRefs.holderName}
                                       InputProps={{ startAdornment: <PersonIcon sx={{mr:1, color:'text.secondary'}} /> }}
                            />
                        </Grid>
                        <Grid size={12}>
                            <TextField label="Kart Numarası" fullWidth required placeholder="0000 0000 0000 0000"
                                       defaultValue=""
                                       inputRef={cardRefs.number}
                                       autoComplete="cc-number"
                                       InputProps={{ startAdornment: <CardIcon sx={{mr:1, color:'text.secondary'}} /> }}
                            />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="Ay (MM)" fullWidth required placeholder="01"
                                       defaultValue=""
                                       inputRef={cardRefs.expireMonth}
                                       autoComplete="cc-exp-month"
                            />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="Yıl (YY)" fullWidth required placeholder="28"
                                       defaultValue=""
                                       inputRef={cardRefs.expireYear}
                                       autoComplete="cc-exp-year"
                            />
                        </Grid>
                        <Grid size={{ xs: 6, md: 4 }}>
                            <TextField label="CVC" fullWidth required type="password" placeholder="123"
                                       defaultValue=""
                                       inputRef={cardRefs.cvc}
                                       autoComplete="cc-csc"
                                       helperText="Kartın arka tarafındaki 3 haneli kod"
                            />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleRetryClose} color="inherit" disabled={retryPaymentMutation.isPending}>
                        İptal
                    </Button>
                    <Button
                        onClick={() => retryPaymentMutation.mutate()}
                        variant="contained"
                        color="primary"
                        disabled={retryPaymentMutation.isPending}
                    >
                        {retryPaymentMutation.isPending ? <CircularProgress size={24} color="inherit" /> : 'Ödemeyi Tamamla'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog
                open={planChangeDialog.open}
                onClose={cancelChangePlan}
                maxWidth="sm"
                fullWidth
            >
                <DialogTitle sx={{ fontWeight: 'bold' }}>Paket Değişikliğini Onayla</DialogTitle>
                <DialogContent>
                    {planChangeDialog.plan ? (() => {
                        const isUpgrade = !!subDetail && planChangeDialog.plan!.price > subDetail.feeAmount;
                        return (
                            <>
                                <Typography sx={{ mb: 1 }}>
                                    <strong>{planChangeDialog.plan!.name}</strong> paketine geçmek istediğinize emin misiniz?
                                </Typography>
                                {isUpgrade ? (
                                    <Alert severity="info" sx={{ mt: 1 }}>
                                        Üst plana geçiyorsunuz. Kalan döneme göre hesaplanan fiyat farkı kayıtlı
                                        varsayılan kartınızdan <strong>hemen tahsil edilecek</strong>; yenileme tarihiniz değişmez.
                                        {(!cards || cards.length === 0) && ' Önce bir kart eklemeniz gerekir.'}
                                    </Alert>
                                ) : (
                                    <Alert severity="info" sx={{ mt: 1 }}>
                                        Alt plana geçiyorsunuz. Değişiklik <strong>bu dönemin sonunda</strong>
                                        {subDetail ? ` (${formatDate(subDetail.nextBillingDate)})` : ''} yürürlüğe girecek;
                                        şimdi herhangi bir tahsilat yapılmaz.
                                    </Alert>
                                )}
                            </>
                        );
                    })() : (
                        <Typography>Paket bilgisi alınamadı.</Typography>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={cancelChangePlan} color="inherit">İptal</Button>
                    <Button onClick={confirmChangePlan} variant="contained" color="primary" disabled={changePlanMutation.isPending}>
                        {changePlanMutation.isPending ? 'Uygulanıyor...' : 'Onayla'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={addCardOpen} onClose={() => setAddCardOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle sx={{ fontWeight: 'bold' }}>Yeni Kart Ekle</DialogTitle>
                <DialogContent dividers>
                    <Alert severity="info" sx={{ mb: 3 }}>
                        <Typography variant="caption">
                            Kart bilgileriniz güvenli şekilde iyzico'da saklanır; sistemimizde kart numarası tutulmaz.
                            Sonraki abonelik ödemeleri varsayılan kartınızdan otomatik tahsil edilir.
                        </Typography>
                    </Alert>
                    <Grid container spacing={2}>
                        <Grid size={12}>
                            <TextField label="Kart Adı (opsiyonel)" fullWidth placeholder="İş kartım"
                                       defaultValue="" inputRef={newCardRefs.alias} />
                        </Grid>
                        <Grid size={12}>
                            <TextField label="Kart Üzerindeki İsim" fullWidth required defaultValue=""
                                       inputRef={newCardRefs.holderName}
                                       InputProps={{ startAdornment: <PersonIcon sx={{ mr: 1, color: 'text.secondary' }} /> }} />
                        </Grid>
                        <Grid size={12}>
                            <TextField label="Kart Numarası" fullWidth required placeholder="0000 0000 0000 0000"
                                       defaultValue="" inputRef={newCardRefs.number} autoComplete="cc-number"
                                       InputProps={{ startAdornment: <CardIcon sx={{ mr: 1, color: 'text.secondary' }} /> }} />
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                            <TextField label="Ay (MM)" fullWidth required placeholder="01"
                                       defaultValue="" inputRef={newCardRefs.expireMonth} autoComplete="cc-exp-month" />
                        </Grid>
                        <Grid size={{ xs: 6 }}>
                            <TextField label="Yıl (YYYY)" fullWidth required placeholder="2030"
                                       defaultValue="" inputRef={newCardRefs.expireYear} autoComplete="cc-exp-year" />
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={() => setAddCardOpen(false)} color="inherit" disabled={addCardMutation.isPending}>
                        İptal
                    </Button>
                    <Button onClick={() => addCardMutation.mutate()} variant="contained" disabled={addCardMutation.isPending}>
                        {addCardMutation.isPending ? <CircularProgress size={24} color="inherit" /> : 'Kartı Kaydet'}
                    </Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
};

export default MerchantSubscription;