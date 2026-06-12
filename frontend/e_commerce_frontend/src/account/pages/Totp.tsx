import {
    Box, Typography, TextField, Button, Stack, Divider, List, ListItem, ListItemText, IconButton, Link, Alert
} from "@mui/material";
import { Delete as DeleteIcon } from "@mui/icons-material";
import type { PageProps } from "keycloakify/account/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Totp(props: PageProps<Extract<KcContext, { pageId: "totp.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg, msgStr } = i18n;
    const { totp, url, stateChecker, mode } = kcContext;

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} active="totp">
            <Typography variant="h6" fontWeight={700} gutterBottom>{msg("authenticatorTitle")}</Typography>

            {/* Kayıtlı authenticator'lar */}
            {totp.enabled && totp.otpCredentials.length > 0 && (
                <Box sx={{ mb: 4 }}>
                    <List disablePadding>
                        {totp.otpCredentials.map((credential, i) => (
                            <ListItem
                                key={i}
                                disableGutters
                                secondaryAction={
                                    <Box component="form" action={url.totpUrl} method="post">
                                        <input type="hidden" name="stateChecker" value={stateChecker} />
                                        <input type="hidden" name="submitAction" value="Delete" />
                                        <input type="hidden" name="credentialId" value={credential.id} />
                                        <IconButton type="submit" color="error" edge="end"><DeleteIcon /></IconButton>
                                    </Box>
                                }
                            >
                                <ListItemText primary={credential.id} />
                            </ListItem>
                        ))}
                    </List>
                    <Divider sx={{ mt: 2 }} />
                </Box>
            )}

            {/* Yeni authenticator kurulumu */}
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>{msg("configureAuthenticators")}</Typography>

            {totp.supportedApplications?.length > 0 && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    {msg("mobile")}: {totp.supportedApplications.join(", ")}
                </Typography>
            )}

            {mode === "manual" ? (
                <Alert severity="info" sx={{ mb: 3 }}>
                    <Typography variant="body2">{msg("totpType")}: {totp.policy.type} · {msg("totpDigits")}: {totp.policy.digits}</Typography>
                    <Typography variant="body2" sx={{ wordBreak: "break-all", my: 1 }}><strong>{totp.totpSecretEncoded}</strong></Typography>
                    <Link href={totp.qrUrl} underline="hover">{msg("totpScanBarcode")}</Link>
                </Alert>
            ) : (
                <Box sx={{ mb: 3, textAlign: "center" }}>
                    <Box
                        component="img"
                        src={`data:image/png;base64,${totp.totpSecretQrCode}`}
                        alt="QR"
                        sx={{ width: 180, height: 180, border: "1px solid", borderColor: "divider", borderRadius: 1 }}
                    />
                    <Box sx={{ mt: 1 }}>
                        <Link href={totp.manualUrl} underline="hover">{msg("totpUnableToScan")}</Link>
                    </Box>
                </Box>
            )}

            <Box component="form" action={url.totpUrl} method="post">
                <input type="hidden" name="stateChecker" value={stateChecker} />
                <input type="hidden" name="totpSecret" value={totp.totpSecret} />
                <Stack spacing={2.5}>
                    <TextField id="totp" name="totp" label={msgStr("authenticatorCode")} required fullWidth autoComplete="off" />
                    <TextField id="userLabel" name="userLabel" label={msgStr("totpDeviceName")} fullWidth />
                    <Stack direction="row" spacing={1.5} justifyContent="flex-end">
                        <Button type="submit" name="submitAction" value="Save" variant="contained">{msg("doSave")}</Button>
                        <Button type="submit" name="submitAction" value="Cancel" color="inherit">{msg("doCancel")}</Button>
                    </Stack>
                </Stack>
            </Box>
        </Template>
    );
}
