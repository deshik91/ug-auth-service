package xyz.deshik91.repository;

import org.springframework.stereotype.Component;
import xyz.deshik91.model.User;
import xyz.deshik91.model.Invitation;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryUserStore {
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, Invitation> invitationsByCode = new ConcurrentHashMap<>();
    private final AtomicLong userIdCounter = new AtomicLong(1);

    // Пользователи
    public User saveUser(User user) {
        if (user.getId() == null) {
            user.setId(userIdCounter.getAndIncrement());
        }
        usersByEmail.put(user.getEmail(), user);
        return user;
    }

    public User findByEmail(String email) {
        return usersByEmail.get(email);
    }

    // Инвайты
    public void saveInvitation(Invitation invitation) {
        invitationsByCode.put(invitation.getCode(), invitation);
    }

    public Invitation findInvitationByCode(String code) {
        return invitationsByCode.get(code);
    }

    public void markInvitationAsUsed(String code) {
        Invitation inv = invitationsByCode.get(code);
        if (inv != null) {
            inv.setUsed(true);
        }
    }

    // Для тестов - создадим один инвайт при старте
    public void initDefaultInvitation() {
        Invitation defaultInv = new Invitation(
                "WELCOME2024",
                null,  // любой email может использовать
                false,
                Instant.now().plusSeconds(30 * 24 * 60 * 60), // 30 дней
                Instant.now()
        );
        invitationsByCode.put(defaultInv.getCode(), defaultInv);

        // Еще один инвайт для конкретного email
        Invitation specificInv = new Invitation(
                "FOR_DESHIK",
                "deshik@example.com",
                false,
                Instant.now().plusSeconds(30 * 24 * 60 * 60),
                Instant.now()
        );
        invitationsByCode.put(specificInv.getCode(), specificInv);
    }
}