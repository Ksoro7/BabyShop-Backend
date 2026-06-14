package OCI.BabyShop.service;

import OCI.BabyShop.domain.ProductMedia;
import OCI.BabyShop.dto.MediaResponse;
import OCI.BabyShop.repository.ProductMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMediaService {

    private final ProductMediaRepository productMediaRepository;

    @Transactional(readOnly = true)
    public List<MediaResponse> getAllMedia() {
        return productMediaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMedia(UUID id) {
        productMediaRepository.deleteById(id);
    }

    private MediaResponse toResponse(ProductMedia media) {
        String productName = "—";
        UUID productId = null;
        try {
            if (media.getProduct() != null) {
                productName = media.getProduct().getName();
                productId = media.getProduct().getId();
            }
        } catch (EntityNotFoundException e) {
            log.warn("Produit introuvable (supprimé) pour le média {}", media.getId());
        }
        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .type(media.getType().name())
                .productName(productName)
                .productId(productId)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
