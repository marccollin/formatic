package com.formatic.core.form;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * Explicit, non-null data value for schema defaults and choices.
 * Integers use BigInteger, numbers use BigDecimal, and arrays contain scalar
 * FormDataValue elements. This does not change HTML string values.
 */
public record FormDataValue(FormDataType type, Object value) {
    public FormDataValue {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        boolean valid = switch (type) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof BigInteger;
            case NUMBER -> value instanceof BigDecimal;
            case BOOLEAN -> value instanceof Boolean;
            case ARRAY -> value instanceof List<?> items
                    && items.stream().allMatch(item ->
                    item instanceof FormDataValue data && data.type() != FormDataType.ARRAY);
        };
        if (!valid) {
            throw new IllegalArgumentException("Value does not match data type " + type);
        }
        if (value instanceof List<?> items) {
            value = List.copyOf(items);
        }
    }

    /**
     * Converts an HTML string using an explicit scalar type. Empty strings are
     * valid strings; arrays require explicit structured values.
     */
    public static FormDataValue fromText(FormDataType type, String text, String location) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(location, "location");
        try {
            return switch (type) {
                case STRING -> new FormDataValue(type, text);
                case INTEGER -> {
                    if (!text.matches("-?(0|[1-9][0-9]*)")) {
                        throw new IllegalArgumentException("expected a JSON integer");
                    }
                    yield new FormDataValue(type, new BigInteger(text));
                }
                case NUMBER -> {
                    if (!text.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
                        throw new IllegalArgumentException("expected a JSON number");
                    }
                    yield new FormDataValue(type, new BigDecimal(text));
                }
                case BOOLEAN -> {
                    if (!text.equals("true") && !text.equals("false")) {
                        throw new IllegalArgumentException("expected true or false");
                    }
                    yield new FormDataValue(type, Boolean.valueOf(text));
                }
                case ARRAY -> throw new IllegalArgumentException("array values must be supplied explicitly");
            };
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(location + ": invalid " + type + " value", exception);
        }
    }
}
