import { Typography, Card, CardContent, Grid, Avatar, Container } from '@mui/material';
import { Store as StoreIcon, Add as AddIcon } from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useNavigate } from 'react-router-dom';
import type {TenantSummary} from "../../../types/tenant.ts";
import { AppRoutes } from '../../../utils/routes.ts'

const SelectStorePage = () => {
    const { myTenants, setActiveTenant } = useMerchantStore();
    const navigate = useNavigate();

    const handleSelect = (tenant: TenantSummary) => {
        setActiveTenant(tenant);
        navigate(AppRoutes.MERCHANT_DASHBOARD);
    };

    return (
        <Container maxWidth="md" sx={{ mt: 10 }}>
            <Typography variant="h4" fontWeight="bold" textAlign="center" mb={1}>
                Yönetmek İstediğiniz Mağazayı Seçin
            </Typography>
            <Typography variant="body1" textAlign="center" color="text.secondary" mb={5}>
                Devam etmek için aşağıdaki işletmelerinizden birini seçin.
            </Typography>

            <Grid container spacing={3} justifyContent="center">
                {myTenants.map((tenant) => (
                    <Grid size={{ xs: 12, md: 4 }} key={tenant.id}>
                        <Card
                            sx={{
                                cursor: 'pointer',
                                transition: '0.3s',
                                '&:hover': { transform: 'translateY(-5px)', boxShadow: 6 }
                            }}
                            onClick={() => handleSelect(tenant)}
                        >
                            <CardContent sx={{ textAlign: 'center', py: 4 }}>
                                <Avatar sx={{ width: 64, height: 64, mx: 'auto', mb: 2, bgcolor: 'primary.main' }}>
                                    <StoreIcon fontSize="large" />
                                </Avatar>
                                <Typography variant="h6" fontWeight="bold">{tenant.name}</Typography>
                                <Typography variant="caption" color="text.secondary">ID: {tenant.id}</Typography>
                            </CardContent>
                        </Card>
                    </Grid>
                ))}

                {/* Yeni Mağaza Aç Kartı */}
                <Grid size={{ xs: 12, md: 4 }}>
                    <Card
                        sx={{
                            cursor: 'pointer', border: '2px dashed #ccc', boxShadow: 'none',
                            height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center'
                        }}
                        onClick={() => navigate('/create-store')}
                    >
                        <CardContent sx={{ textAlign: 'center' }}>
                            <AddIcon sx={{ fontSize: 40, color: 'text.secondary', mb: 1 }} />
                            <Typography variant="h6" color="text.secondary">Yeni Mağaza Aç</Typography>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>
        </Container>
    );
};

export default SelectStorePage;