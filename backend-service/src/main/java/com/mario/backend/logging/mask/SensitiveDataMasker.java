package com.mario.backend.logging.mask;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Pure utility for masking sensitive fields in log output.
 * No Spring dependency — safe to use anywhere.
 */
public final class SensitiveDataMasker {

    private static final Set<String> DEFAULT_SENSITIVE_FIELDS = Set.of(
            "password", "salt", "token", "refreshToken", "accessToken",
            "secret", "secretKey"
    );

    private static final Set<String> HASHABLE_FIELDS = Set.of(
            "imageBase64", "imageData"
    );

    private static final int MAX_STRING_LENGTH = 100;

    private SensitiveDataMasker() {}

    public static String mask(Object value, String[] extraSensitiveFields) {
        try {
            if (value == null) {
                return "null";
            }
            if (value instanceof String s && looksLikeBase64(s)) {
                return sha1(s);
            }
            if (isSimpleType(value)) {
                return value.toString();
            }
            if (value instanceof Collection<?> c) {
                return "[" + c.size() + " items]";
            }
            if (value instanceof Map<?, ?> m) {
                return "{" + m.size() + " entries}";
            }
            if (value.getClass().isArray()) {
                return "[array]";
            }
            return maskObject(value, extraSensitiveFields);
        } catch (StackOverflowError e) {
            return "<circular-reference>";
        } catch (OutOfMemoryError e) {
            return "<object-too-large>";
        } catch (Exception e) {
            return "<masking-failed:" + e.getClass().getSimpleName() + ">";
        }
    }

    private static boolean isSimpleType(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof UUID;
    }

    private static boolean isSensitive(String fieldName, String[] extraFields) {
        String lower = fieldName.toLowerCase();
        if (DEFAULT_SENSITIVE_FIELDS.stream().anyMatch(s -> lower.contains(s.toLowerCase()))) {
            return true;
        }
        for (String extra : extraFields) {
            if (lower.contains(extra.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHashable(String fieldName) {
        String lower = fieldName.toLowerCase();
        return HASHABLE_FIELDS.stream().anyMatch(s -> lower.contains(s.toLowerCase()));
    }

    private static String sha1(Object value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.toString().getBytes(StandardCharsets.UTF_8));
            return "sha1:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "sha1:<unavailable>";
        }
    }

    private static String maskObject(Object obj, String[] extraFields) {
        Class<?> clazz = obj.getClass();
        StringJoiner joiner = new StringJoiner(", ", clazz.getSimpleName() + "{", "}");

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(obj);
                String name = field.getName();

                if (isHashable(name) && fieldValue != null) {
                    joiner.add(name + "=" + sha1(fieldValue));
                } else if (isSensitive(name, extraFields)) {
                    joiner.add(name + "=***");
                } else if (fieldValue == null) {
                    joiner.add(name + "=null");
                } else if (fieldValue instanceof String s) {
                    joiner.add(name + "=" + truncate(s));
                } else if (isSimpleType(fieldValue)) {
                    joiner.add(name + "=" + fieldValue);
                } else if (fieldValue instanceof Collection<?> c) {
                    joiner.add(name + "=[" + c.size() + " items]");
                } else if (fieldValue instanceof Map<?, ?> m) {
                    joiner.add(name + "={" + m.size() + " entries}");
                } else {
                    joiner.add(name + "=" + fieldValue.getClass().getSimpleName());
                }
            } catch (IllegalAccessException e) {
                joiner.add(field.getName() + "=<inaccessible>");
            }
        }
        return joiner.toString();
    }

    private static boolean looksLikeBase64(String s) {
        return s.length() > 200 && s.matches("^[A-Za-z0-9+/=\\s]+$");
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_STRING_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_STRING_LENGTH) + "...[" + s.length() + " chars]";
    }
}
