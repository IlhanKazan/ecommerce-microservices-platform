import type { ClassKey } from "keycloakify/account";
import type { KcContext } from "./KcContext";
import { useI18n } from "./i18n";
import DefaultPage from "keycloakify/account/DefaultPage";
import Template from "./Template";
import Account from "./pages/Account";
import Password from "./pages/Password";
import Sessions from "./pages/Sessions";
import Applications from "./pages/Applications";
import Totp from "./pages/Totp";

export default function KcPage(props: { kcContext: KcContext }) {
    const { kcContext } = props;

    const { i18n } = useI18n({ kcContext });

    return (
        <>
            {(() => {
                switch (kcContext.pageId) {
                    case "account.ftl":
                        return (
                            <Account
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={false}
                            />
                        );
                    case "password.ftl":
                        return (
                            <Password
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={false}
                            />
                        );
                    case "totp.ftl":
                        return (
                            <Totp kcContext={kcContext} i18n={i18n} classes={classes} Template={Template} doUseDefaultCss={false} />
                        );
                    case "sessions.ftl":
                        return (
                            <Sessions kcContext={kcContext} i18n={i18n} classes={classes} Template={Template} doUseDefaultCss={false} />
                        );
                    case "applications.ftl":
                        return (
                            <Applications kcContext={kcContext} i18n={i18n} classes={classes} Template={Template} doUseDefaultCss={false} />
                        );
                    // Log, FederatedIdentity... → temalı kabukta default gövde
                    default:
                        return (
                            <DefaultPage
                                kcContext={kcContext}
                                i18n={i18n}
                                classes={classes}
                                Template={Template}
                                doUseDefaultCss={true}
                            />
                        );
                }
            })()}
        </>
    );
}

const classes = {} satisfies { [key in ClassKey]?: string };
