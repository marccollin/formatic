package com.formatic.core.form;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/** Format checks for nonempty string controls, without browser input sanitization. */
final class StringFormValues {
    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
                    + "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*");
    private static final Pattern COLOR = Pattern.compile("#[a-fA-F0-9]{6}");

    private StringFormValues() {}

    static boolean supports(FormFieldType type) {
        return type == FormFieldType.EMAIL || type == FormFieldType.URL
                || type == FormFieldType.PHONE || type == FormFieldType.COLOR;
    }

    static String violation(FormFieldType type, Object value) {
        if (!(value instanceof CharSequence text)) {
            return "format";
        }
        String string = text.toString();
        return switch (type) {
            case EMAIL -> EMAIL.matcher(string).matches() ? null : "email";
            case URL -> absoluteUri(string) ? null : "url";
            case COLOR -> COLOR.matcher(string).matches() ? null : "color";
            // HTML tel inputs intentionally impose no universal telephone format.
            case PHONE -> null;
            default -> throw new IllegalArgumentException("Unsupported string control " + type);
        };
    }

    private static boolean absoluteUri(String value) {
        try {
            return new URI(value).isAbsolute();
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
