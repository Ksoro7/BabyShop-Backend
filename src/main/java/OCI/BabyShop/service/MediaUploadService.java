package OCI.BabyShop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class MediaUploadService {

    private final Optional<Cloudinary> cloudinary;
    private final String baseUrl;

    @Autowired
    public MediaUploadService(Optional<Cloudinary> cloudinary,
                              @Value("${app.base-url}") String baseUrl) {
        this.cloudinary = cloudinary;
        this.baseUrl = baseUrl;
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        if (cloudinary.isPresent()) {
            return uploadToCloudinary(file, folder);
        }
        return uploadToLocal(file, folder);
    }

    private String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.get().uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", folder));
        String url = (String) result.get("secure_url");
        log.info("Image upload sur Cloudinary : {}", url);
        return url;
    }

    private String uploadToLocal(MultipartFile file, String folder) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Path.of(folder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        String fileUrl = baseUrl + "/" + folder + fileName;
        log.info("Image sauvegardee localement : {}", fileUrl);
        return fileUrl;
    }
}
