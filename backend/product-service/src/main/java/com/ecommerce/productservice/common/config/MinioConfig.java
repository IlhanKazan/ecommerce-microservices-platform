package com.ecommerce.productservice.common.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    // Explicit region: olmadan SDK her işlemden önce GetBucketLocation çağırır;
    // scoped service account'ta o izin yoksa upload AccessDenied (500) verir.
    @Value("${minio.region:us-east-1}")
    private String region;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .region(region)
                .credentials(accessKey, secretKey)
                .build();
    }
}