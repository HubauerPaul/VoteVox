package at.htlwels.votevox.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    List<AdminUser> findAllByPasswordHash(String passwordHash);

    boolean existsByEmailIgnoreCase(String email);
}
