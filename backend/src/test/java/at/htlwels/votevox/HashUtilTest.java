package at.htlwels.votevox;

import at.htlwels.votevox.qrcode.HashUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void sha256OfEmptyStringIsKnownConstant() {
        // Reference vector for SHA-256("")
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashUtil.sha256Hex("")
        );
    }

    @Test
    void sha256ProducesLowercase64HexChars() {
        String hash = HashUtil.sha256Hex("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"), "must be lowercase hex");
    }

    @Test
    void nullInputRejected() {
        assertThrows(IllegalArgumentException.class, () -> HashUtil.sha256Hex(null));
    }

    @Test
    void hashesAreDeterministic() {
        assertEquals(HashUtil.sha256Hex("vote"), HashUtil.sha256Hex("vote"));
        assertNotEquals(HashUtil.sha256Hex("vote"), HashUtil.sha256Hex("Vote"));
    }
}
