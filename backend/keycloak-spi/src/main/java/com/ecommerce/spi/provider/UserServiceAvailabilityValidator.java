package com.ecommerce.spi.provider;

import com.ecommerce.spi.service.UserServiceIntegration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.ValidatorFactory;

public class UserServiceAvailabilityValidator implements SimpleValidator, ValidatorFactory {

    public static final String ID = "user-service-availability";

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        UserModel currentUser = context.getSession().getContext().getUser();

        if (currentUser != null && "admin".equalsIgnoreCase(currentUser.getUsername())) {
            System.out.println(">> VALIDATOR: Admin kullanıcısı tespit edildi, kontroller atlanıyor.");
            return context;
        }

        String method = context.getSession().getContext().getHttpRequest().getHttpMethod();
        String requestUri = context.getSession().getContext().getHttpRequest().getUri().getPath();

        System.out.println(">> VALIDATOR TETİKLENDİ. URI: " + requestUri + " | Method: " + method);

        if (requestUri.contains("/registration")) {
            System.out.println(">> REGISTRATION TESPİT EDİLDİ. VALIDATOR ATLANDI.");
            return context;
        }

        boolean isAccountUpdate = requestUri.contains("/account");
        boolean isAdminUpdate = requestUri.contains("/admin/realms");
        boolean isLoginActionPost = requestUri.contains("/login-actions/") && "POST".equalsIgnoreCase(method);

        if (!isAccountUpdate && !isAdminUpdate && !isLoginActionPost) {
            System.out.println(">> SADECE OKUMA/LOGIN İŞLEMİ. SERVİS KONTROLÜ ATLANDI.");
            return context;
        }

        System.out.println(">> KRİTİK İŞLEM. USER SERVICE KONTROL EDİLİYOR...");

        if (!UserServiceIntegration.getInstance().isHealthy()) {
            System.err.println(">> UPDATE BLOCKED: User Service erişilemez!");

            context.addError(new org.keycloak.validate.ValidationError(ID, inputHint, "Kullanıcı servisine erişilemiyor, işlem gerçekleştirilemedi."));
        }

        return context;
    }

    @Override public String getId() { return ID; }
    @Override public SimpleValidator create(KeycloakSession session) { return this; }
    @Override public void init(org.keycloak.Config.Scope config) {}
    @Override public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {}
    @Override public void close() {}
}