import { Box, TextField, Button, Stack, Typography } from "@mui/material";
import type { PageProps } from "keycloakify/account/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Password(props: PageProps<Extract<KcContext, { pageId: "password.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg, msgStr } = i18n;
    const { url, password, account, stateChecker, messagesPerField } = kcContext;

    const fieldError = (name: string) =>
        messagesPerField.existsError(name)
            ? { error: true, helperText: messagesPerField.get(name) }
            : {};

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} active="password">
            <Typography variant="h6" fontWeight={700}>{msg("changePasswordHtmlTitle")}</Typography>
            <Typography variant="caption" color="text.secondary">{msg("allFieldsRequired")}</Typography>

            <Box component="form" action={url.passwordUrl} method="post" sx={{ mt: 2.5 }}>
                <input type="hidden" name="stateChecker" value={stateChecker} />
                {/* tarayıcı autocomplete için gizli kullanıcı adı */}
                <input type="text" name="username" autoComplete="username" defaultValue={account.username ?? ""} readOnly hidden />

                <Stack spacing={2.5}>
                    {password.passwordSet && (
                        <TextField
                            name="password"
                            type="password"
                            label={msgStr("password")}
                            fullWidth
                            autoComplete="current-password"
                            {...fieldError("password")}
                        />
                    )}
                    <TextField
                        name="password-new"
                        type="password"
                        label={msgStr("passwordNew")}
                        fullWidth
                        autoComplete="new-password"
                        {...fieldError("password-new")}
                    />
                    <TextField
                        name="password-confirm"
                        type="password"
                        label={msgStr("passwordConfirm")}
                        fullWidth
                        autoComplete="new-password"
                        {...fieldError("password-confirm")}
                    />

                    <Stack direction="row" justifyContent="flex-end">
                        <Button type="submit" name="submitAction" value="Save" variant="contained">
                            {msg("doSave")}
                        </Button>
                    </Stack>
                </Stack>
            </Box>
        </Template>
    );
}
