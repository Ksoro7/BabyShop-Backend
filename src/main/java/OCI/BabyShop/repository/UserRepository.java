package OCI.BabyShop.repository;

import OCI.BabyShop.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    // @change [PROD-READY] Recherche par token de reset - 2026-06-12
    Optional<User> findByResetPasswordToken(String resetPasswordToken);
}
