package xyz.deshik91.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.deshik91.dto.request.LoginRequest;
import xyz.deshik91.dto.request.RefreshTokenRequest;
import xyz.deshik91.dto.request.RegisterRequest;
import xyz.deshik91.dto.response.AuthResponse;
import xyz.deshik91.dto.response.ValidateResponse;
import xyz.deshik91.entity.InvitationEntity;
import xyz.deshik91.entity.UserEntity;
import xyz.deshik91.repository.InvitationRepository;
import xyz.deshik91.repository.UserRepository;
import xyz.deshik91.security.JwtUtil;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Проверяем, не занят ли email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email уже зарегистрирован");
        }

        // 2. Проверяем инвайт-код
        InvitationEntity invitation = invitationRepository.findByCode(request.getInvitationCode())
                .orElseThrow(() -> new RuntimeException("Неверный код приглашения"));

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
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(Instant.now());

        UserEntity savedUser = userRepository.save(user);

        // 4. Помечаем инвайт как использованный
        invitation.setUsed(true);
        invitationRepository.save(invitation);

        // 5. Генерируем токены
        String accessToken = jwtUtil.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getEmail());

        // 6. Сохраняем refresh токен
        savedUser.setRefreshToken(refreshToken);
        userRepository.save(savedUser);

        return new AuthResponse(accessToken, refreshToken, 15 * 60L);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Ищем пользователя по email
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Неверный email или пароль"));

        // 2. Проверяем пароль
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Неверный email или пароль");
        }

        // 3. Генерируем новые токены
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // 4. Обновляем refresh токен
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken, 15 * 60L);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        // 1. Извлекаем email из refresh токена
        String email;
        try {
            email = jwtUtil.extractEmail(request.getRefreshToken());
        } catch (Exception e) {
            throw new RuntimeException("Невалидный refresh токен");
        }

        // 2. Проверяем тип токена
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
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 4. Проверяем, что refresh токен совпадает
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(request.getRefreshToken())) {
            throw new RuntimeException("Refresh токен недействителен или был отозван");
        }

        // 5. Проверяем, не истек ли токен
        if (!jwtUtil.validateToken(request.getRefreshToken(), email)) {
            throw new RuntimeException("Refresh токен истек");
        }

        // 6. Генерируем новую пару
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        // 7. Обновляем токен у пользователя
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new AuthResponse(newAccessToken, newRefreshToken, 15 * 60L);
    }

    @Transactional(readOnly = true)
    public ValidateResponse validateToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return new ValidateResponse(null, false, null);
        }

        String token = authorizationHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);
            String tokenType = jwtUtil.extractTokenType(token);
            boolean isValid = jwtUtil.validateToken(token, email);

            if (!isValid) {
                return new ValidateResponse(null, false, null);
            }

            // Проверяем, что пользователь существует
            boolean userExists = userRepository.existsByEmail(email);
            if (!userExists) {
                return new ValidateResponse(null, false, null);
            }

            return new ValidateResponse(email, true, tokenType);

        } catch (Exception e) {
            return new ValidateResponse(null, false, null);
        }
    }
}