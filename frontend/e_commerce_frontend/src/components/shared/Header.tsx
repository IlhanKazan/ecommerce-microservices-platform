import React, { useState } from 'react';
import {
    AppBar, Toolbar, Typography, Button, IconButton, Box, Drawer,
    List, ListItem, ListItemText, ListItemButton, ListItemIcon,
    useMediaQuery, useTheme, Divider
} from '@mui/material';
import {
    Menu as MenuIcon,
    Storefront as StoreIcon,
    AddBusiness as AddBusinessIcon
} from '@mui/icons-material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { AppRoutes } from "../../utils/routes";
import { useAuth } from "react-oidc-context";
import { useAuthStore } from "../../store/useAuthStore";

const Header: React.FC = () => {
    const auth = useAuth();
    const { user } = useAuthStore();
    const navigate = useNavigate();

    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('md'));

    const login = () => auth.signinRedirect();
    const logout = () => auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin });

    const toggleDrawer = (open: boolean) => (event: React.KeyboardEvent | React.MouseEvent) => {
        if (event.type === 'keydown' && ((event as React.KeyboardEvent).key === 'Tab' || (event as React.KeyboardEvent).key === 'Shift')) return;
        setIsDrawerOpen(open);
    };

    const publicNavItems = [
        { name: 'Anasayfa', path: AppRoutes.HOME },
        { name: 'Hakkımızda', path: AppRoutes.ABOUT },
        { name: 'İletişim', path: AppRoutes.CONTACT },
    ];

    const DrawerContent = (
        <Box sx={{ width: 250 }} role="presentation" onClick={toggleDrawer(false)} onKeyDown={toggleDrawer(false)}>
            <List>
                {publicNavItems.map((item) => (
                    <ListItem key={item.name} disablePadding>
                        <ListItemButton component={RouterLink} to={`${item.path}`}>
                            <ListItemText primary={item.name} />
                        </ListItemButton>
                    </ListItem>
                ))}
                <Divider />

                {auth.isAuthenticated ? (
                    <ListItem disablePadding>
                        {user?.isMerchant ? (
                            <ListItemButton component={RouterLink} to="/merchant/select">
                                <ListItemIcon><StoreIcon color="primary" /></ListItemIcon>
                                <ListItemText primary="Mağaza Paneli" primaryTypographyProps={{ fontWeight: 'bold', color: 'primary.main' }} />
                            </ListItemButton>
                        ) : (
                            <ListItemButton component={RouterLink} to={AppRoutes.CREATE_STORE}>
                                <ListItemIcon><AddBusinessIcon color="secondary" /></ListItemIcon>
                                <ListItemText primary="Mağaza Aç & Satış Yap" primaryTypographyProps={{ fontWeight: 'bold', color: 'secondary.main' }} />
                            </ListItemButton>
                        )}
                    </ListItem>
                ) : (
                    <ListItem disablePadding>
                        <ListItemButton component={RouterLink} to={AppRoutes.CREATE_STORE}>
                            <ListItemIcon><AddBusinessIcon /></ListItemIcon>
                            <ListItemText primary="Satış Yap" />
                        </ListItemButton>
                    </ListItem>
                )}

                <Divider />

                {auth.isAuthenticated ? (
                    <>
                        <ListItem disablePadding>
                            <ListItemButton component={RouterLink} to={AppRoutes.ACCOUNT}>
                                <ListItemText primary="Hesabım" />
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
                            <ListItemText primary="Giriş Yap" sx={{ fontWeight: 'bold' }} />
                        </ListItemButton>
                    </ListItem>
                )}
            </List>
        </Box>
    );

    return (
        <AppBar position="static" color="default" elevation={1}>
            <Toolbar sx={{ maxWidth: '1200px', width: '100%', margin: '0 auto' }}>
                {isMobile && (
                    <IconButton size="large" edge="start" color="inherit" onClick={toggleDrawer(true)} sx={{ mr: 2 }}>
                        <MenuIcon />
                    </IconButton>
                )}

                <Typography variant="h6" component="div" sx={{ flexGrow: 1, cursor: 'pointer', fontWeight: 'bold', color: 'primary.main' }} onClick={() => navigate('/')}>
                    İlhan E-Ticaret
                </Typography>

                {!isMobile && (
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                        {publicNavItems.map((item) => (
                            <Button key={item.name} color="inherit" component={RouterLink} to={`${item.path}`}>
                                {item.name}
                            </Button>
                        ))}

                        {auth.isAuthenticated ? (
                            user?.isMerchant ? (
                                <Button
                                    variant="contained"
                                    color="secondary"
                                    startIcon={<StoreIcon />}
                                    component={RouterLink}
                                    to="/merchant/select"
                                    sx={{ ml: 2, borderRadius: 2, fontWeight: 'bold' }}
                                >
                                    Mağaza Paneli
                                </Button>
                            ) : (
                                <Button
                                    variant="outlined"
                                    color="secondary"
                                    startIcon={<AddBusinessIcon />}
                                    component={RouterLink}
                                    to={AppRoutes.CREATE_STORE}
                                    sx={{ ml: 2, borderWidth: 2, fontWeight: 'bold', '&:hover': { borderWidth: 2 } }}
                                >
                                    Mağaza Aç
                                </Button>
                            )
                        ) : (
                            <Button color="inherit" component={RouterLink} to={AppRoutes.CREATE_STORE} sx={{ ml: 2 }}>
                                Satış Yap
                            </Button>
                        )}

                        {auth.isAuthenticated ? (
                            <>
                                <Button color="inherit" component={RouterLink} to={AppRoutes.ACCOUNT} sx={{ ml: 1 }}>
                                    Hesabım
                                </Button>
                                <Button color="error" onClick={logout} sx={{ ml: 1 }}>Çıkış</Button>
                            </>
                        ) : (
                            <Button variant="contained" onClick={login} sx={{ ml: 2 }}>Giriş Yap</Button>
                        )}
                    </Box>
                )}
            </Toolbar>
            <Drawer anchor="left" open={isDrawerOpen} onClose={toggleDrawer(false)}>
                {DrawerContent}
            </Drawer>
        </AppBar>
    );
};

export default Header;