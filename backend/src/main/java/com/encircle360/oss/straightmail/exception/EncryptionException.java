package com.encircle360.oss.straightmail.exception;

/**
 * Custom exception for encryption/decryption failures.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
