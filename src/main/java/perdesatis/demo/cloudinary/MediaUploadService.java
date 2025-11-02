package perdesatis.demo.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private final Cloudinary cloudinary;

    private static final long MAX_IMAGE_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final long MAX_VIDEO_SIZE = 200 * 1024 * 1024; // 200 MB

    /**
     * Medya dosyasını otomatik olarak algılayıp yükler (resim veya video)
     */
    public String uploadAndOptimizeMedia(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new RuntimeException("Dosya formatı belirlenemedi");
        }

        contentType = contentType.toLowerCase();

        if (contentType.startsWith("image/")) {
            return uploadAndOptimizeImage(file);
        } else if (contentType.startsWith("video/")) {
            return uploadAndOptimizeVideo(file);
        } else {
            String filename = file.getOriginalFilename();
            if (filename != null) {
                filename = filename.toLowerCase();

                // Video uzantıları
                if (filename.endsWith(".mp4") || filename.endsWith(".mov") ||
                        filename.endsWith(".avi") || filename.endsWith(".mkv") ||
                        filename.endsWith(".webm") || filename.endsWith(".3gp")) {
                    return uploadAndOptimizeVideo(file);
                }
                // Resim uzantıları
                else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                        filename.endsWith(".png") || filename.endsWith(".gif") ||
                        filename.endsWith(".heic") || filename.endsWith(".heif") ||
                        filename.endsWith(".webp") || filename.endsWith(".bmp")) {
                    return uploadAndOptimizeImage(file);
                }
            }
            throw new RuntimeException("Sadece resim ve video dosyaları yüklenebilir");
        }
    }

    /**
     * Perde ürün görsellerini kalite kaybı olmadan yükler
     * Yüksek çözünürlük ve detay korunur
     */
    public String uploadAndOptimizeImage(MultipartFile photo) throws IOException {
        if (photo.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Resim boyutu çok büyük! Maksimum: 100 MB");
        }

        Map uploadResult = cloudinary.uploader().upload(photo.getBytes(), ObjectUtils.asMap(
                "folder", "perde_urunleri",
                "quality", "auto:best",           // En iyi kalite
                "format", "webp",                  // Modern format (PNG yerine)
                "fetch_format", "auto",            // Tarayıcıya göre otomatik format
                "transformation", new Transformation()
                        .width(2000)               // Yüksek çözünürlük (zoom için)
                        .height(2000)
                        .crop("limit")             // Orijinal oran korunur
                        .quality("auto:best")      // Kalite optimizasyonu
                        .fetchFormat("auto")       // Otomatik format seçimi
        ));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Perde tanıtım videolarını yükler
     * Full HD kalite korunur
     */
    public String uploadAndOptimizeVideo(MultipartFile video) throws IOException {
        if (video.getSize() > MAX_VIDEO_SIZE) {
            throw new RuntimeException("Video boyutu çok büyük! Maksimum: 200 MB");
        }

        Map uploadResult = cloudinary.uploader().upload(video.getBytes(), ObjectUtils.asMap(
                "folder", "perde_videolari",
                "resource_type", "video",
                "format", "mp4",                   // Evrensel format
                "quality", "auto:best",            // En iyi kalite
                "transformation", new Transformation()
                        .width(1920)               // Full HD
                        .height(1080)
                        .crop("limit")
                        .quality("auto:best")
                        .videoCodec("h264")        // Uyumlu codec
        ));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Thumbnail (küçük önizleme) oluşturur
     * Ürün listelerinde kullanmak için
     */
    public String uploadThumbnail(MultipartFile photo) throws IOException {
        if (photo.getSize() > MAX_IMAGE_SIZE) {
            throw new RuntimeException("Resim boyutu çok büyük! Maksimum: 100 MB");
        }

        Map uploadResult = cloudinary.uploader().upload(photo.getBytes(), ObjectUtils.asMap(
                "folder", "perde_thumbnails",
                "quality", "auto:good",
                "format", "webp",
                "transformation", new Transformation()
                        .width(400)
                        .height(400)
                        .crop("fill")
                        .gravity("auto")
        ));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Video thumbnail oluşturur
     */
    public String generateVideoThumbnail(String videoPublicId) throws IOException {
        // Video'nun belirli bir frame'ini thumbnail olarak al
        String thumbnailUrl = cloudinary.url()
                .transformation(new Transformation()
                        .width(800)
                        .height(450)
                        .crop("fill")
                        .startOffset("1.0"))  // 1. saniyeden thumbnail al
                .format("jpg")
                .generate(videoPublicId);

        return thumbnailUrl;
    }
}