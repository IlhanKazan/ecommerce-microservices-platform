import React, { useEffect, useState } from 'react';
import {
    Box, Typography, Paper, Button, Chip, Divider,
    Stack, CircularProgress, Grid
} from '@mui/material';
import { CheckCircle as CheckIcon, Star as StarIcon } from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { tenantService } from '../../../service/tenantService';
import type { SubscriptionPlan } from '../../../types/tenant';

const MerchantSubscription: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
    const [loading, setLoading] = useState(true);
    const [changingPlan, setChangingPlan] = useState<number | null>(null);
    // TODO [08.02.2026 22:56]: mock data
    const currentPlanId = 1;
    const nextBillingDate = "25.01.2026";

    useEffect(() => {
        const loadPlans = async () => {
            try {
                const data = await tenantService.getPlans();
                setPlans(data);
            } catch (error) {
                console.error("Planlar yüklenemedi", error);
            } finally {
                setLoading(false);
            }
        };
        loadPlans();
    }, []);

    const handleChangePlan = async (plan: SubscriptionPlan) => {
        if (!activeTenant || plan.id === currentPlanId) return;

        if (window.confirm(`${plan.name} paketine geçmek istediğinize emin misiniz?`)) {
            setChangingPlan(plan.id);
            try {
                await tenantService.changeSubscriptionPlan(activeTenant.id, plan.id);
                alert("Paket değişikliği talebiniz alındı.");
            } catch (error) {
                alert("Paket değiştirilemedi." + error);
            } finally {
                setChangingPlan(null);
            }
        }
    };

    const renderFeatures = (featuresString: string) => {
        let content: string[];
        try {
            content = JSON.parse(featuresString);
            if (!Array.isArray(content)) content = [featuresString];
        } catch { content = [featuresString]; }

        return (
            <Stack spacing={1} sx={{ mt: 2, mb: 3, flexGrow: 1 }}>
                {content.map((feature, idx) => (
                    <Stack direction="row" alignItems="center" gap={1} key={idx}>
                        <CheckIcon color="success" fontSize="small" />
                        <Typography variant="body2">{feature}</Typography>
                    </Stack>
                ))}
            </Stack>
        );
    };

    if (loading) return <CircularProgress />;

    return (
        <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
            <Box sx={{ mb: 4 }}>
                <Typography variant="h5" fontWeight="bold">Abonelik & Paketler</Typography>
                <Typography color="text.secondary">Mağaza limitlerinizi ve ödeme planınızı yönetin.</Typography>
            </Box>

            <Paper
                sx={{
                    p: 4, mb: 5, borderRadius: 4,
                    background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
                    color: '#fff',
                    position: 'relative',
                    overflow: 'hidden'
                }}
            >
                <StarIcon sx={{ position: 'absolute', right: -20, top: -20, fontSize: 150, opacity: 0.1, color: '#fbbf24' }} />

                <Grid container alignItems="center" spacing={4}>
                    <Grid size={{ xs: 12, md: 8 }}>
                        <Typography variant="overline" sx={{ opacity: 0.7, letterSpacing: 1 }}>MEVCUT PAKETİNİZ</Typography>
                        <Typography variant="h3" fontWeight="bold" sx={{ color: '#38bdf8', mb: 1 }}>
                            {plans.find(p => p.id === currentPlanId)?.name || 'Standart Paket'}
                        </Typography>
                        <Stack direction="row" gap={2} alignItems="center" mt={2}>
                            <Chip label="AKTİF" color="success" size="small" />
                            <Typography variant="body2" sx={{ opacity: 0.8 }}>
                                Sonraki Ödeme: <b>{nextBillingDate}</b>
                            </Typography>
                        </Stack>
                    </Grid>
                    <Grid size={{ xs: 12, md: 4 }} sx={{ textAlign: { md: 'right' } }}>
                        <Button variant="contained" color="info" size="large">
                            Ödeme Yöntemini Güncelle
                        </Button>
                    </Grid>
                </Grid>
            </Paper>

            <Typography variant="h6" fontWeight="bold" sx={{ mb: 3 }}>Tüm Paketler</Typography>

            <Grid container spacing={3}>
                {plans.map((plan) => {
                    const isCurrent = plan.id === currentPlanId;
                    return (
                        <Grid size={{ xs: 12, md: 4 }} key={plan.id}>
                            <Paper
                                sx={{
                                    p: 3, height: '100%', display: 'flex', flexDirection: 'column',
                                    borderRadius: 4,
                                    border: isCurrent ? '2px solid #38bdf8' : '1px solid #e2e8f0',
                                    boxShadow: isCurrent ? '0 0 20px rgba(56, 189, 248, 0.2)' : 'none'
                                }}
                            >
                                <Typography variant="h5" fontWeight="bold">{plan.name}</Typography>
                                <Typography variant="h4" fontWeight="800" color="primary.main" sx={{ mt: 1 }}>
                                    {plan.price} {plan.currency}
                                    <Typography component="span" variant="body2" color="text.secondary">/{plan.billingCycle === 'MONTHLY' ? 'ay' : 'yıl'}</Typography>
                                </Typography>

                                <Divider sx={{ my: 2 }} />

                                {renderFeatures(plan.features)}

                                <Button
                                    variant={isCurrent ? "outlined" : "contained"}
                                    color={isCurrent ? "success" : "primary"}
                                    disabled={isCurrent || changingPlan === plan.id}
                                    onClick={() => handleChangePlan(plan)}
                                    fullWidth
                                    sx={{ mt: 'auto', borderRadius: 3, py: 1.5 }}
                                >
                                    {changingPlan === plan.id ? <CircularProgress size={24} /> : (isCurrent ? 'Kullanılan Paket' : 'Pakete Geç')}
                                </Button>
                            </Paper>
                        </Grid>
                    );
                })}
            </Grid>
        </Box>
    );
};

export default MerchantSubscription;