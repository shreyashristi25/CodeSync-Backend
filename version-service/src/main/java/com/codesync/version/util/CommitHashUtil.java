package com.codesync.version.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public final class CommitHashUtil {
    private CommitHashUtil() {}

    public static String compute(Long fileId, String parentHash, String fullContent, Instant timestamp) {
        try {
            String parent = parentHash == null ? "" : parentHash;
            String basis = fileId + "\n" + parent + "\n" + timestamp.toEpochMilli() + "\n" + fullContent;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(basis.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
