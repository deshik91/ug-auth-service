package xyz.deshik91.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.repository.InMemoryUserStore;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserStore userStore;

    @BeforeEach
    void setUp() {
        // Инициализируем тестовые инвайты
        userStore.initDefaultInvitation();
    }

    @Test
    public void whenValidRegister_thenReturnsTokens() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setInvitationCode("WELCOME2024");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    public void whenDuplicateEmail_thenReturnsError() throws Exception {
        // Первая регистрация
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("duplicate@example.com");
        request1.setPassword("password123");
        request1.setInvitationCode("WELCOME2024");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Вторая регистрация с тем же email
        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("duplicate@example.com");
        request2.setPassword("password123");
        request2.setInvitationCode("WELCOME2024");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email уже зарегистрирован"));
    }

    @Test
    public void whenInvalidInvitation_thenReturnsError() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test2@example.com");
        request.setPassword("password123");
        request.setInvitationCode("INVALID_CODE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный код приглашения"));
    }
}