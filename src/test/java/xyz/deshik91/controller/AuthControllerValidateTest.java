package xyz.deshik91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.dto.response.AuthResponse;
import xyz.deshik91.entity.InvitationEntity;
import xyz.deshik91.repository.InvitationRepository;
import xyz.deshik91.repository.UserRepository;

import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerValidateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    private String accessToken;
    private String refreshToken;
    private String userEmail;

    @BeforeEach
    void setUp() throws Exception {
        // Очищаем БД
        userRepository.deleteAll();
        invitationRepository.deleteAll();

        // Создаем тестовый инвайт
        InvitationEntity invitation = new InvitationEntity();
        invitation.setCode("WELCOME2024");
        invitation.setUsed(false);
        invitation.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
        invitation.setCreatedAt(Instant.now());
        invitationRepository.save(invitation);

        // Регистрируем пользователя
        userEmail = "validate@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(userEmail);
        registerRequest.setPassword("password123");
        registerRequest.setInvitationCode("WELCOME2024");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        accessToken = authResponse.getAccessToken();
        refreshToken = authResponse.getRefreshToken();
    }

    @Test
    void whenValidAccessToken_thenReturnsValidTrue() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tokenType").value("access"));
    }

    @Test
    void whenValidRefreshToken_thenReturnsValidTrue() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tokenType").value("refresh"));
    }

    @Test
    void whenNoBearerPrefix_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", accessToken))  // Без "Bearer "
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void whenInvalidToken_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void whenNoAuthHeader_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))  // Без заголовка
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void whenUserDeleted_thenReturnsUnauthorized() throws Exception {
        // Сначала проверяем что токен работает
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // "Удаляем" пользователя через репозиторий
        userRepository.deleteAll();  // Очищаем всех пользователей

        // Проверяем снова - должно быть 401
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void whenTokenExpired_thenReturnsUnauthorized() throws Exception {
        // Создаем токен с истекшим сроком действия
        // Используем рефлексию чтобы подменить время или создаем заведомо просроченный токен

        // Вариант 1: Создаем токен вручную с истекшей датой
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("test@example.com")
                .claim("type", "access")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // час назад
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60)) // истек минуту назад
                .signWith(Keys.hmacShaKeyFor("mySecretKeyForJWTTokenGeneration2024VeryLongAndSecure".getBytes()))
                .compact();

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}