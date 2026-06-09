package at.htlwels.votevox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main entry point for the VoteVox backend.
 * <p>
 * VoteVox is a secure QR-code based school voting system for HTL Wels.
 * The application guarantees voter anonymity by construction: voting tokens are
 * anonymous and grouped only by school class (never bound to a person), and a
 * cast Vote holds no reference back to the token that produced it. Eligibility
 * is enforced purely by single-use cryptographic tokens delivered through QR codes.
 * </p>
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class VoteVoxApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoteVoxApplication.class, args);
    }
}
