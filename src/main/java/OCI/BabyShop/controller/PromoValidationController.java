package OCI.BabyShop.controller;

import OCI.BabyShop.dto.PromoValidationRequest;
import OCI.BabyShop.dto.PromoValidationResponse;
import OCI.BabyShop.service.PromoValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promo")
@RequiredArgsConstructor
public class PromoValidationController {

    private final PromoValidationService promoValidationService;

    @PostMapping("/valider")
    public ResponseEntity<PromoValidationResponse> validerCode(@Valid @RequestBody PromoValidationRequest request) {
        PromoValidationResponse response = promoValidationService.valider(request);
        return ResponseEntity.ok(response);
    }
}
