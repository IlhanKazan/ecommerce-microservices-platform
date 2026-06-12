import { useEffect } from "react";
import { ThemeProvider } from "@mui/material/styles";
import { Box, Paper, Typography, Alert, Stack, CssBaseline, GlobalStyles, Link } from "@mui/material";
import customTheme from "../utils/customTheme";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import type { KcContext } from "./KcContext";
import type { I18n } from "./i18n";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

/**
 * Tüm login/register/auth ekranlarının ortak kabuğu.
 * Keycloak'ın default Template'i yerine geçer; uygulamanın `customTheme`'i + markası ile
 * MUI kartı içinde render eder. `useInitialize` Keycloak'ın runtime script'lerini yükler.
 */
export default function Template(props: TemplateProps<KcContext, I18n>) {
    const {
        displayInfo = false,
        displayMessage = true,
        headerNode,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        kcContext,
        i18n,
        doUseDefaultCss,
        children
    } = props;

    const { msg, msgStr } = i18n;
    const { realm, auth, url, message, isAppInitiatedAction } = kcContext;

    useEffect(() => {
        document.title = documentTitle ?? msgStr("loginTitle", realm.displayName);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });

    if (!isReadyToRender) {
        return null;
    }

    const severity =
        message?.type === "error" ? "error" :
        message?.type === "warning" ? "warning" :
        message?.type === "success" ? "success" : "info";

    return (
        <ThemeProvider theme={customTheme}>
            <CssBaseline />
            <GlobalStyles styles={{ body: { margin: 0 } }} />
            <Box
                sx={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    bgcolor: "background.default",
                    p: 2
                }}
            >
                <Paper elevation={3} sx={{ width: "100%", maxWidth: 440, p: { xs: 3, sm: 4.5 }, borderRadius: 3 }}>
                    <Stack spacing={3}>
                        <Box textAlign="center">
                            <Typography variant="h5" fontWeight={800} color="primary" letterSpacing={0.5}>
                                İlhan E-Ticaret
                            </Typography>
                            {headerNode != null && (
                                <Typography variant="subtitle1" color="text.secondary" sx={{ mt: 0.5 }}>
                                    {headerNode}
                                </Typography>
                            )}
                        </Box>

                        {displayMessage && message != null && (message.type !== "warning" || !isAppInitiatedAction) && (
                            <Alert severity={severity} variant="outlined">
                                <span dangerouslySetInnerHTML={{ __html: kcSanitize(message.summary) }} />
                            </Alert>
                        )}

                        {children}

                        {socialProvidersNode}

                        {auth?.showTryAnotherWayLink && (
                            <Box component="form" id="kc-select-try-another-way-form" action={url.loginAction} method="post" textAlign="center">
                                <input type="hidden" name="tryAnotherWay" value="on" />
                                <Link component="button" type="submit" variant="body2" underline="hover">
                                    {msg("doTryAnotherWay")}
                                </Link>
                            </Box>
                        )}

                        {displayInfo && infoNode != null && (
                            <Box textAlign="center">{infoNode}</Box>
                        )}
                    </Stack>
                </Paper>
            </Box>
        </ThemeProvider>
    );
}
