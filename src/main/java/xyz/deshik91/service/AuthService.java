package xyz.deshik91.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
}