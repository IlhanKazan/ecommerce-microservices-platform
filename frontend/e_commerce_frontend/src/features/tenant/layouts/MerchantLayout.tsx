import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
    Box, CssBaseline, AppBar, Toolbar, Typography, IconButton,
    Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText,
    Avatar, Menu, MenuItem, Divider, Tooltip
} from '@mui/material';
import {
    Menu as MenuIcon,
    Dashboard as DashboardIcon,
    Inventory as ProductIcon,
    ShoppingBag as OrderIcon,
    RateReview as ReviewIcon,
    CreditCard as BillingIcon,
    Settings as SettingsIcon,
    Store as StoreIcon,
    Logout as LogoutIcon,
    SwapHoriz as SwitchStoreIcon
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useAuthStore } from '../../../store/useAuthStore';
import { useAuth } from 'react-oidc-context';

const DRAWER_WIDTH = 280;

const MENU_ITEMS = [
    { text: 'Panel Özeti', icon: <DashboardIcon />, path: '/merchant/dashboard' },
    { text: 'Ürün Yönetimi', icon: <ProductIcon />, path: '/merchant/products' },
    { text: 'Siparişler', icon: <OrderIcon />, path: '/merchant/orders' },
    { text: 'Değerlendirmeler', icon: <ReviewIcon />, path: '/merchant/reviews' },
    { divider: true },
    { text: 'Abonelik & Ödeme', icon: <BillingIcon />, path: '/merchant/subscription' },
    { text: 'Mağaza Ayarları', icon: <SettingsIcon />, path: '/merchant/settings' },
];

const MerchantLayout: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const { activeTenant, clearMerchantSession } = useMerchantStore();
    const { user } = useAuthStore();
    const auth = useAuth();

    const [mobileOpen, setMobileOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const handleDrawerToggle = () => setMobileOpen(!mobileOpen);
    const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
    const handleMenuClose = () => setAnchorEl(null);

    const handleSwitchStore = () => {
        clearMerchantSession();
        navigate('/merchant/select');
    };

    const handleLogout = () => {
        auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin });
    };

    const drawerContent = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: '#1e293b', color: '#fff' }}>
            <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar
                    variant="rounded"
                    src={activeTenant?.logoUrl || undefined}
                    sx={{ bgcolor: '#38bdf8', width: 40, height: 40 }}
                >
                    <StoreIcon />
                </Avatar>
                <Box sx={{ minWidth: 0, flex: 1 }}>
                    <Typography variant="subtitle2" color="#94a3b8" fontSize="0.75rem">
                        Aktif Mağaza
                    </Typography>
                    <Typography variant="body1" fontWeight="bold" noWrap title={activeTenant?.name}>
                        {activeTenant?.name || 'Yükleniyor...'}
                    </Typography>
                </Box>
                <Tooltip title="Mağaza Değiştir">
                    <IconButton onClick={handleSwitchStore} size="small" sx={{ color: '#94a3b8', '&:hover': { color: '#fff' } }}>
                        <SwitchStoreIcon fontSize="small" />
                    </IconButton>
                </Tooltip>
            </Box>

            <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />

            <List sx={{ px: 2, mt: 2, flexGrow: 1 }}>
                {MENU_ITEMS.map((item, index) => (
                    item.divider ? <Divider key={index} sx={{ my: 2, borderColor: 'rgba(255,255,255,0.1)' }} /> :
                        <ListItem key={item.text} disablePadding sx={{ mb: 0.5 }}>
                            <ListItemButton
                                selected={location.pathname === item.path}
                                onClick={() => navigate(item.path!)}
                                sx={{
                                    borderRadius: 2,
                                    '&.Mui-selected': { bgcolor: '#38bdf8', color: '#fff', '&:hover': { bgcolor: '#0ea5e9' } },
                                    '&:hover': { bgcolor: 'rgba(255,255,255,0.05)' }
                                }}
                            >
                                <ListItemIcon sx={{ color: location.pathname === item.path ? '#fff' : '#94a3b8', minWidth: 40 }}>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText primary={item.text} primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 500 }} />
                            </ListItemButton>
                        </ListItem>
                ))}
            </List>

            <Box sx={{ p: 2, textAlign: 'center', opacity: 0.5 }}>
                <Typography variant="caption">İlhan E-Ticaret v1.0</Typography>
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'flex', bgcolor: '#f8fafc', minHeight: '100vh' }}>
            <CssBaseline />

            <AppBar
                position="fixed"
                elevation={0}
                sx={{
                    width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
                    ml: { md: `${DRAWER_WIDTH}px` },
                    bgcolor: '#fff',
                    color: '#334155',
                    borderBottom: '1px solid #e2e8f0'
                }}
            >
                <Toolbar>
                    <IconButton color="inherit" edge="start" onClick={handleDrawerToggle} sx={{ mr: 2, display: { md: 'none' } }}>
                        <MenuIcon />
                    </IconButton>

                    <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                        {MENU_ITEMS.find(i => i.path === location.pathname)?.text || 'Panel'}
                    </Typography>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Box sx={{ textAlign: 'right', display: { xs: 'none', sm: 'block' }, mr: 1 }}>
                            <Typography variant="subtitle2" fontWeight="bold">
                                {user?.firstName} {user?.lastName}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                                Mağaza Yöneticisi
                            </Typography>
                        </Box>
                        <IconButton onClick={handleMenuOpen} sx={{ p: 0 }}>
                            <Avatar alt={user?.firstName || 'A'} src={user?.profileImageUrl || undefined} sx={{ bgcolor: '#38bdf8' }} />
                        </IconButton>
                    </Box>

                    <Menu
                        sx={{ mt: '45px' }}
                        anchorEl={anchorEl}
                        open={Boolean(anchorEl)}
                        onClose={handleMenuClose}
                        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                        PaperProps={{ elevation: 2, sx: { borderRadius: 3, minWidth: 180 } }}
                    >
                        <MenuItem onClick={() => navigate('/')}>Alışverişe Dön</MenuItem>
                        <Divider />
                        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                            <LogoutIcon fontSize="small" sx={{ mr: 1 }} /> Çıkış Yap
                        </MenuItem>
                    </Menu>
                </Toolbar>
            </AppBar>

            <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={handleDrawerToggle}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: 'block', md: 'none' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
                    }}
                >
                    {drawerContent}
                </Drawer>
                <Drawer
                    variant="permanent"
                    sx={{
                        display: { xs: 'none', md: 'block' },
                        '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, borderRight: 'none' },
                    }}
                    open
                >
                    {drawerContent}
                </Drawer>
            </Box>

            <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8, maxWidth: '100vw', overflowX: 'hidden' }}>
                <Outlet />
            </Box>
        </Box>
    );
};

export default MerchantLayout;