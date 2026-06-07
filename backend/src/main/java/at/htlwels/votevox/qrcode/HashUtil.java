package at.htlwels.votevox.qrcode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for SHA-256 hashing of token plaintexts.
 */
public final class HashUtil {

    private HashUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Computes the SHA-256 digest of the input UTF-8 bytes and returns it as
     * a lowercase 64-character hexadecimal string.
     */
    public static String sha256Hex(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plaintext.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
