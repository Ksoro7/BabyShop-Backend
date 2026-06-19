package OCI.BabyShop;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.util.Map;

public class CloudinaryOnboarding {

    public static void main(String[] args) throws Exception {
        // 1. Configuration Cloudinary (inline)
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "debbksf7i",
                "api_key", "221574955953855",
                "api_secret", "usNNd5SzhfXwOh-KcVmOTUEwIUk"
        ));

        System.out.println("=== Upload d'une image depuis le domaine demo de Cloudinary ===\n");

        // 2. Upload d'une image demo
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                "https://res.cloudinary.com/demo/image/upload/sample.jpg",
                ObjectUtils.emptyMap()
        );

        String secureUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");
        System.out.println("URL securisee : " + secureUrl);
        System.out.println("Public ID     : " + publicId);

        // 3. Recuperation des metadonnees
        Map<?, ?> imageInfo = (Map<?, ?>) uploadResult.get("image_metadata");
        if (imageInfo == null) {
            // Fallback: on prend les infos depuis la reponse directe
            imageInfo = uploadResult;
        }
        System.out.println("\n=== Metadonnees de l'image ===");
        System.out.println("Largeur  : " + uploadResult.get("width") + " px");
        System.out.println("Hauteur  : " + uploadResult.get("height") + " px");
        System.out.println("Format   : " + uploadResult.get("format"));
        System.out.println("Taille   : " + uploadResult.get("bytes") + " octets");

        // 4. Transformation: f_auto (choisit le meilleur format automatiquement, ex. WebP si supporte)
        //    q_auto (ajuste la qualite automatiquement sans perte visible)
        String publicIdStr = (String) uploadResult.get("public_id");
        String transformedUrl = cloudinary.url().format("auto")
                .transformation(new com.cloudinary.Transformation().quality("auto"))
                .generate(publicIdStr);

        System.out.println("\n=== Image optimisee ===");
        System.out.println("Done! Cliquez sur le lien ci-dessous pour voir la version optimisee.");
        System.out.println("(format automatique + qualite automatique)");
        System.out.println(transformedUrl);
    }
}
