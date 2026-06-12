import { useState } from "react";
import {
    Box, TextField, Button, Checkbox, FormControlLabel, Link, Stack, Divider, Typography
} from "@mui/material";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

export default function Login(props: PageProps<Extract<KcContext, { pageId: "login.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { social, realm, url, usernameHidden, login, registrationDisabled, messagesPerField } = kcContext;
    const { msg, msgStr } = i18n;

    const [isLoginButtonDisabled, setIsLoginButtonDisabled] = useState(false);

    const usernameLabel = !realm.loginWithEmailAllowed
        ? "username"
        : !realm.registrationEmailAsUsername
            ? "usernameOrEmail"
            : "email";

    const hasError = messagesPerField.existsError("username", "password");
    const showRegister = realm.password && realm.registrationAllowed && !registrationDisabled;

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={!hasError}
            headerNode={msg("loginAccountTitle")}
            displayInfo={showRegister}
            infoNode={
                showRegister ? (
                    <Typography variant="body2" color="text.secondary">
                        {msg("noAccount")}{" "}
                        <Link href={url.registrationUrl} fontWeight={600} underline="hover">
                            {msg("doRegister")}
                        </Link>
                    </Typography>
                ) : null
            }
            socialProvidersNode={
                realm.password && social?.providers?.length ? (
                    <Box>
                        <Divider sx={{ mb: 2 }}>
                            <Typography variant="caption" color="text.secondary">
                                {msg("identity-provider-login-label")}
                            </Typography>
                        </Divider>
                        <Stack spacing={1}>
                            {social.providers.map((p) => (
                                <Button
                                    key={p.alias}
                                    variant="outlined"
                                    color="inherit"
                                    fullWidth
                                    href={p.loginUrl}
                                    sx={{ textTransform: "none" }}
                                >
                                    {p.displayName}
                                </Button>
                            ))}
                        </Stack>
                    </Box>
                ) : null
            }
        >
            {realm.password && (
                <Box
                    component="form"
                    id="kc-form-login"
                    action={url.loginAction}
                    method="post"
                    onSubmit={() => {
                        setIsLoginButtonDisabled(true);
                        return true;
                    }}
                >
                    <Stack spacing={2.5}>
                        {!usernameHidden && (
                            <TextField
                                id="username"
                                name="username"
                                label={msgStr(usernameLabel)}
                                fullWidth
                                autoFocus
                                autoComplete="username"
                                defaultValue={login.username ?? ""}
                                error={hasError}
                            />
                        )}

                        <TextField
                            id="password"
                            name="password"
                            type="password"
                            label={msgStr("password")}
                            fullWidth
                            autoComplete="current-password"
                            error={hasError}
                        />

                        {hasError && (
                            <Typography
                                variant="caption"
                                color="error"
                                dangerouslySetInnerHTML={{
                                    __html: kcSanitize(messagesPerField.getFirstError("username", "password"))
                                }}
                            />
                        )}

                        <Box display="flex" justifyContent="space-between" alignItems="center">
                            {realm.rememberMe && !usernameHidden ? (
                                <FormControlLabel
                                    control={<Checkbox name="rememberMe" defaultChecked={Boolean(login.rememberMe)} />}
                                    label={<Typography variant="body2">{msg("rememberMe")}</Typography>}
                                />
                            ) : (
                                <span />
                            )}
                            {realm.resetPasswordAllowed && (
                                <Link href={url.loginResetCredentialsUrl} variant="body2" underline="hover">
                                    {msg("doForgotPassword")}
                                </Link>
                            )}
                        </Box>

                        <input
                            type="hidden"
                            id="id-hidden-input"
                            name="credentialId"
                            value={kcContext.auth?.selectedCredential ?? ""}
                        />

                        <Button
                            type="submit"
                            name="login"
                            id="kc-login"
                            variant="contained"
                            size="large"
                            fullWidth
                            disabled={isLoginButtonDisabled}
                        >
                            {msgStr("doLogIn")}
                        </Button>
                    </Stack>
                </Box>
            )}
        </Template>
    );
}
