import { useState } from "react";
import { Box, TextField, Button, Stack, Typography, FormControlLabel, Checkbox } from "@mui/material";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

export default function LoginUpdatePassword(
    props: PageProps<Extract<KcContext, { pageId: "login-update-password.ftl" }>, I18n>
) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg, msgStr } = i18n;
    const { url, messagesPerField, isAppInitiatedAction } = kcContext;

    const [isSubmitting, setIsSubmitting] = useState(false);
    const passwordError = messagesPerField.existsError("password", "password-confirm");

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={!passwordError}
            headerNode={msg("updatePasswordTitle")}
        >
            <Box
                component="form"
                id="kc-passwd-update-form"
                action={url.loginAction}
                method="post"
                onSubmit={() => {
                    setIsSubmitting(true);
                    return true;
                }}
            >
                <Stack spacing={2.5}>
                    <TextField
                        id="password-new"
                        name="password-new"
                        type="password"
                        label={msgStr("passwordNew")}
                        fullWidth
                        autoFocus
                        autoComplete="new-password"
                        error={passwordError}
                    />
                    <TextField
                        id="password-confirm"
                        name="password-confirm"
                        type="password"
                        label={msgStr("passwordConfirm")}
                        fullWidth
                        autoComplete="new-password"
                        error={passwordError}
                    />

                    {passwordError && (
                        <Typography
                            variant="caption"
                            color="error"
                            dangerouslySetInnerHTML={{
                                __html: kcSanitize(messagesPerField.getFirstError("password", "password-confirm"))
                            }}
                        />
                    )}

                    <FormControlLabel
                        control={<Checkbox id="logout-sessions" name="logout-sessions" value="on" defaultChecked />}
                        label={<Typography variant="body2">{msg("logoutOtherSessions")}</Typography>}
                    />

                    <Stack direction="row" spacing={1.5}>
                        <Button type="submit" variant="contained" size="large" fullWidth disabled={isSubmitting}>
                            {msgStr("doSubmit")}
                        </Button>
                        {isAppInitiatedAction && (
                            <Button type="submit" name="cancel-aia" value="true" variant="text" color="inherit" size="large">
                                {msg("doCancel")}
                            </Button>
                        )}
                    </Stack>
                </Stack>
            </Box>
        </Template>
    );
}
