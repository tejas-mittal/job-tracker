package com.jobtracker.monolith.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for storing Gmail OAuth2 tokens at rest.
 *
 * <p>Storage format (base64url, no padding):
 * <pre>base64url( IV[12 bytes] | ciphertext | GCM-auth-tag[16 bytes] )</pre>
 *
 * <p>The encryption key is a 32-char hex string (256 bits) from the environment.
 * A fresh 96-bit IV is generated for every encryption call â€” the IV is
 * prepended to the ciphertext so decryption is self-contained.
 */
@Component
public class TokenEncryptionUtil {

    private static final String ALGORITHM    = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH    = 12;   // bytes (96 bits recommended for GCM)
    private static final int    TAG_LENGTH   = 128;  // bits
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey secretKey;

    public TokenEncryptionUtil(@Value("${encryption.key}") String hexKey) {
        byte[] keyBytes = hexToBytes(hexKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "ENCRYPTION_KEY must be exactly 32 hex bytes (64 hex chars) for AES-256. " +
                    "Got " + keyBytes.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a plaintext string.
     *
     * @param plaintext the raw token to encrypt
     * @return base64url-encoded ciphertext blob (IV + ciphertext + GCM tag)
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a ciphertext blob produced by {@link #encrypt}.
     *
     * @param ciphertextBlob base64url-encoded blob
     * @return original plaintext string
     */
    public String decrypt(String ciphertextBlob) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(ciphertextBlob);

            byte[] iv         = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed â€” key mismatch or tampered ciphertext", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
