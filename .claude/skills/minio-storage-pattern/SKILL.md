---
name: minio-storage-pattern
description: Use when working on MinIO image upload, image storage, file upload endpoints, or any feature that stores user-generated content (product images, profile photos, tenant logos). Covers the backend-proxy upload pattern, bucket organization, validation, and known issues with the current ImageService implementation.
---

# MinIO Image Storage Pattern

Bu skill platformun MinIO üzerinden file storage yapan parçaları için referans.

**Mevcut durum:**
- `user-tenant-service`'te çalışan bir `ImageService` var (profil ve mağaza fotosu)
- `product-service`'e Stage 6'da aynı pattern eklendi
- Ortak bir yapıya henüz çıkarılmadı (tekrar — `common-lib`'e taşınacak, TODO'da var)

## Kullanım pattern'i: Backend Proxy

Frontend dosyayı doğrudan MinIO'ya yüklemez. Backend'deki upload endpoint'ine `multipart/form-data` ile gönderir, backend MinIO'ya yazar, public URL döner.

**Sebep:** MinIO credentials browser'a sızmaz, content-type/boyut/auth validation backend'de yapılır.

```
[Frontend] --multipart--> [Backend ImageService] --MinioClient.putObject--> [MinIO bucket]
                                                                                    |
                                            <--public URL--                         |
[Frontend formu update]                                                             |
[update DTO'ya url ekle] --PUT--> [Backend product/user/tenant update]              |
```

## Bucket organizasyonu

Tek bucket: **`e-commerce-images`** (env: `USER_MINIO_BUCKET`)

Alt klasörler (folder name parametresiyle):

| Folder | Kullanım | Servis |
|---|---|---|
| `profiles/` | Kullanıcı profil fotoları | user-tenant-service |
| `tenants/` | Mağaza logosu / kapak görseli | user-tenant-service |
| `products/` | Ürün ana görseli + galeri | product-service |
| `categories/` | Kategori görselleri (ileride) | product-service |
| `reviews/` | Yorum fotoları (ileride) | product-service |

Object key: `<folder>/<UUID>_<originalFileName>` — UUID çakışmayı engeller, original isim debug kolaylığı.

## Mevcut implementasyon — UTS ImageService

```java
@Service
public class ImageService {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public String uploadImage(MultipartFile file, String folderName) {
        try {
            String fileName = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            return minioUrl + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Resim yüklenirken hata oluştu: " + e.getMessage());
        }
    }
}
```

## Bilinen iyileştirme alanları (Mevcut kod düzgün çalışıyor ama bunlar açık)

Bunlar acil değil, ama ImageService common-lib'e taşındığında **mutlaka** düzeltilmeli. Bu skill yeni servis için ImageService eklerken kullanılırsa iyileştirilmiş hali tercih edilmeli.

### 1. `RuntimeException` yerine domain exception

```java
// Yanlış
throw new RuntimeException("Resim yüklenirken hata oluştu: " + e.getMessage());

// Doğru
throw new SystemException("Resim yüklenemedi", "IMAGE_UPLOAD_FAILED");
```

`GlobalExceptionHandler` `SystemException`'ı 500 + errorCode ile döner, frontend `errorCode`'a göre kullanıcı mesajı gösterir.

### 2. Validation eksik

Şu an her şeyi yüklüyor. Olması gerekenler:

```java
private static final Set<String> ALLOWED_TYPES =
    Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;  // 5 MB

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
```

### 3. InputStream kapanmıyor — try-with-resources

```java
try (InputStream is = file.getInputStream()) {
    minioClient.putObject(
        PutObjectArgs.builder()
            .stream(is, file.getSize(), -1)
            // ...
    );
}
```

MinIO client genelde stream'i tüketir ama defansif yaklaşım iyi.

### 4. URL build'i `+` ile değil

```java
// Riskli (özel karakter, port, https vs http karışıklığı)
return minioUrl + "/" + bucketName + "/" + fileName;

// Doğru
return UriComponentsBuilder.fromUriString(minioUrl)
        .pathSegment(bucketName)
        .pathSegment(fileName)
        .toUriString();
```

`pathSegment` URL encoding yapar. Original filename'de boşluk veya Türkçe karakter varsa kırılmaz.

### 5. Bucket var mı kontrolü

Servis ilk kez ayağa kalktığında bucket olmayabilir. `@PostConstruct` ile garanti et:

```java
@PostConstruct
public void ensureBucket() {
    try {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("MinIO bucket oluşturuldu: {}", bucketName);
        }
    } catch (Exception e) {
        log.error("MinIO bucket kontrolü/oluşturma hatası", e);
    }
}
```

### 6. Original filename sanitization

Kullanıcı `../etc/passwd` gibi path traversal denerse object key bozulur:

```java
String safeName = file.getOriginalFilename() == null
    ? "image"
    : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
String fileName = folderName + "/" + UUID.randomUUID() + "_" + safeName;
```

## MinioConfig (mevcut, kabul edilebilir)

```java
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

## Controller pattern — yeni servise eklerken

```java
@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
@Slf4j
public class ProductImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<ImageUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long tenantId,
            @CurrentUser AuthUser user) {

        String url = imageService.uploadImage(file, "products");
        log.info("Ürün görseli yüklendi. Tenant: {}, User: {}, URL: {}",
                tenantId, user.keycloakId(), url);

        return ResponseEntity.ok(new ImageUploadResponse(url));
    }
}

public record ImageUploadResponse(String url) {}
```

`uploadImage`'in dönen URL'i frontend tarafından product create/update DTO'sundaki `mainImageUrl` veya `imageUrls` alanına eklenir.

## application.yml MinIO bloğu

Tüm servisler aynı bucket'a yazıyor → aynı config:

```yaml
# application-dev.yml
minio:
  url: http://localhost:9005
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket-name: ${USER_MINIO_BUCKET}

# application-prod.yml — sadece url farkı
minio:
  url: http://minio:9000
  # ...
```

## pom.xml dependency

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>  <!-- veya Spring Boot 3.5.x ile uyumlu güncel -->
</dependency>
```

## TODO — common-lib'e taşıma planı

Şu an her servis kendi `ImageService` ve `MinioConfig`'ine sahip. Üçüncü servis eklenirken (basket veya order) common-lib'e taşıma zamanı:

```
common-lib/
  └── storage/
      ├── ImageStorageClient.java   (interface)
      ├── MinioImageStorageClient.java (implementation)
      ├── MinioStorageProperties.java (@ConfigurationProperties)
      └── ImageStorageAutoConfig.java (@AutoConfiguration)
```

Servisler `@EnableImageStorage` veya direkt `@Autowired ImageStorageClient` ile kullanır. Folder name parametresi servis-spesifik kalır (`"products"`, `"profiles"` vb.).

## Production geçişte

- MinIO nginx reverse proxy arkasına alınmalı
- Public URL backend'den signed URL olarak dönmeli (TTL'li, access-controlled)
- Şu an dev'de MinIO doğrudan port'tan erişiliyor — prod'da kapalı

## İlişkili dosyalar

- Mevcut UTS ImageService: `backend/user-tenant-service/.../user/service/ImageService.java`
- Mevcut product-service ImageService: `backend/product-service/.../common/service/ImageService.java` (Stage 6'da eklendi)
- MinioConfig: `backend/user-tenant-service/.../common/config/MinioConfig.java`
- Skill: `spring-service-conventions` (exception kuralı, controller pattern)
- TODO: `common-lib`'e taşıma (üçüncü servis eklenince)
