import React, { useMemo } from 'react';
import {
    Box, Paper, Typography, Stack, Grid, CircularProgress, Alert, Chip,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
} from '@mui/material';
import { BarChart } from '@mui/x-charts/BarChart';
import {
    Payments as RevenueIcon,
    Percent as CommissionIcon,
    AccountBalanceWallet as NetIcon,
} from '@mui/icons-material';
import { useGetTenantAnalytics } from '../../../query/useOrderQueries';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { formatPrice } from '../../../utils/formatPrice';
import EmptyState from '../../../components/shared/EmptyState';

const truncate = (s: string, n = 18) => (s.length > n ? s.slice(0, n - 1) + '…' : s);

interface MoneyCardProps {
    label: string;
    value: number;
    icon: React.ReactNode;
    color: string;
}
const MoneyCard: React.FC<MoneyCardProps> = ({ label, value, icon, color }) => (
    <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3, height: '100%' }}>
        <Stack direction="row" alignItems="center" spacing={2}>
            <Box sx={{ width: 48, height: 48, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover', color }}>
                {icon}
            </Box>
            <Box sx={{ minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" noWrap>{label}</Typography>
                <Typography variant="h5" fontWeight="bold">{formatPrice(value)}</Typography>
            </Box>
        </Stack>
    </Paper>
);

const MerchantAnalyticsPage: React.FC = () => {
    const activeTenant = useMerchantStore((s) => s.activeTenant);
    const tenantId = activeTenant?.id ?? null;
    const { data, isLoading, isError } = useGetTenantAnalytics(tenantId);

    const chart = useMemo(() => {
        const top = (data?.topProducts ?? []).slice(0, 8);
        return {
            labels: top.map((p) => truncate(p.productName)),
            values: top.map((p) => p.revenue),
        };
    }, [data]);

    if (isLoading) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}><CircularProgress /></Box>;
    }
    if (isError) {
        return <Alert severity="error">Analitik verileri yüklenirken bir hata oluştu.</Alert>;
    }

    const topProducts = data?.topProducts ?? [];

    return (
        <Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={1} sx={{ mb: 3 }}>
                <Typography variant="h5" fontWeight="bold">Satış Analizi</Typography>
                <Stack direction="row" spacing={1}>
                    <Chip label={`${data?.totalOrders ?? 0} sipariş`} variant="outlined" />
                    <Chip label={`${data?.totalUnits ?? 0} adet satıldı`} variant="outlined" />
                </Stack>
            </Stack>

            {/* Para kartları: brüt → komisyon → net */}
            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, md: 4 }}>
                    <MoneyCard label="Brüt Ciro" value={data?.totalRevenue ?? 0} icon={<RevenueIcon />} color="primary.main" />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                    <MoneyCard label="Platform Komisyonu" value={data?.totalCommission ?? 0} icon={<CommissionIcon />} color="error.main" />
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                    <MoneyCard label="Net Kazanç" value={data?.totalNet ?? 0} icon={<NetIcon />} color="success.main" />
                </Grid>
            </Grid>

            {topProducts.length === 0 ? (
                <EmptyState title="Henüz satış yok" description="İlk siparişiniz tamamlandığında satış metrikleriniz burada görünecek." />
            ) : (
                <Grid container spacing={2}>
                    {/* En çok ciro getiren ürünler */}
                    <Grid size={{ xs: 12, md: 7 }}>
                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
                            <Typography variant="subtitle2" sx={{ mb: 1 }}>En Çok Ciro Getiren Ürünler (Brüt)</Typography>
                            <BarChart
                                height={320}
                                xAxis={[{ scaleType: 'band', data: chart.labels }]}
                                yAxis={[{ valueFormatter: (v: number | null) => (v != null ? formatPrice(v) : '') }]}
                                series={[{
                                    data: chart.values,
                                    label: 'Brüt Ciro',
                                    color: '#F27A1A',
                                    valueFormatter: (v) => (v != null ? formatPrice(v) : ''),
                                }]}
                            />
                        </Paper>
                    </Grid>

                    {/* Ürün tablosu */}
                    <Grid size={{ xs: 12, md: 5 }}>
                        <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden', height: '100%' }}>
                            <TableContainer>
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Ürün</TableCell>
                                            <TableCell align="right">Adet</TableCell>
                                            <TableCell align="right">Sipariş</TableCell>
                                            <TableCell align="right">Brüt Ciro</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {topProducts.map((p) => (
                                            <TableRow key={p.productId} hover>
                                                <TableCell sx={{ maxWidth: 200 }}>
                                                    <Typography variant="body2" noWrap title={p.productName}>{p.productName}</Typography>
                                                </TableCell>
                                                <TableCell align="right">{p.unitsSold}</TableCell>
                                                <TableCell align="right">{p.orderCount}</TableCell>
                                                <TableCell align="right">{formatPrice(p.revenue)}</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Paper>
                    </Grid>
                </Grid>
            )}
        </Box>
    );
};

export default MerchantAnalyticsPage;
