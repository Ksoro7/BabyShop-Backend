package OCI.BabyShop.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class OrderRequest {
    private Map<UUID, Integer> productQuantities;
    private String discountCode;
    private String paymentMethod;

    /** Nom complet saisi au checkout. */
    private String customerName;

    /** Téléphone saisi au checkout. */
    private String customerPhone;

    /** Adresse de livraison saisie au checkout. */
    private String deliveryAddress;
}
