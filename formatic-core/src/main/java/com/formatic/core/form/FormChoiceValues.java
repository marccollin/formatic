package com.formatic.core.form;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Typed static-choice membership without coercing submitted JSON value kinds. */
final class FormChoiceValues {
    private FormChoiceValues() {}

    static boolean supports(FormFieldMetadata field) {
        return field.getType() == FormFieldType.SELECT || field.getType() == FormFieldType.RADIO
                || field.getType() == FormFieldType.CHECKBOX && field.isMultiple();
    }

    static void validateConfiguration(FormFieldMetadata field) {
        if (field.getOptionsProvider() != null && !field.getOptionsProvider().isEmpty()) {
            throw invalid(field, "dynamic optionsProvider validation is not supported yet");
        }
        if (!supports(field)) {
            if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                throw invalid(field, "choices require a select, radio, or multiple checkbox");
            }
            return;
        }
        if (field.getOptions() == null || field.getOptions().isEmpty()) {
            throw invalid(field, "choice validation requires static options");
        }
        if (field.isMultiple() != (field.getDataType() == FormDataType.ARRAY)
                || field.getType() == FormFieldType.RADIO && field.isMultiple()) {
            throw invalid(field, "choice multiplicity must match its data type");
        }
        FormFieldValues.optionValues(field);
    }

    static boolean accepts(FormFieldMetadata field, Object value) {
        List<Object> submitted = new ArrayList<>();
        if (field.isMultiple()) {
            if (value instanceof Collection<?> items) {
                submitted.addAll(items);
            } else if (value.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(value); i++) {
                    submitted.add(Array.get(value, i));
                }
            } else {
                return false;
            }
        } else {
            submitted.add(value);
        }
        List<FormDataValue> options = FormFieldValues.optionValues(field);
        FormDataType type = field.isMultiple() ? field.getItemDataType() : field.getDataType();
        return submitted.stream().allMatch(item -> {
            Object canonical = canonical(type, item);
            return canonical != null && options.stream()
                    .anyMatch(option -> canonical.equals(canonical(option.type(), option.value())));
        });
    }

    private static Object canonical(FormDataType type, Object value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case STRING -> value instanceof Enum<?> choice ? choice.name()
                    : value instanceof CharSequence || value instanceof Character
                    || value instanceof java.util.UUID || value instanceof java.time.LocalDate
                    || value instanceof java.time.LocalTime || value instanceof java.time.LocalDateTime
                    || value instanceof java.time.OffsetDateTime || value instanceof java.time.Instant
                    ? value.toString() : null;
            case BOOLEAN -> value instanceof Boolean ? value : null;
            case INTEGER, NUMBER -> {
                if (!(value instanceof Number number)) {
                    yield null;
                }
                try {
                    BigDecimal decimal = new BigDecimal(number.toString()).stripTrailingZeros();
                    yield type == FormDataType.INTEGER && decimal.scale() > 0 ? null : decimal;
                } catch (NumberFormatException exception) {
                    yield null;
                }
            }
            case ARRAY -> null;
        };
    }

    private static IllegalArgumentException invalid(FormFieldMetadata field, String message) {
        return new IllegalArgumentException(field.getName() + ": " + message);
    }
}
