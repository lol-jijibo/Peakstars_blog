package com.interview.auth.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 共享的文件魔数校验工具，用于防止文件扩展名伪装攻击。
 * 统一 AdminController / AdminBookController / AdminBookServiceImpl 中重复的魔数检测逻辑。
 */
public final class MagicBytesValidator {

    private MagicBytesValidator() {
    }

    // ── 基础方法 ──

    public static boolean match(byte[] header, int offset, int... expected) {
        for (int i = 0; i < expected.length; i++) {
            if ((header[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] readHeader(InputStream in, int size) throws IOException {
        byte[] header = new byte[size];
        int totalRead = 0;
        while (totalRead < size) {
            int read = in.read(header, totalRead, size - totalRead);
            if (read < 0) {
                break;
            }
            totalRead += read;
        }
        return header;
    }

    // ── 图片格式 ──

    public static boolean isPng(byte[] header) {
        return header.length >= 4 && match(header, 0, 0x89, 0x50, 0x4E, 0x47);
    }

    public static boolean isJpeg(byte[] header) {
        return header.length >= 3 && match(header, 0, 0xFF, 0xD8, 0xFF);
    }

    public static boolean isGif(byte[] header) {
        return header.length >= 4 && match(header, 0, 0x47, 0x49, 0x46, 0x38);
    }

    public static boolean isWebP(byte[] header) {
        return header.length >= 12
            && match(header, 0, 0x52, 0x49, 0x46, 0x46)
            && match(header, 8, 0x57, 0x45, 0x42, 0x50);
    }

    public static boolean isBmp(byte[] header) {
        return header.length >= 2 && match(header, 0, 0x42, 0x4D);
    }

    public static boolean isTiff(byte[] header) {
        return header.length >= 4
            && (match(header, 0, 0x49, 0x49, 0x2A, 0x00)
                || match(header, 0, 0x4D, 0x4D, 0x00, 0x2A));
    }

    public static boolean isSvg(byte[] header) {
        String content = new String(header, 0, Math.min(header.length, 512), StandardCharsets.UTF_8)
            .trim().toLowerCase(Locale.ROOT);
        return content.startsWith("<svg") || content.contains("<svg");
    }

    public static boolean isAvif(byte[] header) {
        if (header.length < 12 || !match(header, 4, 0x66, 0x74, 0x79, 0x70)) {
            return false;
        }
        String boxType = new String(header, 4, 8, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
        return boxType.contains("avif") || boxType.contains("avis");
    }

    public static boolean isImage(byte[] header) {
        return isPng(header) || isJpeg(header) || isGif(header)
            || isWebP(header) || isBmp(header) || isTiff(header)
            || isSvg(header) || isAvif(header);
    }

    // ── 文档格式 ──

    public static boolean isPdf(byte[] header) {
        return header.length >= 4 && match(header, 0, 0x25, 0x50, 0x44, 0x46);
    }

    public static boolean isZipBased(byte[] header) {
        return header.length >= 4 && match(header, 0, 0x50, 0x4B, 0x03, 0x04);
    }
}
