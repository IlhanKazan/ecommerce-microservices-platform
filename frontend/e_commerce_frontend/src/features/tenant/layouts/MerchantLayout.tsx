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
    Inventory as ProductIcon,
    ShoppingBag as OrderIcon,
    RateReview as ReviewIcon,
    CreditCard as BillingIcon,
    Settings as SettingsIcon,
    Store as StoreIcon,
    Logout as LogoutIcon,
    SwapHoriz as SwitchStoreIcon,
    Warehouse as WarehouseIcon,
} from '@mui/icons-material';
import { useMerchantStore } from '../../../store/useMerchantStore';
import { useAuthStore } from '../../../store/useAuthStore';
import { useAuth } from 'react-oidc-context';

const DRAWER_WIDTH = 280;

type NavItem =
    | { text: string; icon: React.ReactNode; path: string; divider?: never }
    | { divider: true; text?: never; icon?: never; path?: never };

const NAV_ITEMS: NavItem[] = [
    { text: 'Panel Özeti',      icon: <DashboardIcon />, path: '/merchant/dashboard' },
    { text: 'Ürün Yönetimi',    icon: <ProductIcon />,   path: '/merchant/products' },
    { text: 'Siparişler',       icon: <OrderIcon />,     path: '/merchant/orders' },
    { text: 'Değerlendirmeler', icon: <ReviewIcon />,    path: '/merchant/reviews' },
    { divider: true },
    { text: 'Depo & Stok',      icon: <WarehouseIcon />, path: '/merchant/warehouses' },
    { divider: true },
    { text: 'Abonelik & Ödeme', icon: <BillingIcon />,   path: '/merchant/subscription' },
    { text: 'Mağaza Ayarları',  icon: <SettingsIcon />,  path: '/merchant/settings' },
];

const MerchantLayout: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { activeTenant, clearMerchantSession } = useMerchantStore();
    const { user } = useAuthStore();
    const auth = useAuth();

    const [mobileOpen, setMobileOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const handleSwitchStore = () => {
        clearMerchantSession();
        navigate('/merchant/select');
    };

    const handleLogout = () =>
        auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin });

    // ─── Drawer içeriği ───────────────────────────────────────────────────────
    const drawerContent = (
        <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: '#1e293b', color: '#fff' }}>
            {/* Mağaza başlığı */}
            <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar
                    variant="rounded"
                    src={activeTenant?.logoUrl ?? undefined}
                    sx={{ bgcolor: '#38bdf8', width: 40, height: 40 }}
                >
                    <StoreIcon />
                </Avatar>
                <Box sx={{ minWidth: 0, flex: 1 }}>
                    <Typography variant="subtitle2" color="#94a3b8" fontSize="0.75rem">
                        Aktif Mağaza
                    </Typography>
                    <Typography variant="body1" fontWeight="bold" noWrap title={activeTenant?.name}>
                        {activeTenant?.name ?? 'Yükleniyor...'}
                    </Typography>
                </Box>
                <Tooltip title="Mağaza Değiştir">
                    <IconButton
                        onClick={handleSwitchStore}
                        size="small"
                        sx={{ color: '#94a3b8', '&:hover': { color: '#fff' } }}
                    >
                        <SwitchStoreIcon fontSize="small" />
                    </IconButton>
                </Tooltip>
            </Box>

            <Divider sx={{ borderColor: 'rgba(255,255,255,0.1)' }} />

            <List sx={{ px: 2, mt: 2, flexGrow: 1 }}>
                {NAV_ITEMS.map((item, idx) =>
                    item.divider ? (
                        <Divider key={idx} sx={{ my: 1.5, borderColor: 'rgba(255,255,255,0.1)' }} />
                    ) : (
                        <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
                            <ListItemButton
                                selected={location.pathname === item.path}
                                onClick={() => {
                                    navigate(item.path!);
                                    setMobileOpen(false);
                                }}
                                sx={{
                                    borderRadius: 2,
                                    '&.Mui-selected': {
                                        bgcolor: '#38bdf8',
                                        color: '#fff',
                                        '&:hover': { bgcolor: '#0ea5e9' },
                                    },
                                    '&:hover': { bgcolor: 'rgba(255,255,255,0.05)' },
                                }}
                            >
                                <ListItemIcon
                                    sx={{
                                        color: location.pathname === item.path ? '#fff' : '#94a3b8',
                                        minWidth: 40,
                                    }}
                                >
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText
                                    primary={item.text}
                                    primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 500 }}
                                />
                            </ListItemButton>
                        </ListItem>
                    ),
                )}
            </List>

            <Box sx={{ p: 2, textAlign: 'center', opacity: 0.4 }}>
                <Typography variant="caption">İlhan E-Ticaret v1.0</Typography>
            </Box>
        </Box>
    );

    // ─── Render ───────────────────────────────────────────────────────────────
    return (
        // display:flex — AppBar fixed, drawer permanent, main offset
        <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f8fafc' }}>
            <CssBaseline />

            {/* ── AppBar ────────────────────────────────────────────────── */}
            <AppBar
                position="fixed"
                elevation={0}
                sx={{
                    // md ve üstünde drawer genişliği kadar sağa kaydır
                    width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
                    ml:    { md: `${DRAWER_WIDTH}px` },
                    bgcolor: '#fff',
                    color: '#334155',
                    borderBottom: '1px solid #e2e8f0',
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

                    <Typography
                        variant="h6"
                        fontWeight="bold"
                        sx={{ flexGrow: 1, color: '#1e293b' }}
                    >
                        Satıcı Paneli
                    </Typography>

                    <Tooltip title={user?.firstName ?? 'Hesap'}>
                        <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} size="small">
                            <Avatar
                                sx={{
                                    width: 36, height: 36,
                                    bgcolor: 'primary.main',
                                    fontSize: '0.9rem',
                                }}
                            >
                                {user?.firstName?.charAt(0) ?? 'U'}
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
                        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
                            <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
                            Çıkış Yap
                        </MenuItem>
                    </Menu>
                </Toolbar>
            </AppBar>

            {/* ── Mobile Drawer (temporary) ─────────────────────────────── */}
            <Drawer
                variant="temporary"
                open={mobileOpen}
                onClose={() => setMobileOpen(false)}
                ModalProps={{ keepMounted: true }}
                sx={{
                    display: { xs: 'block', md: 'none' },
                    '& .MuiDrawer-paper': {
                        width: DRAWER_WIDTH,
                        boxSizing: 'border-box',
                    },
                }}
            >
                {drawerContent}
            </Drawer>

            {/* ── Desktop Drawer (permanent) ────────────────────────────── */}
            <Drawer
                variant="permanent"
                sx={{
                    display: { xs: 'none', md: 'block' },
                    width: DRAWER_WIDTH,
                    flexShrink: 0,
                    '& .MuiDrawer-paper': {
                        width: DRAWER_WIDTH,
                        boxSizing: 'border-box',
                        // Overflow ile içerik taşmasını önle
                        overflowX: 'hidden',
                    },
                }}
            >
                {drawerContent}
            </Drawer>

            {/* ── Ana İçerik ────────────────────────────────────────────── */}
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    minWidth: 0,
                    mt: '64px',
                    p: { xs: 2, md: 4 },
                    minHeight: 'calc(100vh - 64px)',
                    overflowX: 'hidden',
                }}
            >
                <Outlet />
            </Box>
        </Box>
    );
};

export default MerchantLayout;