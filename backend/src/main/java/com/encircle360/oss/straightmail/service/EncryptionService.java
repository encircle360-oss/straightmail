package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for AES-256-GCM symmetric encryption and decryption of sensitive values.
 *
 * <p>Used to encrypt secrets stored in the database (SMTP passwords, Git tokens, API keys) and
 * to decrypt them before use. Encrypted values are stored with an {@code ENC(...)} prefix, allowing
 * the service to distinguish already-encrypted values from plaintext and avoid double-encryption.
 *
 * <p>The AES key must be provided as a Base64-encoded 32-byte value via the {@code encryption.key}
 * configuration property (env: {@code ENCRYPTION_KEY}).
 * A suitable key can be generated with: {@code openssl rand -base64 32}.
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_LEN = 128;
    private static final String ENC_PREFIX = "ENC(";

    private final javax.crypto.SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Constructs the service and initialises the AES secret key.
     *
     * @param base64Key Base64-encoded 32-byte AES key from the {@code encryption.key} property
     * @throws IllegalStateException if the key is absent, not valid Base64, or not exactly 32 bytes
     */
    public EncryptionService(@Value("${encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY is not set. Generate with: openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ENCRYPTION_KEY is not valid Base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "ENCRYPTION_KEY must be exactly 32 bytes (256 bit). Got: " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext value using AES-256-GCM.
     *
     * <p>A fresh random 12-byte IV is generated for each invocation. The result is wrapped in an
     * {@code ENC(...)} envelope for storage. If the value is already prefixed with {@code ENC(},
     * it is returned as-is without re-encryption.
     *
     * @param plaintext the plaintext to encrypt; {@code null} is returned as-is
     * @return the encrypted value formatted as {@code ENC(<base64>)}, or the original value if already encrypted
     * @throws EncryptionException if the encryption operation fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.startsWith(ENC_PREFIX)) return plaintext;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // concatenated without length arithmetic, so no input size can overflow the buffer size
            ByteArrayOutputStream combined = new ByteArrayOutputStream();
            combined.writeBytes(iv);
            combined.writeBytes(ciphertext);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined.toByteArray()) + ")";
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a value that was previously encrypted by {@link #encrypt}.
     *
     * <p>Values not prefixed with {@code ENC(} are returned as-is (treated as already-plaintext).
     *
     * @param encrypted the encrypted value in {@code ENC(<base64>)} format; {@code null} is returned as-is
     * @return the decrypted plaintext string
     * @throws EncryptionException if decryption fails, e.g. due to a wrong key or corrupted data
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith(ENC_PREFIX)) return encrypted;
        try {
            String b64 = encrypted.substring(4, encrypted.length() - 1);
            byte[] combined = Base64.getDecoder().decode(b64);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LEN);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LEN, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LEN, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed — wrong key or corrupted data", e);
        }
    }

    /**
     * Encrypts the given value only if it is not already encrypted.
     *
     * <p>A value is considered already encrypted if it starts with the {@code ENC(} prefix.
     * This is a safe convenience wrapper around {@link #encrypt} that prevents double-encryption
     * when updating tenant credentials.
     *
     * @param value the value to conditionally encrypt; {@code null} is returned as-is
     * @return the encrypted value, or the original value if it was already encrypted or {@code null}
     */
    public String encryptIfNotEncrypted(String value) {
        if (value == null || value.startsWith(ENC_PREFIX)) return value;
        return encrypt(value);
    }
}
