package OCI.BabyShop.controller;

import OCI.BabyShop.dto.MediaResponse;
import OCI.BabyShop.service.AdminMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private final AdminMediaService adminMediaService;

    @GetMapping
    public ResponseEntity<List<MediaResponse>> getAllMedia() {
        return ResponseEntity.ok(adminMediaService.getAllMedia());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMedia(@PathVariable UUID id) {
        adminMediaService.deleteMedia(id);
        return ResponseEntity.ok(Map.of("message", "Média supprimé avec succès"));
    }
}
