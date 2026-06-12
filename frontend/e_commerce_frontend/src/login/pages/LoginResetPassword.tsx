import { useState } from "react";
import { Box, TextField, Button, Stack, Typography, Link } from "@mui/material";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import { kcSanitize } from "keycloakify/lib/kcSanitize";

export default function LoginResetPassword(
    props: PageProps<Extract<KcContext, { pageId: "login-reset-password.ftl" }>, I18n>
) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg, msgStr } = i18n;
    const { url, realm, auth, messagesPerField } = kcContext;

    const [isSubmitting, setIsSubmitting] = useState(false);

    const usernameLabel = !realm.loginWithEmailAllowed
        ? "username"
        : !realm.registrationEmailAsUsername
            ? "usernameOrEmail"
            : "email";

    const hasError = messagesPerField.existsError("username");

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            displayMessage={!hasError}
            headerNode={msg("emailForgotTitle")}
            displayInfo
            infoNode={
                <Typography variant="body2" color="text.secondary">
                    <Link href={url.loginUrl} fontWeight={600} underline="hover">
                        {msg("backToLogin")}
                    </Link>
                </Typography>
            }
        >
            <Box
                component="form"
                action={url.loginAction}
                method="post"
                onSubmit={() => {
                    setIsSubmitting(true);
                    return true;
                }}
            >
                <Stack spacing={2.5}>
                    <Typography variant="body2" color="text.secondary">
                        {realm.loginWithEmailAllowed ? msg("emailInstruction") : msg("emailInstructionUsername")}
                    </Typography>

                    <TextField
                        id="username"
                        name="username"
                        label={msgStr(usernameLabel)}
                        fullWidth
                        autoFocus
                        autoComplete="username"
                        defaultValue={auth?.attemptedUsername ?? ""}
                        error={hasError}
                    />

                    {hasError && (
                        <Typography
                            variant="caption"
                            color="error"
                            dangerouslySetInnerHTML={{
                                __html: kcSanitize(messagesPerField.getFirstError("username"))
                            }}
                        />
                    )}

                    <Button type="submit" variant="contained" size="large" fullWidth disabled={isSubmitting}>
                        {msgStr("doSubmit")}
                    </Button>
                </Stack>
            </Box>
        </Template>
    );
}
