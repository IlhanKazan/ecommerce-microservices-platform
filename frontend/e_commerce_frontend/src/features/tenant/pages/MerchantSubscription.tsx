import React, { useState } from 'react';
import {
    Box, Typography, Paper, Button, Chip, Divider,
    Stack, CircularProgress, Grid, Alert, Dialog, DialogTitle,
    DialogContent, DialogActions, TextField, Tooltip,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TablePagination
} from '@mui/material';
import {
    CheckCircle as CheckIcon,
    Star as StarIcon,
    ErrorOutline as ErrorIcon,
    CreditCard as CardIcon,
    Person as PersonIcon,
    Autorenew as AutoRenewIcon,
    History as HistoryIcon,
    Receipt as ReceiptIcon
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { tenantService } from '../api/tenantService.ts';
import type { SubscriptionPlan, PaymentCardInfo, PaymentStatus, PaymentType } from '../../../types/tenant';

const MerchantSubscription: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const queryClient = useQueryClient();

    const [retryModalOpen, setRetryModalOpen] = useState(false);
    const [retryPlanId, setRetryPlanId] = useState<number | null>(null);
    const [cardInfo, setCardInfo] = useState<PaymentCardInfo>({
        holderName: '', number: '', expireMonth: '', expireYear: '', cvc: ''
    });

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(5);

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

    const changePlanMutation = useMutation({
        mutationFn: async (planId: number) => {
            if (!activeTenant) throw new Error("Tenant yok");
            return tenantService.changeSubscriptionPlan(activeTenant.id, planId);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantSubscription', activeTenant?.id] });
            alert("Paket değişikliği başarıyla uygulandı.");
        },
        onError: () => alert("Paket değiştirilirken bir hata oluştu.")
    });

    const retryPaymentMutation = useMutation({
        mutationFn: async () => {
            if (!activeTenant) throw new Error("Tenant yok");
            if (!retryPlanId) throw new Error("Lütfen önce bir paket seçin.");

            return tenantService.retryPayment(activeTenant.id, retryPlanId, cardInfo);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenantSubscription', activeTenant?.id] });
            queryClient.invalidateQueries({ queryKey: ['tenantPaymentHistory', activeTenant?.id] });
            setRetryModalOpen(false);
            setRetryPlanId(null);
            setCardInfo({ holderName: '', number: '', expireMonth: '', expireYear: '', cvc: '' });
            alert("İşlem başarıyla tamamlandı!");
        },
        onError: () => alert("İşlem başarısız oldu. Lütfen kart bilgilerinizi kontrol edin.")
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
            if (window.confirm(`${plan.name} paketine geçmek istediğinize emin misiniz?`)) {
                changePlanMutation.mutate(plan.id);
            }
        }
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
                            startIcon={<CardIcon />}
                            onClick={() => handleRetryOpen()}
                            sx={{ px: 4, py: 1.5, borderRadius: 3, fontWeight: 'bold' }}
                        >
                            {!subDetail ? "Ödeme Yap / Abonelik Başlat" : (isPaymentFailed ? "Ödemeyi Yenile" : "Ödeme Yöntemini Güncelle")}
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            <Typography variant="h6" fontWeight="bold" sx={{ mb: 3 }}>Tüm Paketler</Typography>

            <Grid container spacing={3} sx={{ mb: 6 }}>
                {plans.map((plan) => {
                    const isCurrent = subDetail ? (plan.name === subDetail.planName) : false;
                    const isChangingThis = changePlanMutation.variables === plan.id && changePlanMutation.isPending;

                    return (
                        <Grid size={{ xs: 12, md: 4 }} key={plan.id}>
                            <Paper
                                sx={{
                                    p: 3, height: '100%', display: 'flex', flexDirection: 'column',
                                    borderRadius: 4,
                                    border: isCurrent ? '2px solid #38bdf8' : '1px solid #e2e8f0',
                                    boxShadow: isCurrent ? '0 0 20px rgba(56, 189, 248, 0.2)' : 'none',
                                    transition: 'transform 0.2s',
                                    '&:hover': { transform: 'translateY(-5px)' }
                                }}
                            >
                                <Typography variant="h5" fontWeight="bold">{plan.name}</Typography>
                                <Typography variant="h4" fontWeight="800" color="primary.main" sx={{ mt: 1 }}>
                                    {plan.price} {plan.currency}
                                    <Typography component="span" variant="body2" color="text.secondary">/{plan.billingCycle === 'MONTHLY' ? 'ay' : 'yıl'}</Typography>
                                </Typography>

                                <Divider sx={{ my: 2 }} />

                                <Stack spacing={1} sx={{ mt: 2, mb: 3, flexGrow: 1 }}>
                                    <Stack direction="row" alignItems="center" gap={1}>
                                        <CheckIcon color="success" fontSize="small" />
                                        <Typography variant="body2">Standart Özellikler</Typography>
                                    </Stack>
                                </Stack>

                                <Button
                                    variant={isCurrent ? "outlined" : "contained"}
                                    color={isCurrent ? "success" : "primary"}
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
                    <Alert severity="info" sx={{ mb: 3 }}>
                        Aboneliğinizi başlatmak veya devam ettirmek için güncel kart bilgilerinizi giriniz.
                    </Alert>
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
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={handleRetryClose} color="inherit" disabled={retryPaymentMutation.isPending}>
                        İptal
                    </Button>
                    <Button
                        onClick={() => retryPaymentMutation.mutate()}
                        variant="contained"
                        color="primary"
                        disabled={retryPaymentMutation.isPending || !cardInfo.number}
                    >
                        {retryPaymentMutation.isPending ? <CircularProgress size={24} color="inherit" /> : 'Ödemeyi Tamamla'}
                    </Button>
                </DialogActions>
            </Dialog>

        </Box>
    );
};

export default MerchantSubscription;