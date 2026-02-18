package xyz.deshik91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import xyz.deshik91.dto.request.LoginRequest;
import xyz.deshik91.dto.request.RefreshTokenRequest;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.dto.response.AuthResponse;
import xyz.deshik91.repository.InMemoryUserStore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    private String refreshToken;

    @BeforeEach
    void setUp() throws Exception {
        // Очищаем хранилище перед каждым тестом
        userStore.clear();

        // Инициализируем инвайты заново
        userStore.initDefaultInvitation();

        // Регистрируем пользователя
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("refresh@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setInvitationCode("WELCOME2024");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())  // Теперь должно быть 200
                .andReturn();

        // Сохраняем refresh токен из ответа
        AuthResponse authResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        refreshToken = authResponse.getRefreshToken();
    }

    @Test
    void whenValidRefreshToken_thenReturnsNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void whenInvalidRefreshToken_thenReturnsError() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.token.string");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Невалидный refresh токен"));
    }

    @Test
    void whenRefreshTokenAlreadyUsed_thenReturnsError() throws Exception {
        // Используем refresh токен первый раз
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        MvcResult firstRefresh = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Пытаемся использовать тот же токен снова
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh токен недействителен или был отозван"));
    }

    @Test
    void whenLoginWithValidCredentials_thenRefreshTokenIsUpdated() throws Exception {
        // Сохраняем старый токен
        String oldRefreshToken = refreshToken;

        // Логинимся
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("refresh@example.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Получаем новый refresh токен после логина
        AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String newRefreshToken = loginResponse.getRefreshToken();

        // Проверяем что токены разные
        org.assertj.core.api.Assertions.assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        // Старый токен (из регистрации) должен стать недействительным
        RefreshTokenRequest oldRequest = new RefreshTokenRequest();
        oldRequest.setRefreshToken(oldRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldRequest)))
                .andExpect(status().isBadRequest());

        // А новый должен работать
        RefreshTokenRequest newRequest = new RefreshTokenRequest();
        newRequest.setRefreshToken(newRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isOk());
    }
}