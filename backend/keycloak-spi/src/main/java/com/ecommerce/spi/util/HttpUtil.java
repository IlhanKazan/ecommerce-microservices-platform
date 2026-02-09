package com.ecommerce.spi.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class HttpUtil {

    private static final int TIMEOUT_MS = 3000;

    private HttpUtil() {}

    private static CloseableHttpClient createClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MS)
                .setConnectionRequestTimeout(TIMEOUT_MS)
                .setSocketTimeout(TIMEOUT_MS)
                .build();
        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }

    public static int post(String url, String jsonPayload, String token) throws IOException {
        try (CloseableHttpClient client = createClient()) {
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            if (token != null) request.setHeader("Authorization", "Bearer " + token);
            if (jsonPayload != null) request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    public static int put(String url, String jsonPayload, String token) throws IOException {
        try (CloseableHttpClient client = createClient()) {
            HttpPut request = new HttpPut(url);
            request.setHeader("Content-Type", "application/json");
            if (token != null) request.setHeader("Authorization", "Bearer " + token);
            if (jsonPayload != null) request.setEntity(new StringEntity(jsonPayload, "UTF-8"));

            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    public static int delete(String url, String token) throws IOException {
        try (CloseableHttpClient client = createClient()) {
            HttpDelete request = new HttpDelete(url);
            if (token != null) request.setHeader("Authorization", "Bearer " + token);

            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            }
        }
    }

    public static int get(String url) throws IOException {
        System.out.println(">> DEBUG HTTP GET: Gelen URL -> " + url);
        try (CloseableHttpClient client = createClient()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request)) {
                System.out.println(">> DEBUG STATUS CODE: " + response.getStatusLine().getStatusCode());
                return response.getStatusLine().getStatusCode();
            }
        }
    }
}