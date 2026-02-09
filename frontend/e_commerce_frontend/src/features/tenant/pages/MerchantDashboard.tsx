import React, { useEffect, useState } from 'react';
import {
    Box, Typography, Paper, Chip, Stack,
    Avatar, Divider, CircularProgress, Alert, Grid
} from '@mui/material';
import {
    Verified as VerifiedIcon,
    Language as WebIcon,
    Email as EmailIcon,
    Phone as PhoneIcon,
    LocationOn as LocationIcon,
    Group as GroupIcon,
    Business as BusinessIcon,
    DateRange as DateIcon
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { tenantService } from '../../../service/tenantService';
import type { TenantDetail } from '../../../types/tenant';
import { AddressType, BusinessType } from '../../../types/enums';
import TenantAddressCard from "../../../components/shared/address/TenantAddressCard.tsx";

const MerchantDashboard: React.FC = () => {
    const { activeTenant } = useMerchantStore();
    const [tenantDetail, setTenantDetail] = useState<TenantDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchDetail = async () => {
            if (!activeTenant) return;
            try {
                setLoading(true);
                const data = await tenantService.getTenantById(activeTenant.id);
                setTenantDetail(data);
            } catch (err) {
                console.error(err);
                setError('Mağaza detayları yüklenirken bir hata oluştu.');
            } finally {
                setLoading(false);
            }
        };

        fetchDetail();
    }, [activeTenant?.id]);

    if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}><CircularProgress /></Box>;
    if (error) return <Alert severity="error">{error}</Alert>;
    if (!tenantDetail) return <Alert severity="warning">Mağaza bilgisi bulunamadı.</Alert>;

    type ChipColor = 'success' | 'warning' | 'error' | 'default' | 'primary' | 'secondary' | 'info';

    const getStatusColor = (status: string): ChipColor => {
        switch (status) {
            case 'ACTIVE': return 'success';
            case 'PENDING': return 'warning';
            case 'SUSPENDED': return 'error';
            default: return 'default';
        }
    };

    const getVerificationColor = (isVerified: boolean): ChipColor => {
        switch (isVerified) {
            case true : return 'success';
            case false : return 'error';
        }
    };

    const getRoleLabel = (role: string) => {
        const map: Record<string, string> = { 'OWNER': 'Sahibi', 'ADMIN': 'Yönetici', 'STAFF': 'Personel', 'ACCOUNTANT': 'Muhasebe' };
        return map[role] || role;
    };

    const prettyAddressType = (type: string | undefined) => {
        return type === AddressType.SHIPPING ? 'Teslimat/Depo' : 'Fatura';
    };

    return (
        <Box>
            <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
                <Box>
                    <Stack direction="row" alignItems="center" gap={1}>
                        <Typography variant="h4" fontWeight="800" color="text.primary">
                            {tenantDetail.name}
                        </Typography>
                        {tenantDetail.isVerified && (
                            <VerifiedIcon color="primary" />
                        )}
                        <Chip
                            label={tenantDetail.status}
                            color={getStatusColor(tenantDetail.status)}
                            size="small"
                            sx={{ fontWeight: 'bold' }}
                        />
                        <Chip
                            label={tenantDetail.isVerified ? 'Doğrulanmış' : 'Doğrulanmamış'}
                            color={getVerificationColor(tenantDetail.isVerified)}
                            size="small"
                            sx={{ fontWeight: 'bold' }}
                        />
                    </Stack>
                    <Typography variant="body1" color="text.secondary" sx={{ mt: 0.5 }}>
                        {tenantDetail.description || 'Henüz bir açıklama eklenmemiş.'}
                    </Typography>
                </Box>
                <Box sx={{ textAlign: 'right' }}>
                    <Typography variant="caption" display="block" color="text.secondary">
                        Kuruluş Tarihi
                    </Typography>
                    <Chip
                        icon={<DateIcon />}
                        label={new Date(tenantDetail.createdAt).toLocaleDateString('tr-TR')}
                        variant="outlined"
                    />
                </Box>
            </Box>

            <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 8 }}>
                    <Paper elevation={0} sx={{ p: 3, borderRadius: 4, border: '1px solid #e2e8f0', height: '100%' }}>
                        <Stack direction="row" alignItems="center" gap={1} mb={2}>
                            <BusinessIcon color="action" />
                            <Typography variant="h6" fontWeight="bold">Kurumsal Bilgiler</Typography>
                        </Stack>
                        <Divider sx={{ mb: 2 }} />

                        <Grid container spacing={2}>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">Resmi Ünvan</Typography>
                                <Typography fontWeight="500">{tenantDetail.businessName}</Typography>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">İşletme Tipi</Typography>
                                <Typography fontWeight="500">
                                    {tenantDetail.businessType === BusinessType.CORPORATE ? 'Anonim/Limited Şirket' : 'Şahıs Şirketi'}
                                </Typography>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">Vergi No / T.C.</Typography>
                                <Typography fontWeight="500">{tenantDetail.taxId || '-'}</Typography>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">Website</Typography>
                                <Stack direction="row" alignItems="center" gap={0.5}>
                                    <WebIcon fontSize="small" color="action" />
                                    <Typography fontWeight="500">{tenantDetail.websiteUrl || '-'}</Typography>
                                </Stack>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">İletişim Email</Typography>
                                <Stack direction="row" alignItems="center" gap={0.5}>
                                    <EmailIcon fontSize="small" color="action" />
                                    <Typography fontWeight="500">{tenantDetail.contactEmail}</Typography>
                                </Stack>
                            </Grid>
                            <Grid size={{ xs: 12, sm: 6 }}>
                                <Typography variant="subtitle2" color="text.secondary">Telefon</Typography>
                                <Stack direction="row" alignItems="center" gap={0.5}>
                                    <PhoneIcon fontSize="small" color="action" />
                                    <Typography fontWeight="500">{tenantDetail.contactPhone}</Typography>
                                </Stack>
                            </Grid>
                        </Grid>
                    </Paper>
                </Grid>

                <Grid size={{ xs: 12, md: 4 }}>
                    <Paper elevation={0} sx={{ p: 3, borderRadius: 4, border: '1px solid #e2e8f0', height: '100%' }}>
                        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2}>
                            <Stack direction="row" alignItems="center" gap={1}>
                                <GroupIcon color="action" />
                                <Typography variant="h6" fontWeight="bold">Ekip</Typography>
                            </Stack>
                            <Chip label={`${tenantDetail.members.length} Üye`} size="small" />
                        </Stack>
                        <Divider sx={{ mb: 2 }} />

                        <Stack spacing={2}>
                            {tenantDetail.members.map((member) => (
                                <Stack key={member.userId} direction="row" alignItems="center" gap={2}>
                                    <Avatar src={member.profileImageUrl || undefined} sx={{ width: 40, height: 40 }}>
                                        {member.firstName.charAt(0)}
                                    </Avatar>
                                    <Box flex={1}>
                                        <Typography variant="body2" fontWeight="bold">
                                            {member.firstName} {member.lastName}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            {member.email}
                                        </Typography>
                                    </Box>
                                    <Chip label={getRoleLabel(member.role)} size="small" variant="outlined" />
                                </Stack>
                            ))}
                        </Stack>
                    </Paper>
                </Grid>

                <Grid size={12}>
                    <Paper elevation={0} sx={{ p: 3, borderRadius: 4, border: '1px solid #e2e8f0' }}>
                        <Stack direction="row" alignItems="center" gap={1} mb={2}>
                            <LocationIcon color="action" />
                            <Typography variant="h6" fontWeight="bold">Kayıtlı Adres</Typography>
                        </Stack>
                        <Divider sx={{ mb: 2 }} />

                        <Grid container spacing={3}>
                            {tenantDetail.addresses.map((addr) => (
                                <Grid size={{ xs: 12, md: 6 }} key={addr.id}>
                                    <TenantAddressCard address={addr} />
                                </Grid>
                            ))}
                        </Grid>
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default MerchantDashboard;
