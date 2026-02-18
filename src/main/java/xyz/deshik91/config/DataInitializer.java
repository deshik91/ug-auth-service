package xyz.deshik91.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.deshik91.entity.InvitationEntity;
import xyz.deshik91.repository.InvitationRepository;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final InvitationRepository invitationRepository;

    @Override
    public void run(String... args) throws Exception {
        // Создаем тестовые инвайты только если их нет
        if (invitationRepository.count() == 0) {
            InvitationEntity defaultInv = new InvitationEntity();
            defaultInv.setCode("WELCOME2024");
            defaultInv.setUsed(false);
            defaultInv.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 дней
            defaultInv.setCreatedAt(Instant.now());
            invitationRepository.save(defaultInv);

            InvitationEntity specificInv = new InvitationEntity();
            specificInv.setCode("FOR_DESHIK");
            specificInv.setEmail("deshik@example.com");
            specificInv.setUsed(false);
            specificInv.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
            specificInv.setCreatedAt(Instant.now());
            invitationRepository.save(specificInv);
        }
    }
}