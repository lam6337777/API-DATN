package shoe.com.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import shoe.com.api.exception.BadRequestException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lưu ảnh upload.
 * - Có CLOUDINARY_URL (cloudinary://api_key:api_secret@cloud_name) → upload lên Cloudinary, trả URL https đầy đủ.
 *   Dùng cho server (ổ đĩa của Render free bị xóa mỗi lần khởi động lại).
 * - Không có → lưu vào thư mục app.upload.dir và trả đường dẫn /uploads/... như trước (chạy local).
 */
@Service
public class FileUploadService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    @Value("${app.cloudinary.url:}")
    private String cloudinaryUrl;

    @Value("${app.cloudinary.folder:pf-shoes}")
    private String cloudinaryFolder;

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Pattern CLOUDINARY_URL = Pattern.compile("^cloudinary://([^:]+):([^@]+)@(.+)$");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Upload 1 file ảnh, trả về URL
     */
    public String uploadFile(MultipartFile file) {
        validateFile(file);
        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        try {
            if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
                return uploadToCloudinary(file.getBytes(), UUID.randomUUID() + extension, file.getContentType());
            }
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            String fileName = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return urlPrefix + "/" + fileName;
        } catch (IOException e) {
            throw new BadRequestException("Không thể upload file: " + e.getMessage());
        }
    }

    /**
     * Upload nhiều file ảnh
     */
    public List<String> uploadMultipleFiles(MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file));
        }
        return urls;
    }

    /** Signed upload qua REST API của Cloudinary (không cần SDK). */
    private String uploadToCloudinary(byte[] bytes, String fileName, String contentType) throws IOException {
        Matcher m = CLOUDINARY_URL.matcher(cloudinaryUrl.trim());
        if (!m.matches()) {
            throw new BadRequestException("CLOUDINARY_URL không hợp lệ (dạng cloudinary://api_key:api_secret@cloud_name)");
        }
        String apiKey = m.group(1), apiSecret = m.group(2), cloud = m.group(3);
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        // Chữ ký: sha1 của các tham số (sắp xếp theo tên) nối với api_secret
        String signature = sha1("folder=" + cloudinaryFolder + "&timestamp=" + timestamp + apiSecret);

        String boundary = "----pf" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "api_key", apiKey);
        writeField(body, boundary, "timestamp", timestamp);
        writeField(body, boundary, "folder", cloudinaryFolder);
        writeField(body, boundary, "signature", signature);
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: " + (contentType != null ? contentType : "application/octet-stream") + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(res.body());
            if (res.statusCode() >= 300 || !json.hasNonNull("secure_url")) {
                String msg = json.path("error").path("message").asText("HTTP " + res.statusCode());
                throw new BadRequestException("Cloudinary từ chối ảnh: " + msg);
            }
            return json.get("secure_url").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Upload ảnh bị gián đoạn");
        }
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File rỗng");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Chỉ hỗ trợ file ảnh: JPEG, PNG, WebP, GIF");
        }
    }
}
