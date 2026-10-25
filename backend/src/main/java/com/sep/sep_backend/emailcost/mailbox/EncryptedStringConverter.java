package com.sep.sep_backend.emailcost.mailbox;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Encrypts sensitive strings (e.g. mailbox app passwords) before they are
 * written to the database and decrypts them on read. The entity keeps working
 * with plaintext values; encryption is transparent.
 *
 * The key and salt are read from the MAILBOX_ENCRYPTION_KEY / MAILBOX_ENCRYPTION_SALT
 * environment variables, with development defaults so the app still starts locally.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String DEFAULT_KEY = "lovelace-mailbox-secret-key";
    private static final String DEFAULT_SALT = "5c0744940b5c369b";

    private final TextEncryptor encryptor;

    public EncryptedStringConverter() {
        String key = resolve("MAILBOX_ENCRYPTION_KEY", DEFAULT_KEY);
        String salt = resolve("MAILBOX_ENCRYPTION_SALT", DEFAULT_SALT);
        this.encryptor = Encryptors.text(key, salt);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            return encryptor.decrypt(dbData);
        } catch (Exception exception) {
            // Legacy plaintext value stored before encryption was introduced.
            return dbData;
        }
    }

    private String resolve(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
