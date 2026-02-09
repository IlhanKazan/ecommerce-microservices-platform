import React from 'react';
import { Box, Paper, Typography, List, ListItemButton, ListItemIcon, ListItemText, Divider, Grid, Avatar } from '@mui/material';
import { Person as PersonIcon, ShoppingBag as OrderIcon, LocationOn as AddressIcon, Logout as LogoutIcon } from '@mui/icons-material';
import { useAuth } from "react-oidc-context";
import { useAuthStore } from "../../../store/useAuthStore";
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const AccountLayout: React.FC = () => {
    const auth = useAuth();
    const { userProfile } = useAuthStore();
    const location = useLocation();
    const navigate = useNavigate();

    const menuItems = [
        { path: '/user', label: 'Kullanıcı Bilgilerim', icon: <PersonIcon />, exact: true },
        { path: '/user/orders', label: 'Siparişlerim', icon: <OrderIcon /> },
        { path: '/user/addresses', label: 'Adres Bilgilerim', icon: <AddressIcon /> },
    ];

    return (
        <Box sx={{ py: 4, px: { xs: 2, md: 0 }, maxWidth: '1200px', mx: 'auto' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold', color: 'text.primary' }}>
                Hesabım
            </Typography>

            <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 3 }}>
                    <Paper elevation={0} sx={{ border: '1px solid #e0e0e0', borderRadius: 2, overflow: 'hidden' }}>
                        <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                            <Avatar sx={{ width: 50, height: 50, bgcolor: 'background.paper', color: 'primary.main' }}>
                                {userProfile?.given_name?.[0]}
                            </Avatar>
                            <Box sx={{ overflow: 'hidden' }}>
                                <Typography variant="subtitle1" fontWeight="bold" noWrap>
                                    {userProfile?.given_name} {userProfile?.family_name}
                                </Typography>
                                <Typography variant="caption" display="block" noWrap sx={{ opacity: 0.8 }}>
                                    {userProfile?.email}
                                </Typography>
                            </Box>
                        </Box>

                        <List component="nav">
                            {menuItems.map((item) => {
                                const isActive = item.exact
                                    ? location.pathname === item.path
                                    : location.pathname.startsWith(item.path);

                                return (
                                    <ListItemButton
                                        key={item.path}
                                        selected={isActive}
                                        onClick={() => navigate(item.path)}
                                        sx={{
                                            borderLeft: isActive ? '4px solid' : '4px solid transparent',
                                            borderColor: 'secondary.main',
                                            '&.Mui-selected': { bgcolor: 'action.hover' }
                                        }}
                                    >
                                        <ListItemIcon sx={{ color: isActive ? 'secondary.main' : 'inherit' }}>
                                            {item.icon}
                                        </ListItemIcon>
                                        <ListItemText primary={item.label} />
                                    </ListItemButton>
                                );
                            })}
                            <Divider />
                            <ListItemButton onClick={() => auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin })}>
                                <ListItemIcon><LogoutIcon color="error" /></ListItemIcon>
                                <ListItemText primary="Çıkış Yap" primaryTypographyProps={{ color: 'error' }} />
                            </ListItemButton>
                        </List>
                    </Paper>
                </Grid>

                <Grid size={{ xs: 12, md: 9 }}>
                    <Paper elevation={0} sx={{ p: 4, border: '1px solid #e0e0e0', borderRadius: 2, bgcolor: 'background.paper', minHeight: '500px' }}>
                        <Outlet />
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AccountLayout;