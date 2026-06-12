import { createRoot } from "react-dom/client";
import { StrictMode, Suspense, lazy } from "react";
import { KcPage } from "./kc.gen";

// Keycloak içinde çalışırken (login/register/account ekranları) Keycloakify teması,
// normal kullanımda ise uygulamanın kendisi render edilir.
// Uygulama, tema bundle'ına girmesin diye lazy import ediliyor.
const AppEntrypoint = lazy(() => import("./main.app"));

createRoot(document.getElementById("root")!).render(
    <StrictMode>
        {window.kcContext ? (
            <KcPage kcContext={window.kcContext} />
        ) : (
            <Suspense fallback={null}>
                <AppEntrypoint />
            </Suspense>
        )}
    </StrictMode>
);
