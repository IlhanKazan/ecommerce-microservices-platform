package com.ecommerce.productservice.common.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.SystemException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
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

    // Tarayıcıya gömülecek public endpoint — internal minio.url'den ayrıdır.
    // Container içinde minio.url=http://minio:9000 olur ama tarayıcı bunu çözemez.
    @Value("${minio.public-url}")
    private String publicUrl;

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

        return UriComponentsBuilder.fromUriString(publicUrl)
                .pathSegment(bucketName)
                .pathSegment(folderName)
                .pathSegment(uuid + "_" + safeName)
                .toUriString();
    }

    /**
     * Tek bir görseli MinIO'dan siler. Best-effort: hata fırlatmaz, sadece loglar —
     * silme başarısızlığı asıl iş akışını (ürün update/delete vb.) çökertmemeli.
     * URL host-agnostiktir; eski {@code http://minio:9000/...} kayıtlarını da parse eder.
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String objectKey = extractObjectKey(imageUrl);
        if (objectKey == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            log.info("MinIO görsel silindi: {}", objectKey);
        } catch (Exception e) {
            log.warn("MinIO görsel silinemedi (best-effort): {}", imageUrl, e);
        }
    }

    public void deleteImages(Collection<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        imageUrls.forEach(this::deleteImage);
    }

    // URL'in path'inden bucket prefix'ini soyarak object key'i çıkarır:
    // http://host/<bucket>/<folder>/<uuid>_<name> -> <folder>/<uuid>_<name>
    private String extractObjectKey(String imageUrl) {
        try {
            String path = URI.create(imageUrl).getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String prefix = bucketName + "/";
            if (path.startsWith(prefix)) {
                path = path.substring(prefix.length());
            }
            return path.isBlank() ? null : path;
        } catch (Exception e) {
            log.warn("Görsel URL parse edilemedi: {}", imageUrl, e);
            return null;
        }
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
