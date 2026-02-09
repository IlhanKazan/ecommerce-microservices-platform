package com.ecommerce.spi.provider;

import com.ecommerce.spi.service.UserServiceIntegration;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SmartDeleteAccountAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "delete_account";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        context.challenge(context.form()
                .setAttribute("triggered_from_aia", false)
                .createForm("delete-account-confirm.ftl"));
    }

    @Override
    public void processAction(RequiredActionContext context) {
        System.out.println(">> SMART DELETE: Silme işlemi başlatılıyor...");

        String userId = context.getUser().getId();

        if (!UserServiceIntegration.getInstance().deleteUser(userId)) {
            System.err.println(">> SMART DELETE: User Service silme başarısız. İşlem iptal ediliyor.");

            context.failure();
            context.challenge(context.form()
                    .setAttribute("triggered_from_aia", false)
                    .setError("Sistemsel bir sorun nedeniyle hesap silinemedi. Lütfen daha sonra deneyin.")
                    .createForm("delete-account-confirm.ftl"));
            return;
        }

        try {
            System.out.println(">> SMART DELETE: User Service onayı alındı. Keycloak kullanıcısı siliniyor.");
            context.getSession().users().removeUser(context.getRealm(), context.getUser());

            context.challenge(context.form()
                    .setSuccess("Hesabınız başarıyla silindi.")
                    .createForm("info.ftl"));

        } catch (Exception e) {
            context.failure();
        }
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() { return InitiatedActionSupport.SUPPORTED; }

    @Override public RequiredActionProvider create(KeycloakSession session) { return this; }
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public void close() {}
    @Override public String getId() { return PROVIDER_ID; }
    @Override public String getDisplayText() { return "Smart Delete Account (Sync Check)"; }
    @Override public boolean isOneTimeAction() { return true; }
}