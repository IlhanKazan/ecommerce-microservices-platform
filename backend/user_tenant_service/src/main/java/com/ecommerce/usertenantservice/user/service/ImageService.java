package com.ecommerce.usertenantservice.user.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.UUID;

@Service
public class ImageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public ImageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadImage(MultipartFile file, String folderName ) {
        try {
            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            String fileName = folderName + "/" + UUID.randomUUID().toString() + "_" + originalFileName;

            InputStream inputStream = file.getInputStream();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            return minioUrl + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Resim yüklenirken hata oluştu: " + e.getMessage());
        }
    }
}