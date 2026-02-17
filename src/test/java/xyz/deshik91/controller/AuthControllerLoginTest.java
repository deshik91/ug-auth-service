package xyz.deshik91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import xyz.deshik91.dto.request.LoginRequest;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.repository.InMemoryUserStore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @BeforeEach
    void setUp() throws Exception {
        // Очищаем хранилище и создаем тестового пользователя
        userStore.initDefaultInvitation();

        // Регистрируем пользователя для тестов
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("logintest@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setInvitationCode("WELCOME2024");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
    }

    @Test
    void whenValidLogin_thenReturnsTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logintest@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void whenInvalidPassword_thenReturnsError() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logintest@example.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный email или пароль"));
    }

    @Test
    void whenNonExistentEmail_thenReturnsError() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nosuchuser@example.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный email или пароль"));
    }
}