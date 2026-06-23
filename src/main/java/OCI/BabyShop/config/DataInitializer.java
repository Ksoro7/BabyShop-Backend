package OCI.BabyShop.config;

import OCI.BabyShop.domain.*;
import OCI.BabyShop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DiscountRepository discountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@rabishop.com}")
    private String adminEmail;

    @Value("${admin.password:Admin123!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedDiscounts();
        seedCategories();
        // @change [PROD-READY] Migration one-time désactivée (version NULL déjà corrigée) - 2026-06-12
        // fixProductVersion();
    }

    private void seedAdmin() {
        if ("true".equals(System.getenv("RENDER")) && "Admin123!".equals(adminPassword)) {
            throw new IllegalStateException("🛑 SÉCURITÉ CRITIQUE : Mot de passe admin par défaut utilisé en production (Render). Définissez la variable d'environnement 'admin.password' !");
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin déjà présent, skip seed");
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName("Admin")
                .lastName("RaBiShop")
                .phone("")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        userRepository.save(admin);
        log.info("Admin créé : {}", adminEmail);
    }

    private void seedDiscounts() {
        if (discountRepository.count() > 0) {
            log.info("Discounts déjà présents, skip seed");
            return;
        }

        Discount[] seeds = {
                Discount.builder()
                        .code("BIENVENUE10")
                        .type(Discount.DiscountType.percentage)
                        .valeur(BigDecimal.TEN)
                        .montantMinAchat(BigDecimal.ZERO)
                        .usageUniqueParUser(true)
                        .actif(true)
                        .build(),
                Discount.builder()
                        .code("RENTREE2025")
                        .type(Discount.DiscountType.percentage)
                        .valeur(BigDecimal.valueOf(15))
                        .montantMinAchat(BigDecimal.valueOf(20000))
                        .dateExpiration(LocalDate.of(2025, 10, 31))
                        .nbMaxUtilisations(200)
                        .usageUniqueParUser(false)
                        .actif(true)
                        .build(),
                Discount.builder()
                        .code("PLUS30K")
                        .type(Discount.DiscountType.fixed)
                        .valeur(BigDecimal.valueOf(5000))
                        .montantMinAchat(BigDecimal.valueOf(30000))
                        .usageUniqueParUser(false)
                        .actif(true)
                        .build(),
                Discount.builder()
                        .code("FIDELITE20")
                        .type(Discount.DiscountType.percentage)
                        .valeur(BigDecimal.valueOf(20))
                        .montantMinAchat(BigDecimal.ZERO)
                        .usageUniqueParUser(true)
                        .actif(true)
                        .build()
        };

        for (Discount d : seeds) {
            discountRepository.save(d);
            log.info("Seed discount: {}", d.getCode());
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            categoryRepository.findAll().forEach(cat -> {
                if (cat.getImageUrl() == null) {
                    cat.setImageUrl("assets/images/medias.png");
                    categoryRepository.save(cat);
                    log.info("Category image set: {} -> {}", cat.getName(), cat.getImageUrl());
                }
            });
            return;
        }

        Category ordinateurs = categoryRepository.save(
                Category.builder().name("Ordinateurs").imageUrl("assets/images/medias.png").build());
        log.info("Seed category: {}", ordinateurs.getName());

        Category chaussures = categoryRepository.save(
                Category.builder().name("Chaussures").imageUrl("assets/images/medias.png").build());
        log.info("Seed category: {}", chaussures.getName());

        Category vetements = categoryRepository.save(
                Category.builder().name("Vêtements").imageUrl("assets/images/medias.png").build());
        log.info("Seed category: {}", vetements.getName());

        List.of(
                Category.builder().name("Enfants / B\u00e9b\u00e9").parent(vetements).imageUrl("assets/images/medias.png").build(),
                Category.builder().name("Femmes").parent(vetements).imageUrl("assets/images/medias.png").build(),
                Category.builder().name("Hommes").parent(vetements).imageUrl("assets/images/medias.png").build()
        ).forEach(child -> {
            categoryRepository.save(child);
            log.info("Seed category: {} (parent: V\u00eatements)", child.getName());
        });
    }

    private void fixProductVersion() {
        productRepository.fixNullVersion();
        log.info("Version NULL corrigee");
    }

}
