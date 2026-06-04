package OCI.BabyShop.service;

import OCI.BabyShop.domain.ProductMedia;
import OCI.BabyShop.dto.MediaResponse;
import OCI.BabyShop.repository.ProductMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .type(media.getType().name())
                .productName(media.getProduct() != null ? media.getProduct().getName() : "—")
                .productId(media.getProduct() != null ? media.getProduct().getId() : null)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
