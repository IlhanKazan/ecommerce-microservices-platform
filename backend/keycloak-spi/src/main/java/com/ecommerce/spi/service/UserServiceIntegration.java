package com.ecommerce.spi.service;

import com.ecommerce.spi.config.SpiConfig;
import com.ecommerce.spi.util.HttpUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserServiceIntegration {

    private static final UserServiceIntegration INSTANCE = new UserServiceIntegration();
    public static UserServiceIntegration getInstance() { return INSTANCE; }
    private UserServiceIntegration() {}

    // User Service'ten kullanıcıyı siler.
    public boolean deleteUser(String userId) {
        String token = getServiceAccountToken();
        String url = SpiConfig.USER_SERVICE_BASE_URL + "/" + userId;
        try {
            int status = HttpUtil.delete(url, token);
            return status >= 200 && status < 300 || status == 404;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // User Service'e kullanıcı verilerini senkronize eder (Create/Update)
    public void syncUser(String userId, String email, String username, String firstName, String lastName, boolean isUpdate) {
        String token = getServiceAccountToken();
        if (token == null) throw new RuntimeException("Token alınamadı");

        String jsonPayload = String.format(
                "{\"keycloakId\":\"%s\", \"email\":\"%s\", \"username\":\"%s\", \"firstName\":\"%s\", \"lastName\":\"%s\"}",
                userId, email != null ? email : "", username != null ? username : "",
                firstName != null ? firstName : "", lastName != null ? lastName : ""
        );

        try {
            int status;
            if (isUpdate) {
                status = HttpUtil.put(SpiConfig.USER_SERVICE_BASE_URL + "/" + userId, jsonPayload, token);
            } else {
                status = HttpUtil.post(SpiConfig.USER_SERVICE_BASE_URL + "/", jsonPayload, token);
            }

            if (status < 200 || status >= 300) {
                throw new RuntimeException("Service Error Status: " + status);
            }
        } catch (IOException e) {
            throw new RuntimeException("Sync Failed", e);
        }
    }

    // User Service Ayakta mı ?
    public boolean isHealthy() {
        try {
            int status = HttpUtil.get(SpiConfig.USER_SERVICE_HEALTH_URL);
            return status < 500;
        } catch (IOException e) {
            return false;
        }
    }

    // Keycloak'tan Service Account Token alır.
    private String getServiceAccountToken() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(SpiConfig.KEYCLOAK_TOKEN_URL);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "client_credentials"));
            params.add(new BasicNameValuePair("client_id", SpiConfig.CLIENT_ID));
            params.add(new BasicNameValuePair("client_secret", SpiConfig.CLIENT_SECRET));
            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    Pattern pattern = Pattern.compile("\"access_token\":\"(.*?)\"");
                    Matcher matcher = pattern.matcher(responseBody);
                    if (matcher.find()) return matcher.group(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}