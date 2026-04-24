import React, { useState } from 'react';
import {
    AppBar, Toolbar, Typography, Button, IconButton, Box, Drawer,
    List, ListItem, ListItemText, ListItemButton, ListItemIcon,
    useMediaQuery, useTheme, Divider, Badge, InputBase, Paper
} from '@mui/material';
import {
    Menu as MenuIcon,
    Storefront as StoreIcon,
    AddBusiness as AddBusinessIcon,
    ShoppingCartOutlined as CartIcon,
    Search as SearchIcon,
    PersonOutline as PersonIcon,
} from '@mui/icons-material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { AppRoutes } from '../../utils/routes';
import { useAuth } from 'react-oidc-context';
import { useAuthStore } from '../../store/useAuthStore';
import { useCartStore } from '../../store/useCartStore';
import { useBasketItemCount } from '../../query/useBasketQueries';

const Header: React.FC = () => {
    const auth = useAuth();
    const { user } = useAuthStore();
    const navigate = useNavigate();
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));

    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [searchValue, setSearchValue] = useState('');

    // --- SEPET SAYISI MANTIĞINI DEĞİŞTİRDİK ---
    const localItemCount = useCartStore((state) => state.getItemCount());
    const apiItemCount = useBasketItemCount();
    const itemCount = auth.isAuthenticated ? apiItemCount : localItemCount;

    const login = () => auth.signinRedirect();
    const logout = () => auth.signoutRedirect({
        post_logout_redirect_uri: window.location.origin
    });

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        if (!searchValue.trim()) return;
        navigate(`${AppRoutes.PRODUCT_LIST}?keyword=${encodeURIComponent(searchValue.trim())}`);
        setSearchValue('');
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') handleSearch(e as any);
    };

    const publicNavItems = [
        { name: 'Anasayfa', path: AppRoutes.HOME },
        { name: 'Ürünler', path: AppRoutes.PRODUCT_LIST },
        { name: 'Hakkımızda', path: AppRoutes.ABOUT },
    ];

    const DrawerContent = (
        <Box sx={{ width: 280 }} role="presentation">
            <Box sx={{ p: 2, bgcolor: 'primary.main' }}>
                <Typography variant="h6" color="white" fontWeight="bold">
                    İlhan E-Ticaret
                </Typography>
            </Box>
            <List>
                {publicNavItems.map((item) => (
                    <ListItem key={item.name} disablePadding>
                        <ListItemButton
                            component={RouterLink}
                            to={item.path}
                            onClick={() => setIsDrawerOpen(false)}
                        >
                            <ListItemText primary={item.name} />
                        </ListItemButton>
                    </ListItem>
                ))}
                <Divider />
                {auth.isAuthenticated && (
                    <ListItem disablePadding>
                        <ListItemButton
                            component={RouterLink}
                            to={AppRoutes.CART}
                            onClick={() => setIsDrawerOpen(false)}
                        >
                            <ListItemIcon>
                                <Badge badgeContent={itemCount} color="error">
                                    <CartIcon />
                                </Badge>
                            </ListItemIcon>
                            <ListItemText primary="Sepetim" />
                        </ListItemButton>
                    </ListItem>
                )}
                <Divider />
                {auth.isAuthenticated ? (
                    <>
                        {user?.isMerchant ? (
                            <ListItem disablePadding>
                                <ListItemButton
                                    component={RouterLink}
                                    to={AppRoutes.MERCHANT_SELECT}
                                    onClick={() => setIsDrawerOpen(false)}
                                >
                                    <ListItemIcon><StoreIcon color="primary" /></ListItemIcon>
                                    <ListItemText
                                        primary="Mağaza Paneli"
                                        primaryTypographyProps={{ fontWeight: 'bold', color: 'primary.main' }}
                                    />
                                </ListItemButton>
                            </ListItem>
                        ) : (
                            <ListItem disablePadding>
                                <ListItemButton
                                    component={RouterLink}
                                    to={AppRoutes.CREATE_STORE}
                                    onClick={() => setIsDrawerOpen(false)}
                                >
                                    <ListItemIcon><AddBusinessIcon color="secondary" /></ListItemIcon>
                                    <ListItemText primary="Mağaza Aç" />
                                </ListItemButton>
                            </ListItem>
                        )}
                        <ListItem disablePadding>
                            <ListItemButton
                                component={RouterLink}
                                to={AppRoutes.ACCOUNT}
                                onClick={() => setIsDrawerOpen(false)}
                            >
                                <ListItemText primary={`Hesabım (${user?.firstName || ''})`} />
                            </ListItemButton>
                        </ListItem>
                        <ListItem disablePadding>
                            <ListItemButton onClick={logout}>
                                <ListItemText primary="Çıkış Yap" sx={{ color: 'error.main' }} />
                            </ListItemButton>
                        </ListItem>
                    </>
                ) : (
                    <ListItem disablePadding>
                        <ListItemButton onClick={login}>
                            <ListItemText primary="Giriş Yap" primaryTypographyProps={{ fontWeight: 'bold' }} />
                        </ListItemButton>
                    </ListItem>
                )}
            </List>
        </Box>
    );

    return (
        <AppBar position="sticky" color="default" elevation={1} sx={{ bgcolor: 'white' }}>
            <Toolbar
                sx={{
                    maxWidth: '1400px',
                    width: '100%',
                    mx: 'auto',
                    px: { xs: 1.5, md: 3 },
                    minHeight: { xs: 56, md: 64 },
                    display: 'flex',
                    alignItems: 'center',
                    gap: { xs: 1, md: 2 },
                }}
            >
                <Box
                    sx={{
                        flex: '1 1 0',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        minWidth: 0,
                    }}
                >
                    {isMobile && (
                        <IconButton
                            size="large"
                            edge="start"
                            color="inherit"
                            onClick={() => setIsDrawerOpen(true)}
                            sx={{ mr: 0.5 }}
                        >
                            <MenuIcon />
                        </IconButton>
                    )}

                    <Typography
                        variant="h6"
                        fontWeight="bold"
                        color="primary.main"
                        sx={{
                            cursor: 'pointer',
                            flexShrink: 0,
                            fontSize: { xs: '1rem', md: '1.2rem' },
                            letterSpacing: '-0.3px',
                            whiteSpace: 'nowrap',
                        }}
                        onClick={() => navigate(AppRoutes.HOME)}
                    >
                        İlhan E-Ticaret
                    </Typography>
                </Box>

                <Box
                    sx={{
                        flex: '2 1 0',
                        display: 'flex',
                        justifyContent: 'center',
                        maxWidth: 620,
                        mx: 'auto',
                    }}
                >
                    <Paper
                        component="form"
                        onSubmit={handleSearch}
                        elevation={0}
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            width: '100%',
                            border: '1.5px solid',
                            borderColor: 'divider',
                            borderRadius: 2,
                            px: 1.5,
                            py: 0.25,
                            transition: 'border-color 0.2s, box-shadow 0.2s',
                            '&:focus-within': {
                                borderColor: 'primary.main',
                                boxShadow: '0 0 0 3px rgba(125,85,37,0.08)',
                            },
                        }}
                    >
                        <InputBase
                            value={searchValue}
                            onChange={(e) => setSearchValue(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder={isMobile ? 'Ara...' : 'Ürün, kategori veya marka ara...'}
                            sx={{ flex: 1, fontSize: '0.9rem' }}
                            inputProps={{ 'aria-label': 'ürün ara' }}
                        />
                        <IconButton type="submit" size="small" color="primary" aria-label="ara">
                            <SearchIcon fontSize="small" />
                        </IconButton>
                    </Paper>
                </Box>

                <Box
                    sx={{
                        flex: '1 1 0',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'flex-end',
                        gap: 0.5,
                        minWidth: 0,
                    }}
                >
                    {!isMobile && (
                        <>
                            {auth.isAuthenticated ? (
                                user?.isMerchant ? (
                                    <Button
                                        variant="outlined"
                                        color="primary"
                                        startIcon={<StoreIcon />}
                                        component={RouterLink}
                                        to={AppRoutes.MERCHANT_SELECT}
                                        size="small"
                                        sx={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}
                                    >
                                        Mağazam
                                    </Button>
                                ) : (
                                    <Button
                                        variant="text"
                                        color="inherit"
                                        startIcon={<AddBusinessIcon />}
                                        component={RouterLink}
                                        to={AppRoutes.CREATE_STORE}
                                        size="small"
                                        sx={{ whiteSpace: 'nowrap' }}
                                    >
                                        Mağaza Aç
                                    </Button>
                                )
                            ) : (
                                <Button
                                    color="inherit"
                                    component={RouterLink}
                                    to={AppRoutes.CREATE_STORE}
                                    size="small"
                                    sx={{ whiteSpace: 'nowrap' }}
                                >
                                    Satış Yap
                                </Button>
                            )}

                            {auth.isAuthenticated ? (
                                <Button
                                    color="inherit"
                                    startIcon={<PersonIcon />}
                                    component={RouterLink}
                                    to={AppRoutes.ACCOUNT}
                                    size="small"
                                    sx={{ whiteSpace: 'nowrap' }}
                                >
                                    {user?.firstName || 'Hesabım'}
                                </Button>
                            ) : (
                                <Button
                                    variant="contained"
                                    onClick={login}
                                    size="small"
                                    sx={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}
                                >
                                    Giriş Yap
                                </Button>
                            )}
                        </>
                    )}

                    <IconButton
                        component={RouterLink}
                        to={AppRoutes.CART}
                        color="inherit"
                        aria-label={`Sepet, ${itemCount} ürün`}
                    >
                        <Badge badgeContent={itemCount} color="error" max={99}>
                            <CartIcon />
                        </Badge>
                    </IconButton>
                </Box>
            </Toolbar>

            <Drawer anchor="left" open={isDrawerOpen} onClose={() => setIsDrawerOpen(false)}>
                {DrawerContent}
            </Drawer>
        </AppBar>
    );
};

export default Header;