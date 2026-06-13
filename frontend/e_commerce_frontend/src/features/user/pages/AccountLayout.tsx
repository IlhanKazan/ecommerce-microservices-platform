import React from 'react';
import { Box, Paper, Typography, List, ListItemButton, ListItemIcon, ListItemText, Divider, Grid, Avatar } from '@mui/material';
import { Person as PersonIcon, ShoppingBag as OrderIcon, LocationOn as AddressIcon, FavoriteBorder as FavoriteIcon, Logout as LogoutIcon } from '@mui/icons-material';
import { useAuth } from "react-oidc-context";
import { useAuthStore } from "../../../store/useAuthStore";
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { tokens } from '../../../utils/themeTokens';

const AccountLayout: React.FC = () => {
    const auth = useAuth();
    const { user, oidcProfile } = useAuthStore();
    const location = useLocation();
    const navigate = useNavigate();

    // İsim/email backend user'dan, yoksa OIDC profilinden (refresh/direkt giriş fallback)
    const firstName = user?.firstName ?? oidcProfile?.given_name ?? '';
    const lastName = user?.lastName ?? oidcProfile?.family_name ?? '';
    const email = user?.email ?? oidcProfile?.email ?? '';
    const avatarUrl = user?.profileImageUrl ?? undefined;
    const initial = (firstName || email || '?').charAt(0).toUpperCase();

    const menuItems = [
        { path: '/user', label: 'Kullanıcı Bilgilerim', icon: <PersonIcon />, exact: true },
        { path: '/user/orders', label: 'Siparişlerim', icon: <OrderIcon /> },
        { path: '/user/favorites', label: 'Favorilerim', icon: <FavoriteIcon /> },
        { path: '/user/addresses', label: 'Adres Bilgilerim', icon: <AddressIcon /> },
    ];

    return (
        <Box sx={{ py: 4, px: { xs: 2, md: 0 }, maxWidth: '1200px', mx: 'auto' }}>
            <Typography variant="h4" sx={{ mb: 3, fontWeight: 'bold', color: 'text.primary' }}>
                Hesabım
            </Typography>

            <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 3 }}>
                    <Paper elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 3, overflow: 'hidden', boxShadow: '0 4px 16px rgba(26,34,56,0.06)' }}>
                        <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2, background: tokens.gradient.dark, color: 'common.white' }}>
                            <Avatar
                                src={avatarUrl}
                                sx={{ width: 56, height: 56, bgcolor: 'primary.main', color: 'common.white', fontWeight: 700, fontSize: '1.4rem', border: '2px solid rgba(255,255,255,0.4)' }}
                            >
                                {initial}
                            </Avatar>
                            <Box sx={{ overflow: 'hidden' }}>
                                <Typography variant="subtitle1" fontWeight={700} noWrap>
                                    {firstName} {lastName}
                                </Typography>
                                <Typography variant="caption" display="block" noWrap sx={{ opacity: 0.8 }}>
                                    {email}
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
                    <Paper elevation={0} sx={{ p: { xs: 2.5, md: 4 }, border: '1px solid', borderColor: 'divider', borderRadius: 3, bgcolor: 'background.paper', minHeight: '500px', boxShadow: '0 4px 16px rgba(26,34,56,0.05)' }}>
                        <Outlet />
                    </Paper>
                </Grid>
            </Grid>
        </Box>
    );
};

export default AccountLayout;