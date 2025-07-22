package com.formatic.core.utils;

/**
 * Utility class for common String operations.
 *
 * Currently provides methods to check if a string contains non-blank text.
 * Avoid to add external dependencies
 */
public class StringUtils {

    public static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

}
