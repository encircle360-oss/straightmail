package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    // Valid 32-byte AES-256 key in Base64 (same as test application.yml)
    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_KEY);
    }

    @Test
    void encrypt_and_decrypt_roundtrip() {
        String plaintext = "my-secret-smtp-password";
        String encrypted = encryptionService.encrypt(plaintext);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC("), "Encrypted value must start with ENC(");
        assertTrue(encrypted.endsWith(")"), "Encrypted value must end with )");
        assertEquals(plaintext, encryptionService.decrypt(encrypted));
    }

    @Test
    void encrypt_produces_different_ciphertexts_due_to_random_iv() {
        String plaintext = "same-input";
        String first = encryptionService.encrypt(plaintext);
        String second = encryptionService.encrypt(plaintext);
        assertNotEquals(first, second, "Each encryption must produce a different ciphertext (random IV)");
        // But both must decrypt to the same value
        assertEquals(plaintext, encryptionService.decrypt(first));
        assertEquals(plaintext, encryptionService.decrypt(second));
    }

    @Test
    void encryptIfNotEncrypted_encrypts_plaintext() {
        String plaintext = "my-password";
        String result = encryptionService.encryptIfNotEncrypted(plaintext);
        assertTrue(result.startsWith("ENC("));
        assertEquals(plaintext, encryptionService.decrypt(result));
    }

    @Test
    void encryptIfNotEncrypted_is_idempotent_for_already_encrypted_values() {
        String plaintext = "my-password";
        String encrypted = encryptionService.encryptIfNotEncrypted(plaintext);
        String secondCall = encryptionService.encryptIfNotEncrypted(encrypted);
        assertEquals(encrypted, secondCall, "Already-encrypted values must not be re-encrypted");
    }

    @Test
    void encrypt_produces_unique_ciphertexts_over_many_iterations() {
        String plaintext = "same-input";
        java.util.Set<String> ciphertexts = new java.util.HashSet<>();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            String encrypted = encryptionService.encrypt(plaintext);
            assertFalse(ciphertexts.contains(encrypted), "ciphertext should still be unique after " + i + " iterations");
            ciphertexts.add(encrypted);
            assertEquals(plaintext, encryptionService.decrypt(encrypted));
        }
        assertEquals(iterations, ciphertexts.size());
    }

    @Test
    void encrypt_null_returns_null() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void decrypt_null_returns_null() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void decrypt_non_encrypted_value_returns_it_unchanged() {
        String plaintext = "not-encrypted-value";
        assertEquals(plaintext, encryptionService.decrypt(plaintext));
    }

    @Test
    void encryptIfNotEncrypted_null_returns_null() {
        assertNull(encryptionService.encryptIfNotEncrypted(null));
    }

    @Test
    void constructor_rejects_blank_key() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService(""));
    }

    @Test
    void constructor_rejects_null_key() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService(null));
    }

    @Test
    void constructor_rejects_invalid_base64() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService("not!!valid!!base64"));
    }

    @Test
    void constructor_rejects_key_with_wrong_length() {
        // Valid Base64 but decodes to 16 bytes (128 bit) — too short for AES-256
        assertThrows(IllegalStateException.class, () -> new EncryptionService("AAAAAAAAAAAAAAAAAAAAAA=="));
    }

    @Test
    void decrypt_fails_with_corrupted_data() {
        String corrupted = "ENC(S09NRUJDQUQ=)"; // "SOMEBCAD" in Base64, too short for IV
        assertThrows(EncryptionException.class, () -> encryptionService.decrypt(corrupted));
    }

    @Test
    void decrypt_fails_with_invalid_base64() {
        String corrupted = "ENC(invalid-base64!!!)";
        assertThrows(EncryptionException.class, () -> encryptionService.decrypt(corrupted));
    }
}
