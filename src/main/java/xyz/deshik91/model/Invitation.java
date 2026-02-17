package xyz.deshik91.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {
    private String code;           // Уникальный код приглашения
    private String email;          // На какой email выдан (может быть null, если "общий")
    private boolean used;           // Использован или нет
    private Instant expiresAt;      // Срок действия
    private Instant createdAt;
}