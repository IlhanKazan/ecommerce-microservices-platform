import {
    Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Stack, Chip, Link
} from "@mui/material";
import type { PageProps } from "keycloakify/account/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Applications(props: PageProps<Extract<KcContext, { pageId: "applications.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg } = i18n;
    const { url, stateChecker } = kcContext;

    // Defansif: runtime'da beklenmedik/eksik veri beyaz ekrana yol açmasın.
    const appList = kcContext.applications?.applications ?? [];

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} active="applications">
            <Typography variant="h6" fontWeight={700} gutterBottom>{msg("applicationsHtmlTitle")}</Typography>

            {appList.length === 0 ? (
                <Typography variant="body2" color="text.secondary">—</Typography>
            ) : (
                <TableContainer>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>{msg("application")}</TableCell>
                                <TableCell>{msg("grantedPermissions")}</TableCell>
                                <TableCell align="right">{msg("action")}</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {appList.map((application, i) => {
                                const client = application?.client ?? {};
                                const name = client.name || client.clientId || "—";
                                const grants = [
                                    ...(application?.clientScopesGranted ?? []),
                                    ...(application?.additionalGrants ?? [])
                                ];
                                return (
                                    <TableRow key={i}>
                                        <TableCell>
                                            {application?.effectiveUrl ? (
                                                <Link href={application.effectiveUrl} underline="hover">{name}</Link>
                                            ) : name}
                                        </TableCell>
                                        <TableCell>
                                            <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                                                {grants.length === 0
                                                    ? <Typography variant="body2" color="text.disabled">—</Typography>
                                                    : grants.map((g, j) => <Chip key={j} size="small" label={String(g)} />)}
                                            </Stack>
                                        </TableCell>
                                        <TableCell align="right">
                                            {client.clientId && (
                                                <Box component="form" action={url.applicationsUrl} method="post">
                                                    <input type="hidden" name="stateChecker" value={stateChecker} />
                                                    <input type="hidden" name="referrer" value="applications" />
                                                    <input type="hidden" name="clientId" value={client.clientId} />
                                                    <Button type="submit" size="small" color="error">{msg("revoke")}</Button>
                                                </Box>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}
        </Template>
    );
}
