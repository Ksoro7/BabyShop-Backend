package OCI.BabyShop.config;

import OCI.BabyShop.domain.Discount;
import OCI.BabyShop.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DiscountRepository discountRepository;

    @Override
    public void run(String... args) {
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
}
