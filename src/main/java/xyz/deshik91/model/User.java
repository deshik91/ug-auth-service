package xyz.deshik91.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String passwordHash;  // Будем хранить BCrypt хеш
    private String refreshToken;  // Текущий refresh токен (для инвалидации)
    private Instant createdAt;
}