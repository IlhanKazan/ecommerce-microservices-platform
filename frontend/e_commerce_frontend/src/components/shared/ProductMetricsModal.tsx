import React, { useMemo } from 'react';
import {
    Dialog, DialogTitle, DialogContent, IconButton, Box, Paper, Typography, Stack, Grid,
    CircularProgress, Alert, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
} from '@mui/material';
import {
    Close as CloseIcon,
    Inventory2 as UnitsIcon,
    Payments as RevenueIcon,
    ReceiptLong as OrdersIcon,
} from '@mui/icons-material';
import { BarChart } from '@mui/x-charts/BarChart';
import { formatPrice } from '../../utils/formatPrice';
import EmptyState from './EmptyState';
import type { ProductSalesMetrics } from '../../types/order';

interface Props {
    open: boolean;
    onClose: () => void;
    productName: string;
    data: ProductSalesMetrics | undefined;
    isLoading: boolean;
    isError: boolean;
}

const truncate = (s: string, n = 18) => (s.length > n ? s.slice(0, n - 1) + '…' : s);

interface StatCardProps {
    label: string;
    value: string;
    icon: React.ReactNode;
    color: string;
}
const StatCard: React.FC<StatCardProps> = ({ label, value, icon, color }) => (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
        <Stack direction="row" alignItems="center" spacing={2}>
            <Box sx={{ width: 44, height: 44, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'action.hover', color }}>
                {icon}
            </Box>
            <Box sx={{ minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" noWrap>{label}</Typography>
                <Typography variant="h6" fontWeight="bold">{value}</Typography>
            </Box>
        </Stack>
    </Paper>
);

export const ProductMetricsModal: React.FC<Props> = ({ open, onClose, productName, data, isLoading, isError }) => {
    // Varyant kırılımı yalnız birden fazla satılan varyant/ürün varsa anlamlı
    const breakdown = useMemo(() => data?.breakdown ?? [], [data]);
    const hasBreakdown = breakdown.length > 1;

    const chart = useMemo(() => {
        const rows = breakdown.slice(0, 10);
        return {
            labels: rows.map((p) => truncate(p.productName)),
            values: rows.map((p) => p.revenue),
        };
    }, [breakdown]);

    const hasSales = (data?.totalUnits ?? 0) > 0;

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                Satış Metrikleri — {productName}
                <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent dividers>
                {isLoading ? (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
                ) : isError ? (
                    <Alert severity="error">Metrikler yüklenirken bir hata oluştu.</Alert>
                ) : !hasSales ? (
                    <EmptyState title="Henüz satış yok" description="Bu ürün için tamamlanmış sipariş bulunmuyor." />
                ) : (
                    <>
                        {/* Toplam kartlar */}
                        <Grid container spacing={2} sx={{ mb: hasBreakdown ? 3 : 0 }}>
                            <Grid size={{ xs: 12, sm: 4 }}>
                                <StatCard label="Satılan Adet" value={String(data?.totalUnits ?? 0)} icon={<UnitsIcon />} color="primary.main" />
                            </Grid>
                            <Grid size={{ xs: 12, sm: 4 }}>
                                <StatCard label="Toplam Ciro" value={formatPrice(data?.totalRevenue ?? 0)} icon={<RevenueIcon />} color="success.main" />
                            </Grid>
                            <Grid size={{ xs: 12, sm: 4 }}>
                                <StatCard label="Sipariş Sayısı" value={String(data?.totalOrders ?? 0)} icon={<OrdersIcon />} color="info.main" />
                            </Grid>
                        </Grid>

                        {/* Varyant kırılımı (yalnız varyantlı üründe) */}
                        {hasBreakdown && (
                            <Grid container spacing={2}>
                                <Grid size={{ xs: 12, md: 7 }}>
                                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, height: '100%' }}>
                                        <Typography variant="subtitle2" sx={{ mb: 1 }}>Varyant Bazında Ciro</Typography>
                                        <BarChart
                                            height={300}
                                            xAxis={[{ scaleType: 'band', data: chart.labels }]}
                                            yAxis={[{ valueFormatter: (v: number | null) => (v != null ? formatPrice(v) : '') }]}
                                            series={[{
                                                data: chart.values,
                                                label: 'Ciro',
                                                color: '#F27A1A',
                                                valueFormatter: (v) => (v != null ? formatPrice(v) : ''),
                                            }]}
                                        />
                                    </Paper>
                                </Grid>
                                <Grid size={{ xs: 12, md: 5 }}>
                                    <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden', height: '100%' }}>
                                        <TableContainer>
                                            <Table size="small">
                                                <TableHead>
                                                    <TableRow>
                                                        <TableCell>Varyant</TableCell>
                                                        <TableCell align="right">Adet</TableCell>
                                                        <TableCell align="right">Sipariş</TableCell>
                                                        <TableCell align="right">Ciro</TableCell>
                                                    </TableRow>
                                                </TableHead>
                                                <TableBody>
                                                    {breakdown.map((p) => (
                                                        <TableRow key={p.productId} hover>
                                                            <TableCell sx={{ maxWidth: 180 }}>
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
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ProductMetricsModal;
