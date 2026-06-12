import {
    ThemeProvider
} from "@mui/material/styles";
import {
    Box, Container, Paper, Typography, Alert, Tabs, Tab, Button, CssBaseline, Stack, GlobalStyles
} from "@mui/material";
import { Logout as LogoutIcon, ArrowBack as ArrowBackIcon } from "@mui/icons-material";
import customTheme from "../utils/customTheme";
import type { TemplateProps } from "keycloakify/account/TemplateProps";
import type { KcContext } from "./KcContext";
import type { I18n } from "./i18n";
import { useInitialize } from "keycloakify/account/Template.useInitialize";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

export default function Template(props: TemplateProps<KcContext, I18n>) {
    const { children, active, kcContext, i18n, doUseDefaultCss } = props;

    const { msg, msgStr } = i18n;
    const { url, features, referrer, message } = kcContext;

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });
    if (!isReadyToRender) {
        return null;
    }

    const navItems: { id: string; label: string; href: string }[] = [
        { id: "account", label: msgStr("account"), href: url.accountUrl },
        ...(features.passwordUpdateSupported ? [{ id: "password", label: msgStr("password"), href: url.passwordUrl }] : []),
        { id: "totp", label: msgStr("authenticator"), href: url.totpUrl },
        ...(features.identityFederation ? [{ id: "social", label: msgStr("federatedIdentity"), href: url.socialUrl }] : []),
        { id: "sessions", label: msgStr("sessions"), href: url.sessionsUrl },
        { id: "applications", label: msgStr("applications"), href: url.applicationsUrl },
        ...(features.log ? [{ id: "log", label: msgStr("log"), href: url.logUrl }] : []),
    ];

    const tabsValue = navItems.some((i) => i.id === active) ? active : false;

    const severity =
        message?.type === "error" ? "error" :
        message?.type === "warning" ? "warning" :
        message?.type === "success" ? "success" : "info";

    return (
        <ThemeProvider theme={customTheme}>
            <CssBaseline />
            {/* Sayfalar arası scrollbar kaynaklı yatay kaymayı önler */}
            <GlobalStyles styles={{ body: { overflowY: "scroll" } }} />
            <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
                {/* Üst bar */}
                <Box sx={{ bgcolor: "background.paper", borderBottom: "1px solid", borderColor: "divider" }}>
                    <Container maxWidth="md">
                        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 2 }}>
                            <Typography variant="h6" fontWeight={800} color="primary">
                                İlhan E-Ticaret
                            </Typography>
                            <Stack direction="row" spacing={1}>
                                {referrer?.url && (
                                    <Button component="a" href={referrer.url} startIcon={<ArrowBackIcon />} color="inherit" size="small">
                                        {referrer.name ?? msgStr("backToApplication")}
                                    </Button>
                                )}
                                <Button component="a" href={url.getLogoutUrl()} startIcon={<LogoutIcon />} color="inherit" size="small">
                                    {msg("doSignOut")}
                                </Button>
                            </Stack>
                        </Stack>
                    </Container>
                </Box>

                <Container maxWidth="md" sx={{ py: 4 }}>
                    {/* Nav */}
                    <Tabs value={tabsValue} variant="scrollable" scrollButtons="auto" sx={{ mb: 3 }}>
                        {navItems.map((item) => (
                            <Tab
                                key={item.id}
                                value={item.id}
                                label={item.label}
                                component="a"
                                href={item.href}
                                sx={{ textTransform: "none" }}
                            />
                        ))}
                    </Tabs>

                    {message !== undefined && (
                        <Alert severity={severity} variant="outlined" sx={{ mb: 3 }}>
                            <span dangerouslySetInnerHTML={{ __html: kcSanitize(message.summary) }} />
                        </Alert>
                    )}

                    <Paper elevation={0} variant="outlined" sx={{ p: { xs: 2.5, sm: 4 }, borderRadius: 3 }}>
                        {children}
                    </Paper>
                </Container>
            </Box>
        </ThemeProvider>
    );
}
