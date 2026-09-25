package ru.otus.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PasswordDigest {

    public String md5(String raw) {
        try {
            var digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 недоступен", ex);
        }
    }

    public boolean matches(String raw, String hash) {
        if (hash == null || raw == null) {
            return false;
        }
        return md5(raw).equalsIgnoreCase(hash);
    }
}
