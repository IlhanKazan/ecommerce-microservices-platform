import { Box, TextField, Button, Stack, Typography } from "@mui/material";
import type { PageProps } from "keycloakify/account/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Account(props: PageProps<Extract<KcContext, { pageId: "account.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg, msgStr } = i18n;
    const { url, realm, account, stateChecker, messagesPerField, referrer } = kcContext;

    const fieldError = (name: string) =>
        messagesPerField.existsError(name)
            ? { error: true, helperText: messagesPerField.get(name) }
            : {};

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} active="account">
            <Typography variant="h6" fontWeight={700}>{msg("editAccountHtmlTitle")}</Typography>
            <Typography variant="caption" color="text.secondary">* {msg("requiredFields")}</Typography>

            <Box component="form" action={url.accountUrl} method="post" sx={{ mt: 2.5 }}>
                <input type="hidden" name="stateChecker" value={stateChecker} />
                <Stack spacing={2.5}>
                    {!realm.registrationEmailAsUsername && (
                        <TextField
                            name="username"
                            label={msgStr("username")}
                            fullWidth
                            defaultValue={account.username ?? ""}
                            disabled={!realm.editUsernameAllowed}
                            {...fieldError("username")}
                        />
                    )}
                    <TextField
                        name="email"
                        type="email"
                        label={msgStr("email")}
                        fullWidth
                        required
                        defaultValue={account.email ?? ""}
                        {...fieldError("email")}
                    />
                    <TextField
                        name="firstName"
                        label={msgStr("firstName")}
                        fullWidth
                        required
                        defaultValue={account.firstName ?? ""}
                        {...fieldError("firstName")}
                    />
                    <TextField
                        name="lastName"
                        label={msgStr("lastName")}
                        fullWidth
                        required
                        defaultValue={account.lastName ?? ""}
                        {...fieldError("lastName")}
                    />

                    <Stack direction="row" spacing={1.5} justifyContent="flex-end">
                        {referrer?.url && (
                            <Button component="a" href={referrer.url} color="inherit">
                                {msg("backToApplication")}
                            </Button>
                        )}
                        <Button type="submit" name="submitAction" value="Cancel" color="inherit">
                            {msg("doCancel")}
                        </Button>
                        <Button type="submit" name="submitAction" value="Save" variant="contained">
                            {msg("doSave")}
                        </Button>
                    </Stack>
                </Stack>
            </Box>
        </Template>
    );
}
