package OCI.BabyShop.controller;

import OCI.BabyShop.dto.PaymentInitRequest;
import OCI.BabyShop.dto.PaymentInitResponse;
import OCI.BabyShop.service.CinetPayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CinetPayService cinetPayService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitResponse> initiatePayment(
            @Valid @RequestBody PaymentInitRequest request,
            @RequestParam(defaultValue = "") String returnUrl) {
        PaymentInitResponse response = cinetPayService.initiatePayment(request.getOrderId(), returnUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        cinetPayService.handleWebhook(payload);
        return ResponseEntity.ok("OK");
    }
}
