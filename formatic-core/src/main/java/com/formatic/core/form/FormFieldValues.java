package com.formatic.core.form;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Read-only preparation of metadata values for schema export. */
public final class FormFieldValues {
    private FormFieldValues() {}

    /**
     * An explicit typed default takes precedence. Legacy null/empty strings
     * mean absent; use a typed STRING value to represent an explicit empty default.
     */
    public static Optional<FormDataValue> defaultValue(FormFieldMetadata field) {
        Objects.requireNonNull(field, "field");
        FormDataValue explicit = field.getTypedDefaultValue();
        if (explicit != null) {
            requireType(field, explicit, field.getDataType(), "default");
            if (explicit.value() instanceof List<?> items) {
                FormDataType itemType = requireKnownType(field, field.getItemDataType(), "default item");
                for (Object item : items) {
                    if (item instanceof FormDataValue value) {
                        requireType(field, value, itemType, "default item");
                    }
                }
            }
            return Optional.of(explicit);
        }
        String legacy = field.getDefaultValue();
        if (legacy == null || legacy.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(FormDataValue.fromText(
                requireKnownType(field, field.getDataType(), "default"), legacy, field.getName() + ": default"));
    }

    /**
     * Converts choices without modifying metadata or provider-owned options.
     * Duplicate values are rejected after conversion (for example 1.0 and 1.00).
     * Option labels and other presentation state remain on the original options.
     */
    public static List<FormDataValue> optionValues(FormFieldMetadata field) {
        Objects.requireNonNull(field, "field");
        List<SelectRadioOption> options = field.getOptions();
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        FormDataType type = requireKnownType(field,
                field.getDataType() == FormDataType.ARRAY ? field.getItemDataType() : field.getDataType(), "options");
        List<FormDataValue> values = options.stream().map(option -> {
            if (option == null || option.getValue() == null) {
                throw new IllegalArgumentException(field.getName() + ": null option value");
            }
            return FormDataValue.fromText(type, option.getValue(), field.getName() + ": option");
        }).toList();
        long unique = values.stream().map(value ->
                value.value() instanceof java.math.BigDecimal number ? number.stripTrailingZeros() : value.value())
                .distinct().count();
        if (unique != values.size()) {
            throw new IllegalArgumentException(field.getName() + ": duplicate typed option values");
        }
        return values;
    }

    private static void requireType(FormFieldMetadata field, FormDataValue value, FormDataType expected,
                                    String feature) {
        if (value.type() != requireKnownType(field, expected, feature)) {
            throw new IllegalArgumentException(field.getName() + ": " + feature + " does not match data type " + expected);
        }
    }

    private static FormDataType requireKnownType(FormFieldMetadata field, FormDataType type, String feature) {
        if (type == null) {
            throw new IllegalArgumentException(field.getName() + ": unknown data type for " + feature);
        }
        return type;
    }
}
