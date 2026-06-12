import { useState, type ReactElement } from "react";
import { Box, Button, Stack, Typography, FormControlLabel, Checkbox, Link } from "@mui/material";
import type { PageProps } from "keycloakify/login/pages/PageProps";
import type { KcContext } from "../KcContext";
import type { I18n } from "../i18n";
import type { LazyOrNot } from "keycloakify/tools/LazyOrNot";
import type { UserProfileFormFieldsProps } from "keycloakify/login/UserProfileFormFieldsProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";

type RegisterProps = PageProps<Extract<KcContext, { pageId: "register.ftl" }>, I18n> & {
    UserProfileFormFields: LazyOrNot<(props: UserProfileFormFieldsProps) => ReactElement>;
    doMakeUserConfirmPassword: boolean;
};

export default function Register(props: RegisterProps) {
    const { kcContext, i18n, doUseDefaultCss, Template, classes, UserProfileFormFields, doMakeUserConfirmPassword } = props;
    const { msg, msgStr } = i18n;
    const { url, messagesPerField, recaptchaRequired, recaptchaVisible, recaptchaSiteKey, termsAcceptanceRequired } = kcContext;

    // UserProfileFormFields kc-class tabanlı render eder; kcClsx onun için gerekli.
    const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

    const [isFormSubmittable, setIsFormSubmittable] = useState(false);
    const [areTermsAccepted, setAreTermsAccepted] = useState(false);

    return (
        <Template
            kcContext={kcContext}
            i18n={i18n}
            doUseDefaultCss={doUseDefaultCss}
            classes={classes}
            headerNode={msg("registerTitle")}
            displayInfo
            infoNode={
                <Typography variant="body2" color="text.secondary">
                    <Link href={url.loginUrl} fontWeight={600} underline="hover">
                        {msg("backToLogin")}
                    </Link>
                </Typography>
            }
        >
            <Box component="form" id="kc-register-form" action={url.registrationAction} method="post">
                <Stack spacing={2.5}>
                    <UserProfileFormFields
                        kcContext={kcContext}
                        i18n={i18n}
                        kcClsx={kcClsx}
                        onIsFormSubmittableValueChange={setIsFormSubmittable}
                        doMakeUserConfirmPassword={doMakeUserConfirmPassword}
                    />

                    {termsAcceptanceRequired && (
                        <Box>
                            <Typography variant="subtitle2" gutterBottom>
                                {msg("termsTitle")}
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                                {msg("termsText")}
                            </Typography>
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        name="termsAccepted"
                                        checked={areTermsAccepted}
                                        onChange={(e) => setAreTermsAccepted(e.target.checked)}
                                        color={messagesPerField.existsError("termsAccepted") ? "error" : "primary"}
                                    />
                                }
                                label={<Typography variant="body2">{msg("acceptTerms")}</Typography>}
                            />
                        </Box>
                    )}

                    {recaptchaRequired && recaptchaVisible && (
                        <Box className="g-recaptcha" data-size="compact" data-sitekey={recaptchaSiteKey} />
                    )}

                    <Button
                        type="submit"
                        variant="contained"
                        size="large"
                        fullWidth
                        disabled={!isFormSubmittable || (termsAcceptanceRequired && !areTermsAccepted)}
                    >
                        {msgStr("doRegister")}
                    </Button>
                </Stack>
            </Box>
        </Template>
    );
}
