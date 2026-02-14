package com.mario.backend.face.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class IdempotencyService {

    private static final ThreadLocal<String> currentKey = new ThreadLocal<>();

    public String computeImageHash(String imageData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(imageData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    public static void setCurrentKey(String key) {
        currentKey.set(key);
    }

    public static String getCurrentKey() {
        return currentKey.get();
    }

    public static void clearCurrentKey() {
        currentKey.remove();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
