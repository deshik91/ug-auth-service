package xyz.deshik91.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.deshik91.entity.InvitationEntity;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    Optional<InvitationEntity> findByCode(String code);
    boolean existsByCode(String code);
}