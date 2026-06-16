import React, { useMemo } from 'react';
import { Box, Paper, Typography, Stack, Grid, CircularProgress } from '@mui/material';
import { BarChart } from '@mui/x-charts/BarChart';
import {
    Store as StoreIcon,
    Inventory2 as ProductIcon,
    ShoppingBag as OrderIcon,
    People as UsersIcon,
    Payments as RevenueIcon,
} from '@mui/icons-material';
import {
    useAdminPaymentStats, useAdminProductStats, useAdminOrderStats,
    useAdminStores, useAdminUsers,
} from '../../../../query/useAdminQueries';
import { formatPrice } from '../../../../utils/formatPrice';

const STATUS_LABELS: Record<string, string> = {
    CONFIRMED: 'Onaylandı',
    SHIPPED: 'Kargoda',
    DELIVERED: 'Teslim',
    CANCELLED: 'İptal',
    REFUNDED: 'İade',
};

interface StatCardProps {
    label: string;
    value: React.ReactNode;
    icon: React.ReactNode;
    color?: string;
    loading?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({ label, value, icon, color = 'primary.main', loading }) => (
    <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3, height: '100%' }}>
        <Stack direction="row" alignItems="center" spacing={2}>
            <Box sx={{
                width: 48, height: 48, borderRadius: 2, display: 'flex',
                alignItems: 'center', justifyContent: 'center',
                bgcolor: 'action.hover', color,
            }}>
                {icon}
            </Box>
            <Box sx={{ minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" noWrap>{label}</Typography>
                <Typography variant="h5" fontWeight="bold">
                    {loading ? <CircularProgress size={20} /> : value}
                </Typography>
            </Box>
        </Stack>
    </Paper>
);

const AdminDashboardPage: React.FC = () => {
    const payment = useAdminPaymentStats();
    const products = useAdminProductStats();
    const orderStats = useAdminOrderStats();
    const stores = useAdminStores({ page: 0, size: 1 });
    const users = useAdminUsers({ page: 0, size: 1 });

    const revenueChart = useMemo(() => {
        const monthly = payment.data?.monthly ?? [];
        return {
            months: monthly.map((m) => m.month),
            subscription: monthly.map((m) => m.subscription),
            commission: monthly.map((m) => m.commission),
        };
    }, [payment.data]);

    const statusChart = useMemo(() => {
        const by = orderStats.data?.byStatus ?? {};
        const keys = Object.keys(STATUS_LABELS).filter((k) => by[k] != null);
        return {
            labels: keys.map((k) => STATUS_LABELS[k]),
            values: keys.map((k) => by[k] ?? 0),
        };
    }, [orderStats.data]);

    return (
        <Box>
            <Typography variant="h5" fontWeight="bold" sx={{ mb: 3 }}>Genel Bakış</Typography>

            {/* Özet kartları */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
                    <StatCard label="Mağaza" icon={<StoreIcon />}
                        loading={stores.isLoading}
                        value={stores.data?.totalElements ?? '—'} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
                    <StatCard label="Ürün" icon={<ProductIcon />} color="info.main"
                        loading={products.isLoading}
                        value={products.data?.totalProducts ?? '—'} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
                    <StatCard label="Sipariş" icon={<OrderIcon />} color="warning.main"
                        loading={orderStats.isLoading}
                        value={orderStats.data?.totalOrders ?? '—'} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
                    <StatCard label="Kullanıcı" icon={<UsersIcon />} color="success.main"
                        loading={users.isLoading}
                        value={users.data?.totalElements ?? '—'} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
                    <StatCard label="Toplam Gelir" icon={<RevenueIcon />}
                        loading={payment.isLoading}
                        value={payment.data ? formatPrice(payment.data.totalRevenue) : '—'} />
                </Grid>
            </Grid>

            {/* Gelir kırılımı */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, md: 6 }}>
                    <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                        <Typography variant="caption" color="text.secondary">Komisyon Geliri (satış)</Typography>
                        <Typography variant="h5" fontWeight="bold" color="primary.main">
                            {payment.data ? formatPrice(payment.data.commissionRevenue) : '—'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {payment.data?.productPaymentCount ?? 0} ürün ödemesi
                        </Typography>
                    </Paper>
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                    <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                        <Typography variant="caption" color="text.secondary">Abonelik Geliri</Typography>
                        <Typography variant="h5" fontWeight="bold" color="secondary.main">
                            {payment.data ? formatPrice(payment.data.subscriptionRevenue) : '—'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {payment.data?.subscriptionPaymentCount ?? 0} abonelik ödemesi
                        </Typography>
                    </Paper>
                </Grid>
            </Grid>

            {/* Grafikler */}
            <Grid container spacing={2}>
                <Grid size={{ xs: 12, md: 7 }}>
                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
                        <Typography variant="subtitle2" sx={{ mb: 1 }}>Aylık Platform Geliri</Typography>
                        {payment.isLoading ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                        ) : revenueChart.months.length === 0 ? (
                            <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>Henüz gelir verisi yok.</Typography>
                        ) : (
                            <BarChart
                                height={260}
                                xAxis={[{ scaleType: 'band', data: revenueChart.months }]}
                                series={[
                                    { data: revenueChart.commission, label: 'Komisyon', color: '#F27A1A', stack: 'total' },
                                    { data: revenueChart.subscription, label: 'Abonelik', color: '#2D3A8C', stack: 'total' },
                                ]}
                            />
                        )}
                    </Paper>
                </Grid>
                <Grid size={{ xs: 12, md: 5 }}>
                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
                        <Typography variant="subtitle2" sx={{ mb: 1 }}>Sipariş Durum Dağılımı</Typography>
                        {orderStats.isLoading ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                        ) : statusChart.labels.length === 0 ? (
                            <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>Veri yok.</Typography>
                        ) : (
                            <BarChart
                                height={260}
                                xAxis={[{ scaleType: 'band', data: statusChart.labels }]}
                                series={[{ data: statusChart.values, label: 'Sipariş', color: '#3FB68B' }]}
                            />
                        )}
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AdminDashboardPage;
