package OCI.BabyShop.config;

import OCI.BabyShop.domain.Category;
import OCI.BabyShop.domain.Discount;
import OCI.BabyShop.repository.CategoryRepository;
import OCI.BabyShop.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    @Transactional
    public void run(String... args) {
        seedDiscounts();
        seedCategories();
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
            categoryRepository.findByName("Chaussures").ifPresent(cat -> {
                if (!"assets/images/medias.png".equals(cat.getImageUrl())) {
                    cat.setImageUrl("assets/images/medias.png");
                    categoryRepository.save(cat);
                    log.info("Category image updated: {} -> {}", cat.getName(), cat.getImageUrl());
                }
            });
            return;
        }

        Category ordinateurs = categoryRepository.save(
                Category.builder().name("Ordinateurs").build());
        log.info("Seed category: {}", ordinateurs.getName());

        Category chaussures = categoryRepository.save(
                Category.builder().name("Chaussures").imageUrl("assets/images/medias.png").build());
        log.info("Seed category: {}", chaussures.getName());

        Category vetements = categoryRepository.save(
                Category.builder().name("Vêtements").build());
        log.info("Seed category: {}", vetements.getName());

        List.of(
                Category.builder().name("Enfants / Bébé").parent(vetements).build(),
                Category.builder().name("Femmes").parent(vetements).build(),
                Category.builder().name("Hommes").parent(vetements).build()
        ).forEach(child -> {
            categoryRepository.save(child);
            log.info("Seed category: {} (parent: Vêtements)", child.getName());
        });
    }
}
