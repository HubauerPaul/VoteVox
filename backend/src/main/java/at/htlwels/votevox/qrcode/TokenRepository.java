package at.htlwels.votevox.qrcode;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByTokenValueHash(String tokenValueHash);

    /**
     * Pessimistic write lock on the token row. Used during vote casting to
     * serialize concurrent attempts and guarantee single use.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Token t WHERE t.tokenValueHash = :hash")
    Optional<Token> findByTokenValueHashForUpdate(String hash);
}
