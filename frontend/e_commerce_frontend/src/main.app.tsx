import App from './App';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { BrowserRouter } from 'react-router-dom';
import customTheme from './utils/customTheme';
import { GlobalStyles } from "@mui/material";
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, type AuthProviderProps } from "react-oidc-context";

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 60_000,
            gcTime: 1000 * 60 * 10, // 10 dk — cache'de kalma süresi (navigation'da yeniden fetch'i azaltır)
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});

const GlobalScrollbarFix = (
    <GlobalStyles
        styles={{
            body: {
                overflowY: 'scroll',
            },
        }}
    />
);

const oidcConfig: AuthProviderProps = {
    authority: `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`,
    client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
    redirect_uri: window.location.origin,
    automaticSilentRenew: true,
    loadUserInfo: true,
    accessTokenExpiringNotificationTimeInSeconds: 60,

    onSigninCallback: () => {
        window.history.replaceState({}, document.title, window.location.pathname);
    }
};

export default function AppEntrypoint() {
    return (
        <ThemeProvider theme={customTheme}>
            <AuthProvider {...oidcConfig}>
                <BrowserRouter>
                    <QueryClientProvider client={queryClient}>
                        <CssBaseline />
                        {GlobalScrollbarFix}
                        <App />
                    </QueryClientProvider>
                </BrowserRouter>
            </AuthProvider>
        </ThemeProvider>
    );
}
