import React, { useRef, useState } from 'react';
import {
    AppBar, Toolbar, Typography, Button, IconButton, Box, Drawer,
    List, ListItem, ListItemText, ListItemButton, ListItemIcon,
    useMediaQuery, useTheme, Divider, Badge, InputBase, Paper,
    Autocomplete, Avatar, ListItemAvatar,
} from '@mui/material';
import {
    Menu as MenuIcon,
    Storefront as StoreIcon,
    AddBusiness as AddBusinessIcon,
    ShoppingCartOutlined as CartIcon,
    Search as SearchIcon,
    AdminPanelSettings as AdminPanelSettingsIcon,
    HistoryOutlined as HistoryIcon,
} from '@mui/icons-material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { AppRoutes } from '../../utils/routes';
import { useAuth } from 'react-oidc-context';
import { useAuthStore } from '../../store/useAuthStore';
import { useCartStore } from '../../store/useCartStore';
import { useBasketItemCount } from '../../query/useBasketQueries';
import { useAutocomplete } from '../../query/useProductQueries';
import { useRecentSearches } from '../../query/useUserQueries';
import { userService } from '../../features/user/api/userService';
import { useDebounce } from '../../hooks/useDebounce';
import type { AutocompleteSuggestion } from '../../types/product';

const Header: React.FC = () => {
    const auth = useAuth();
    const { user } = useAuthStore();
    // Sepet için auth kaynağı: ProductCard/CartPage ile aynı (useAuthStore) — react-oidc ile desync olmasın
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const navigate = useNavigate();
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));

    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [inputValue, setInputValue] = useState('');
    // freeSolo Enter'da onChange + onSubmit çift ateşlenir — bu flag ile ikincisi engellenir
    const navigatedRef = useRef(false);

    const debouncedQ = useDebounce(inputValue, 300);
    const { data: suggestions = [] } = useAutocomplete(debouncedQ);

    // Arama çubuğu boş + giriş yapılmışsa son aramaları göster; yazınca canlı öneriler.
    const { data: recentSearches = [] } = useRecentSearches(isAuthenticated);
    const showRecent = isAuthenticated && !inputValue.trim();
    const searchOptions: (AutocompleteSuggestion | string)[] = showRecent ? recentSearches : suggestions;

    // --- SEPET SAYISI MANTIĞINI DEĞİŞTİRDİK ---
    const localItemCount = useCartStore((state) => state.getItemCount());
    const apiItemCount = useBasketItemCount();
    const itemCount = isAuthenticated ? apiItemCount : localItemCount;

    const login = () => auth.signinRedirect();
    const logout = () => auth.signoutRedirect({
        post_logout_redirect_uri: window.location.origin
    });

    const handleSearchSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (navigatedRef.current) {
            navigatedRef.current = false;
            return;
        }
        if (!inputValue.trim()) return;
        if (isAuthenticated) userService.recordSearch(inputValue.trim()); // best-effort: son aramalar + AI sinyali
        navigate(`${AppRoutes.PRODUCT_LIST}?keyword=${encodeURIComponent(inputValue.trim())}`);
        setInputValue('');
    };

    const handleOptionChange = (_: unknown, option: AutocompleteSuggestion | string | null) => {
        if (!option) return;
        if (typeof option === 'object') {
            navigate(`/product/${option.id}`);
            setInputValue('');
        } else if (option.trim()) {
            navigatedRef.current = true;
            if (isAuthenticated) userService.recordSearch(option.trim());
            navigate(`${AppRoutes.PRODUCT_LIST}?keyword=${encodeURIComponent(option.trim())}`);
            setInputValue('');
        }
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
                        {user?.isPlatformAdmin && (
                            <ListItem disablePadding>
                                <ListItemButton
                                    component={RouterLink}
                                    to={AppRoutes.ADMIN_DASHBOARD}
                                    onClick={() => setIsDrawerOpen(false)}
                                >
                                    <ListItemIcon><AdminPanelSettingsIcon color="error" /></ListItemIcon>
                                    <ListItemText
                                        primary="Admin Paneli"
                                        primaryTypographyProps={{ fontWeight: 'bold', color: 'error.main' }}
                                    />
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
                    <Autocomplete<AutocompleteSuggestion | string, false, false, true>
                        freeSolo
                        disableClearable
                        openOnFocus
                        filterOptions={(x) => x}
                        options={searchOptions}
                        getOptionLabel={(opt) => (typeof opt === 'string' ? opt : opt.name)}
                        inputValue={inputValue}
                        onInputChange={(_, val, reason) => {
                            if (reason !== 'reset') setInputValue(val);
                        }}
                        onChange={handleOptionChange}
                        renderOption={(props, option) => {
                            const { key, ...rest } = props as { key: React.Key } & React.HTMLAttributes<HTMLLIElement>;
                            // Son arama (string) — geçmiş ikonlu basit satır
                            if (typeof option === 'string') {
                                return (
                                    <ListItem key={key} {...rest} dense disablePadding sx={{ px: 1.5, py: 0.5 }}>
                                        <ListItemIcon sx={{ minWidth: 36 }}>
                                            <HistoryIcon fontSize="small" color="action" />
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={option}
                                            primaryTypographyProps={{ variant: 'body2', noWrap: true }}
                                        />
                                    </ListItem>
                                );
                            }
                            return (
                                <ListItem key={key} {...rest} dense disablePadding sx={{ px: 1.5, py: 0.5 }}>
                                    <ListItemAvatar sx={{ minWidth: 44 }}>
                                        <Avatar
                                            src={option.mainImageUrl ?? undefined}
                                            alt={option.name}
                                            sx={{ width: 32, height: 32, fontSize: '0.8rem' }}
                                        >
                                            {option.name[0]?.toUpperCase()}
                                        </Avatar>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={option.name}
                                        secondary={`${option.price.toLocaleString('tr-TR')} ${option.currency}`}
                                        primaryTypographyProps={{ variant: 'body2', noWrap: true }}
                                        secondaryTypographyProps={{ variant: 'caption' }}
                                    />
                                </ListItem>
                            );
                        }}
                        renderInput={(params) => (
                            <Paper
                                component="form"
                                onSubmit={handleSearchSubmit}
                                ref={params.InputProps.ref}
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
                                        boxShadow: '0 0 0 3px rgba(242,122,26,0.15)',
                                    },
                                }}
                            >
                                <InputBase
                                    inputProps={params.inputProps}
                                    placeholder={isMobile ? 'Ara...' : 'Ürün, kategori veya marka ara...'}
                                    sx={{ flex: 1, fontSize: '0.9rem' }}
                                />
                                <IconButton type="submit" size="small" color="primary" aria-label="ara">
                                    <SearchIcon fontSize="small" />
                                </IconButton>
                            </Paper>
                        )}
                        sx={{ width: '100%' }}
                    />
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
                                <>
                                {user?.isPlatformAdmin && (
                                    <Button
                                        variant="outlined"
                                        color="error"
                                        startIcon={<AdminPanelSettingsIcon />}
                                        component={RouterLink}
                                        to={AppRoutes.ADMIN_DASHBOARD}
                                        size="small"
                                        sx={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}
                                    >
                                        Admin
                                    </Button>
                                )}
                                {user?.isMerchant ? (
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
                                )}
                                </>
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
                                    startIcon={
                                        <Avatar
                                            src={user?.profileImageUrl ?? undefined}
                                            sx={{ width: 26, height: 26, bgcolor: 'primary.main', fontSize: '0.8rem', fontWeight: 700 }}
                                        >
                                            {(user?.firstName ?? 'H').charAt(0).toUpperCase()}
                                        </Avatar>
                                    }
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