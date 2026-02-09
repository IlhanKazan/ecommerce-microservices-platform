package com.ecommerce.spi.provider;

import com.ecommerce.spi.service.UserServiceIntegration;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class UserSyncEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;

    public UserSyncEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getError() != null) return;

        if (isAdmin(event.getUserId())) {
            System.out.println(">> EVENT LISTENER: Admin işlemi tespit edildi, sync yapılmıyor.");
            return;
        }

        if (EventType.REGISTER.equals(event.getType())) {
            System.out.println(">> EVENT: REGISTER yakalandı -> " + event.getUserId());
            try {
                UserData data = getUserData(event.getUserId(), event);

                UserServiceIntegration.getInstance().syncUser(
                        data.id, data.email, data.username, data.firstName, data.lastName, false
                );
            } catch (RuntimeException e) {
                System.err.println("HATA: User Service senkronizasyonu başarısız! Rollback yapılıyor...");
                deleteUserOnFailure(event.getUserId());
                throw e;
            }
        }

        else if (EventType.UPDATE_PROFILE.equals(event.getType())) {

            if (isUserInDeleteMode(event.getUserId())) {
                System.out.println(">> IGNORE: Kullanıcı silme modunda. Update gönderilmiyor.");
                return;
            }

            System.out.println(">> EVENT: UPDATE yakalandı " + event.getUserId());
            try {
                UserData data = getUserData(event.getUserId(), event);

                UserServiceIntegration.getInstance().syncUser(
                        data.id, data.email, data.username, data.firstName, data.lastName, true
                );
            } catch (RuntimeException e) {
                System.err.println("HATA: Update başarısız! Transaction Rollback yapılıyor.");
                session.getTransactionManager().setRollbackOnly();
                throw new jakarta.ws.rs.InternalServerErrorException("Update Cancelled due to Service Error");
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getError() != null) return;

        if (ResourceType.USER.equals(event.getResourceType()) && OperationType.DELETE.equals(event.getOperationType())) {
            String userId = event.getResourcePath().replace("users/", "");
            UserServiceIntegration.getInstance().deleteUser(userId);
        }
    }

    private boolean isAdmin(String userId) {
        try {
            RealmModel realm = session.getContext().getRealm();
            if (realm == null) realm = session.realms().getRealm("master");
            UserModel user = session.users().getUserById(realm, userId);
            return user != null && "admin".equalsIgnoreCase(user.getUsername());
        } catch (Exception e) { return false; }
    }

    private boolean isUserInDeleteMode(String userId) {
        try {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            return user != null && user.getRequiredActionsStream().anyMatch(action -> "delete_account".equals(action));
        } catch (Exception e) { return false; }
    }

    private void deleteUserOnFailure(String userId) {
        try {
            RealmModel realm = session.getContext().getRealm();
            if (realm == null) realm = session.realms().getRealm(session.getContext().getRealm().getId());
            UserModel user = session.users().getUserById(realm, userId);
            if (user != null) {
                session.users().removeUser(realm, user);
                System.out.println(">> ROLLBACK BAŞARILI: Kullanıcı silindi " + userId);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static class UserData { String id, email, username, firstName, lastName; }

    private UserData getUserData(String userId, Event event) {
        UserData data = new UserData();
        data.id = userId;
        try {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            if (user != null) {
                data.email = user.getEmail();
                data.username = user.getUsername();
                data.firstName = user.getFirstName();
                data.lastName = user.getLastName();
                return data;
            }
        } catch (Exception e) {}

        if (event.getDetails() != null) {
            data.email = event.getDetails().get("email");
            data.username = event.getDetails().get("username");
            data.firstName = event.getDetails().get("first_name");
            data.lastName = event.getDetails().get("last_name");
        }
        return data;
    }

    @Override public void close() {}
}