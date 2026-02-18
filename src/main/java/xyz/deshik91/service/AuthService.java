package xyz.deshik91.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.deshik91.dto.request.LoginRequest;
import xyz.deshik91.dto.request.RefreshTokenRequest;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.dto.response.AuthResponse;
import xyz.deshik91.model.Invitation;
import xyz.deshik91.model.User;
import xyz.deshik91.repository.InMemoryUserStore;
import xyz.deshik91.security.JwtUtil;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final InMemoryUserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        // 1. Проверяем, не занят ли email
        if (userStore.findByEmail(request.getEmail()) != null) {
            throw new RuntimeException("Email уже зарегистрирован");
        }

        // 2. Проверяем инвайт-код
        Invitation invitation = userStore.findInvitationByCode(request.getInvitationCode());
        if (invitation == null) {
            throw new RuntimeException("Неверный код приглашения");
        }

        if (invitation.isUsed()) {
            throw new RuntimeException("Код приглашения уже использован");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Срок действия кода приглашения истек");
        }

        // Если инвайт на конкретный email, проверяем соответствие
        if (invitation.getEmail() != null && !invitation.getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Код приглашения выдан на другой email");
        }

        // 3. Создаем пользователя
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(Instant.now());

        User savedUser = userStore.saveUser(user);

        // 4. Помечаем инвайт как использованный
        userStore.markInvitationAsUsed(request.getInvitationCode());

        // 5. Генерируем токены
        String accessToken = jwtUtil.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getEmail());

        // 6. Сохраняем refresh токен (чтобы можно было инвалидировать)
        savedUser.setRefreshToken(refreshToken);
        userStore.saveUser(savedUser); // обновляем пользователя с токеном

        return new AuthResponse(accessToken, refreshToken, 15 * 60L); // 15 минут в секундах
    }

    // Добавь этот метод в класс AuthService
    public AuthResponse login(LoginRequest request) {
        // 1. Ищем пользователя по email
        User user = userStore.findByEmail(request.getEmail());
        if (user == null) {
            throw new RuntimeException("Неверный email или пароль");
        }

        // 2. Проверяем пароль
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Неверный email или пароль");
        }

        // 3. Генерируем новые токены
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // 4. Обновляем refresh токен у пользователя (для возможности инвалидации)
        user.setRefreshToken(refreshToken);
        userStore.saveUser(user);

        return new AuthResponse(accessToken, refreshToken, 15 * 60L);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        // 1. Извлекаем email из refresh токена
        String email;
        try {
            email = jwtUtil.extractEmail(request.getRefreshToken());
        } catch (Exception e) {
            throw new RuntimeException("Невалидный refresh токен");
        }

        // 2. Проверяем тип токена (должен быть refresh)
        String tokenType;
        try {
            tokenType = jwtUtil.extractTokenType(request.getRefreshToken());
        } catch (Exception e) {
            throw new RuntimeException("Невалидный refresh токен");
        }

        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("Неверный тип токена. Ожидался refresh токен");
        }

        // 3. Ищем пользователя
        User user = userStore.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Пользователь не найден");
        }

        // 4. Проверяем, что refresh токен совпадает с тем, что мы выдали
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(request.getRefreshToken())) {
            throw new RuntimeException("Refresh токен недействителен или был отозван");
        }

        // 5. Проверяем, не истек ли refresh токен
        if (!jwtUtil.validateToken(request.getRefreshToken(), email)) {
            throw new RuntimeException("Refresh токен истек");
        }

        // 6. Генерируем новую пару токенов
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        // 7. Обновляем refresh токен у пользователя (ротация токенов)
        user.setRefreshToken(newRefreshToken);
        userStore.saveUser(user);

        return new AuthResponse(newAccessToken, newRefreshToken, 15 * 60L);
    }
}