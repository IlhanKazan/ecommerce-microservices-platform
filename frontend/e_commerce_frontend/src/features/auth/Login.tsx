import React from 'react';
import { Typography, Box, Paper, TextField, Button, Checkbox, FormControlLabel, Link } from '@mui/material';
import { useI18n, useKcContext } from 'keycloakify';
import { Link as RouterLink } from 'react-router-dom';

const Login: React.FC = () => {
    const { t } = useI18n();
    const kcContext = useKcContext();

    const formAction = kcContext?.url?.loginAction || '';

    return (
        <Paper elevation={6} sx={{ p: 5, mx: 'auto', bgcolor: 'background.paper' }}>
            <Typography variant="h4" component="h1" gutterBottom textAlign="center" color="primary">
                E-COMMERCE
            </Typography>
            <Typography variant="h5" gutterBottom textAlign="center" sx={{ mb: 3 }}>
                {t("doLogIn")}
            </Typography>

            <Box component="form" method="POST" action={formAction}>

                <TextField
                    label={t("usernameOrEmail")}
                    name="username"
                    fullWidth
                    required
                    sx={{ mb: 2 }}
                    defaultValue={kcContext?.login?.username}
                />

                <TextField
                    label={t("password")}
                    name="password"
                    type="password"
                    fullWidth
                    required
                    sx={{ mb: 1 }}
                />

                <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
                    <FormControlLabel
                        control={<Checkbox name="rememberMe" defaultChecked={kcContext?.login?.rememberMe === 'on'} />}
                        label={t("rememberMe")}
                    />
                    {kcContext?.url?.loginResetCredentialsUrl && (
                        <Link href={kcContext.url.loginResetCredentialsUrl} variant="body2">
                            {t("doForgotPassword")}
                        </Link>
                    )}
                </Box>

                <Button
                    type="submit"
                    variant="contained"
                    color="primary"
                    fullWidth
                    name="login"
                    value="Giriş Yap"
                    size="large"
                >
                    {t("doLogIn")}
                </Button>
            </Box>

            {kcContext?.url?.registrationUrl && (
                <Box textAlign="center" sx={{ mt: 3 }}>
                    <Typography variant="body2">
                        {t("noAccount")}{" "}
                        <Link href={kcContext.url.registrationUrl} component="a">
                            {t("doRegister")}
                        </Link>
                    </Typography>
                </Box>
            )}
        </Paper>
    );
};

export default Login;