import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
    Box, CssBaseline, AppBar, Toolbar, Typography, IconButton,
    Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText,
    Avatar, Menu, MenuItem, Divider, Tooltip,
} from '@mui/material';
import {
    Menu as MenuIcon,
    Dashboard as DashboardIcon,
    Store as StoreIcon,
    Category as CategoryIcon,
    Inventory2 as ProductIcon,
    ShoppingBag as OrderIcon,
    Payments as PaymentsIcon,
    People as UsersIcon,
    AdminPanelSettings as AdminIcon,
    Storefront as StorefrontIcon,
    Build as BuildIcon,
    Logout as LogoutIcon,
} from '@mui/icons-material';
import { useAuthStore } from '../../../store/useAuthStore';
import { useAuth } from 'react-oidc-context';
import { AppRoutes } from '../../../utils/routes';

const DRAWER_WIDTH = 280;

const NAV_ITEMS = [
    { text: 'Genel Bakış', icon: <DashboardIcon />, path: AppRoutes.ADMIN_DASHBOARD },
    { text: 'Mağazalar', icon: <StoreIcon />, path: AppRoutes.ADMIN_STORES },
    { text: 'Ürünler', icon: <ProductIcon />, path: AppRoutes.ADMIN_PRODUCTS },
    { text: 'Siparişler', icon: <OrderIcon />, path: AppRoutes.ADMIN_ORDERS },
    { text: 'Ödemeler', icon: <PaymentsIcon />, path: AppRoutes.ADMIN_TRANSACTIONS },
    { text: 'Kullanıcılar', icon: <UsersIcon />, path: AppRoutes.ADMIN_USERS },
    { text: 'Kategoriler', icon: <CategoryIcon />, path: AppRoutes.ADMIN_CATEGORIES },
    { text: 'Sistem / Bakım', icon: <BuildIcon />, path: AppRoutes.ADMIN_MAINTENANCE },
];

const AdminLayout: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user } = useAuthStore();
    const auth = useAuth();

    const [mobileOpen, setMobileOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const handleLogout = () =>
        auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin });

    const isSelected = (path: string) => location.pathname === path;

    const drawerContent = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: 'secondary.main', color: '#fff' }}>
            <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar variant="rounded" sx={{ bgcolor: 'primary.main', width: 40, height: 40 }}>
                    <AdminIcon />
                </Avatar>
                <Box sx={{ minWidth: 0, flex: 1 }}>
                    <Typography variant="subtitle2" sx={{ color: 'rgba(255,255,255,0.6)' }} fontSize="0.75rem">
                        Platform Yönetimi
                    </Typography>
                    <Typography variant="body1" fontWeight="bold" noWrap>
                        Süper Admin
                    </Typography>
                </Box>
            </Box>

            <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />

            <List sx={{ px: 2, mt: 2, flexGrow: 1 }}>
                {NAV_ITEMS.map((item) => (
                    <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
                        <ListItemButton
                            selected={isSelected(item.path)}
                            onClick={() => { navigate(item.path); setMobileOpen(false); }}
                            sx={{
                                borderRadius: 2,
                                '&.Mui-selected': {
                                    bgcolor: 'primary.main', color: '#fff',
                                    '&:hover': { bgcolor: 'primary.dark' },
                                },
                                '&:hover': { bgcolor: 'rgba(255,255,255,0.06)' },
                            }}
                        >
                            <ListItemIcon
                                sx={{ color: isSelected(item.path) ? '#fff' : 'rgba(255,255,255,0.6)', minWidth: 40 }}
                            >
                                {item.icon}
                            </ListItemIcon>
                            <ListItemText
                                primary={item.text}
                                primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 500 }}
                            />
                        </ListItemButton>
                    </ListItem>
                ))}
            </List>

            <Box sx={{ p: 2, textAlign: 'center', opacity: 0.4 }}>
                <Typography variant="caption">İlhan E-Ticaret — Admin</Typography>
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
            <CssBaseline />

            <AppBar
                position="fixed"
                elevation={0}
                sx={{
                    width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
                    ml: { md: `${DRAWER_WIDTH}px` },
                    bgcolor: 'background.paper',
                    color: 'text.primary',
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                    zIndex: (theme) => theme.zIndex.drawer + 1,
                }}
            >
                <Toolbar>
                    <IconButton
                        edge="start"
                        onClick={() => setMobileOpen((v) => !v)}
                        sx={{ mr: 2, display: { md: 'none' } }}
                    >
                        <MenuIcon />
                    </IconButton>

                    <Typography variant="h6" fontWeight="bold" sx={{ flexGrow: 1, color: 'text.primary' }}>
                        Platform Yönetim Paneli
                    </Typography>

                    <Tooltip title={user?.firstName ?? 'Hesap'}>
                        <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} size="small">
                            <Avatar
                                src={user?.profileImageUrl ?? undefined}
                                sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.9rem' }}
                            >
                                {user?.firstName?.charAt(0) ?? 'A'}
                            </Avatar>
                        </IconButton>
                    </Tooltip>

                    <Menu
                        anchorEl={anchorEl}
                        open={Boolean(anchorEl)}
                        onClose={() => setAnchorEl(null)}
                        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                    >
                        <MenuItem disabled sx={{ opacity: '1 !important' }}>
                            <Typography variant="body2" fontWeight="bold">
                                {user?.firstName} {user?.lastName}
                            </Typography>
                        </MenuItem>
                        <Divider />
                        <MenuItem onClick={() => navigate('/')}>
                            <StorefrontIcon fontSize="small" sx={{ mr: 1 }} />
                            Platforma Dön
                        </MenuItem>
                        <Divider />
                        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                            <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
                            Çıkış Yap
                        </MenuItem>
                    </Menu>
                </Toolbar>
            </AppBar>

            <Drawer
                variant="temporary"
                open={mobileOpen}
                onClose={() => setMobileOpen(false)}
                ModalProps={{ keepMounted: true }}
                sx={{
                    display: { xs: 'block', md: 'none' },
                    '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
                }}
            >
                {drawerContent}
            </Drawer>

            <Drawer
                variant="permanent"
                sx={{
                    display: { xs: 'none', md: 'block' },
                    width: DRAWER_WIDTH,
                    flexShrink: 0,
                    '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box', overflowX: 'hidden' },
                }}
            >
                {drawerContent}
            </Drawer>

            <Box
                component="main"
                sx={{
                    flexGrow: 1, minWidth: 0, p: { xs: 2, md: 4 },
                    minHeight: '100vh', overflowX: 'hidden',
                    display: 'flex', flexDirection: 'column',
                }}
            >
                <Toolbar />
                <Box sx={{ flexGrow: 1 }}>
                    <Outlet />
                </Box>
            </Box>
        </Box>
    );
};

export default AdminLayout;
