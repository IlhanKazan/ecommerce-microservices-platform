package com.ecommerce.spi.provider;

import com.ecommerce.spi.service.UserServiceIntegration;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.FormContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class UserServiceValidationAction implements FormAction, FormActionFactory {

    public static final String PROVIDER_ID = "user-service-validation";

    public UserServiceValidationAction() {}

    @Override
    public void validate(ValidationContext context) {
        System.out.println(">> VALIDATION: User Service kontrol ediliyor...");

        if (!UserServiceIntegration.getInstance().isHealthy()) {

            System.err.println(">> VALIDATION HATA: User Service erişilemez durumda! İşlem engellendi.");

            List<FormMessage> errors = new ArrayList<>();
            errors.add(new FormMessage(null, "Kullanıcı servisine erişilemiyor, lütfen daha sonra tekrar deneyiniz."));

            context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
            return;
        }

        context.success();
    }

    @Override
    public void success(FormContext context) {
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
    }

    @Override
    public boolean requiresUser() { return false; }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public FormAction create(KeycloakSession session) { return this; }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() { return PROVIDER_ID; }

    @Override
    public String getDisplayType() { return "User Service Availability Check"; }

    @Override
    public String getReferenceCategory() { return null; }

    @Override
    public boolean isConfigurable() { return false; }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() { return false; }

    @Override
    public String getHelpText() { return "User Service erişilebilirliğini kontrol eder."; }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() { return null; }
}