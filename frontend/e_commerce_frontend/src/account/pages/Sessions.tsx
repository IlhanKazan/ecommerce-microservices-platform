import {
    Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Button, Stack, Chip
} from "@mui/material";
import type { PageProps } from "keycloakify/account/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";

export default function Sessions(props: PageProps<Extract<KcContext, { pageId: "sessions.ftl" }>, I18n>) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes } = props;
    const { msg } = i18n;
    const { sessions, url, stateChecker } = kcContext;

    const fmt = (v: unknown) => {
        const n = Number(v);
        return Number.isFinite(n) && n > 1_000_000 ? new Date(n * 1000).toLocaleString("tr-TR") : String(v ?? "");
    };

    const clientName = (c: unknown) =>
        typeof c === "string" ? c : ((c as { clientId?: string; clientName?: string })?.clientId
            ?? (c as { clientName?: string })?.clientName ?? "");

    return (
        <Template kcContext={kcContext} i18n={i18n} doUseDefaultCss={doUseDefaultCss} classes={classes} active="sessions">
            <Typography variant="h6" fontWeight={700} gutterBottom>{msg("sessionsHtmlTitle")}</Typography>

            <TableContainer>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>{msg("ip")}</TableCell>
                            <TableCell>{msg("started")}</TableCell>
                            <TableCell>{msg("lastAccess")}</TableCell>
                            <TableCell>{msg("expires")}</TableCell>
                            <TableCell>{msg("clients")}</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {sessions.sessions.map((session, i) => (
                            <TableRow key={i}>
                                <TableCell>{session.ipAddress}</TableCell>
                                <TableCell>{fmt(session.started)}</TableCell>
                                <TableCell>{fmt(session.lastAccess)}</TableCell>
                                <TableCell>{fmt(session.expires)}</TableCell>
                                <TableCell>
                                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                                        {session.clients.map((c, j) => (
                                            <Chip key={j} size="small" label={clientName(c)} />
                                        ))}
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Box component="form" action={url.sessionsUrl} method="post" sx={{ mt: 3 }}>
                <input type="hidden" name="stateChecker" value={stateChecker} />
                <Stack direction="row" justifyContent="flex-end">
                    <Button type="submit" variant="outlined" color="error">
                        {msg("doLogOutAllSessions")}
                    </Button>
                </Stack>
            </Box>
        </Template>
    );
}
