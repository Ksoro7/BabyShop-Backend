package OCI.BabyShop.service;

import OCI.BabyShop.domain.Role;
import OCI.BabyShop.domain.User;
import OCI.BabyShop.domain.UserDiscount;
import OCI.BabyShop.dto.AuthResponse;
import OCI.BabyShop.dto.LoginRequest;
import OCI.BabyShop.dto.RegisterRequest;
import OCI.BabyShop.repository.RefreshTokenRepository;
import OCI.BabyShop.repository.UserDiscountRepository;
import OCI.BabyShop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDiscountRepository userDiscountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userDiscountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jean");
        request.setLastName("Dupont");
        request.setEmail("jean@test.com");
        request.setPassword("Password1!");

        AuthResponse response = authService.register(request);

        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("USER", response.getRole());
        assertEquals("jean@test.com", response.getEmail());
        assertNotNull(response.getWelcomeDiscountCode());

        Optional<User> saved = userRepository.findByEmail("jean@test.com");
        assertTrue(saved.isPresent());
        assertEquals("Jean", saved.get().getFirstName());
        assertEquals(Role.USER, saved.get().getRole());
    }

    @Test
    void register_shouldCreateWelcomeDiscount() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Alice");
        request.setLastName("Martin");
        request.setEmail("alice@test.com");
        request.setPassword("Password1!");

        AuthResponse response = authService.register(request);

        assertNotNull(response.getWelcomeDiscountCode());
        assertTrue(response.getWelcomeDiscountCode().startsWith("WELCOME10-"));

        User user = userRepository.findByEmail("alice@test.com").orElseThrow();
        Optional<UserDiscount> discount = userDiscountRepository.findByDiscountCode(response.getWelcomeDiscountCode());
        assertTrue(discount.isPresent());
        assertEquals(user.getId(), discount.get().getUser().getId());
        assertFalse(discount.get().isUsed());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("duplicate@test.com");
        request.setPassword("Password1!");
        authService.register(request);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setFirstName("Autre");
        duplicate.setLastName("User");
        duplicate.setEmail("duplicate@test.com");
        duplicate.setPassword("Password2!");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(duplicate));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void login_shouldReturnTokensForValidCredentials() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Pierre");
        request.setLastName("Durand");
        request.setEmail("pierre@test.com");
        request.setPassword("Password1!");
        authService.register(request);

        LoginRequest login = new LoginRequest();
        login.setEmail("pierre@test.com");
        login.setPassword("Password1!");

        AuthResponse response = authService.login(login);

        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("USER", response.getRole());
        assertEquals("pierre@test.com", response.getEmail());
    }

    @Test
    void login_shouldFailWithWrongPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Marie");
        request.setLastName("Leroy");
        request.setEmail("marie@test.com");
        request.setPassword("Password1!");
        authService.register(request);

        LoginRequest login = new LoginRequest();
        login.setEmail("marie@test.com");
        login.setPassword("WrongPassword1!");

        assertThrows(BadCredentialsException.class, () -> authService.login(login));
    }

    @Test
    void refresh_shouldReturnNewTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Luc");
        request.setLastName("Petit");
        request.setEmail("luc@test.com");
        request.setPassword("Password1!");
        AuthResponse registerResponse = authService.register(request);

        AuthResponse refreshResponse = authService.refresh(registerResponse.getRefreshToken());

        assertNotNull(refreshResponse.getToken());
        assertNotNull(refreshResponse.getRefreshToken());
        assertEquals("luc@test.com", refreshResponse.getEmail());
        assertNotEquals(registerResponse.getRefreshToken(), refreshResponse.getRefreshToken());
    }

    @Test
    void refresh_shouldFailWithRevokedToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Emma");
        request.setLastName("Robert");
        request.setEmail("emma@test.com");
        request.setPassword("Password1!");
        AuthResponse registerResponse = authService.register(request);

        authService.logout(registerResponse.getRefreshToken());

        assertThrows(ResponseStatusException.class,
                () -> authService.refresh(registerResponse.getRefreshToken()));
    }

    @Test
    void forgotPassword_shouldCreateResetToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("reset@test.com");
        request.setPassword("Password1!");
        authService.register(request);

        authService.forgotPassword("reset@test.com");

        User user = userRepository.findByEmail("reset@test.com").orElseThrow();
        assertNotNull(user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordTokenExpiry());
    }

    @Test
    void forgotPassword_shouldReturnSameMessageForUnknownEmail() {
        var result = authService.forgotPassword("inconnu@test.com");
        assertEquals("Si cet email existe, vous recevrez un lien de réinitialisation.",
                result.get("message"));
    }

    @Test
    void register_shouldRejectBlankPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setLastName("User");
        request.setEmail("blankpass@test.com");
        request.setPassword("");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));
        assertEquals(400, ex.getStatusCode().value());
    }
}
