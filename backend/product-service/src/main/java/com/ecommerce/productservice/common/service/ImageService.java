package com.ecommerce.productservice.common.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.SystemException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public ImageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket oluşturuldu: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("MinIO bucket kontrolü/oluşturma hatası", e);
        }
    }

    public String uploadImage(MultipartFile file, String folderName) {
        validate(file);

        String safeName = file.getOriginalFilename() == null
                ? "image"
                : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        String uuid = UUID.randomUUID().toString();
        String objectKey = folderName + "/" + uuid + "_" + safeName;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new SystemException("Resim yüklenemedi", "IMAGE_UPLOAD_FAILED");
        }

        return UriComponentsBuilder.fromUriString(minioUrl)
                .pathSegment(bucketName)
                .pathSegment(folderName)
                .pathSegment(uuid + "_" + safeName)
                .toUriString();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Dosya boş olamaz", "EMPTY_FILE");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("Dosya 5MB'dan büyük olamaz", "FILE_TOO_LARGE");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Sadece JPEG, PNG, WebP yüklenebilir", "INVALID_FILE_TYPE");
        }
    }
}
