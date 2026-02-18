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
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.entity.InvitationEntity;
import xyz.deshik91.repository.InvitationRepository;
import xyz.deshik91.repository.UserRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private UserRepository userRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @BeforeEach
    void setUp() throws Exception {
        // Очищаем БД через репозитории
        userRepository.deleteAll();
        invitationRepository.deleteAll();

        System.out.println("=== Creating invitation in LoginTest ===");

        // Создаем тестовый инвайт
        InvitationEntity invitation = new InvitationEntity();
        invitation.setCode("WELCOME2024");
        invitation.setUsed(false);
        invitation.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
        invitation.setCreatedAt(Instant.now());

        InvitationEntity saved = invitationRepository.save(invitation);
        System.out.println("Saved invitation with code: " + saved.getCode() + ", id: " + saved.getId());

        // Проверяем что инвайт сохранился
        var found = invitationRepository.findByCode("WELCOME2024");
        System.out.println("Found invitation: " + found.isPresent());

        // Регистрируем пользователя для тестов
        System.out.println("=== Registering test user ===");

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("logintest@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setInvitationCode("WELCOME2024");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn(); // Не проверяем статус здесь, просто получаем результат

        System.out.println("Registration status: " + result.getResponse().getStatus());
        System.out.println("Registration response: " + result.getResponse().getContentAsString());

        // Теперь проверяем что статус 200
        assertEquals(200, result.getResponse().getStatus(),
                "Registration failed: " + result.getResponse().getContentAsString());
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