package at.htlwels.votevox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main entry point for the VoteVox backend.
 * <p>
 * VoteVox is a secure QR-code based school voting system for HTL Wels.
 * The application guarantees voter anonymity by schema-level separation of
 * voter identity (Student) and ballot (Vote), enforced via the database schema
 * (no foreign key from votes to students), and by single-use cryptographic tokens
 * delivered through QR codes.
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
