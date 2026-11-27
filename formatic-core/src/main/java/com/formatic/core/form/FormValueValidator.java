package com.formatic.core.form;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Validates submission values using metadata; presentation flags do not bypass checks. */
public final class FormValueValidator {
    public List<FormValueViolation> validate(List<FormFieldMetadata> fields, Function<String, Object> values) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(values, "values");
        var patterns = new ArrayList<HtmlPattern>(fields.size());
        fields.forEach(field -> patterns.add(prepareConfiguration(field)));
        var violations = new ArrayList<FormValueViolation>();
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            Object value = values.apply(field.getName());
            boolean empty = value == null || value instanceof CharSequence text && text.isEmpty()
                    || value instanceof Collection<?> items && items.isEmpty()
                    || value != null && value.getClass().isArray() && Array.getLength(value) == 0;
            boolean unchecked = field.getType() == FormFieldType.CHECKBOX && !field.isMultiple()
                    && !Boolean.TRUE.equals(value);
            if (field.isRequired() && (empty || unchecked)) {
                add(violations, field, "required", "A value is required.");
                continue;
            }
            if (empty) {
                continue;
            }
            if (patterns.get(i) != null) {
                if (!(value instanceof CharSequence text)) {
                    throw configuration(field, "pattern requires a character sequence");
                }
                if (!patterns.get(i).matches(text)) {
                    add(violations, field, "pattern", "Must match the configured pattern.");
                }
            }
            if (FormChoiceValues.supports(field) && !FormChoiceValues.accepts(field, value)) {
                add(violations, field, "choice", "Must select an allowed value.");
            }
            if (StringFormValues.supports(field.getType())) {
                String rule = StringFormValues.violation(field.getType(), value);
                if (rule != null) {
                    String message = switch (rule) {
                        case "email" -> "Must be a valid email address.";
                        case "url" -> "Must be an absolute URI.";
                        case "color" -> "Must be a six-digit hexadecimal color.";
                        default -> "Must be a character sequence.";
                    };
                    add(violations, field, rule, message);
                    if (!(value instanceof CharSequence)) {
                        continue;
                    }
                }
            }
            if (TemporalFormValues.supports(field.getType())) {
                String rule = TemporalFormValues.violation(field, value);
                if (rule != null) {
                    String message = switch (rule) {
                        case "temporalMin" -> "Must be on or after the configured minimum.";
                        case "temporalMax" -> "Must be on or before the configured maximum.";
                        case "timeRange" -> "Must be within the configured overnight time range.";
                        case "temporalStep" -> "Must align with the configured temporal step.";
                        default -> "Must be a valid local date or time.";
                    };
                    add(violations, field, rule, message);
                }
            }
            if (field.getMinLength() >= 0 || field.getMaxLength() >= 0) {
                if (!(value instanceof CharSequence text)) {
                    throw configuration(field, "length constraints require a character sequence");
                }
                // HTML length constraints count UTF-16 code units.
                if (field.getMinLength() >= 0 && text.length() < field.getMinLength()) {
                    add(violations, field, "minLength", "Must contain at least " + field.getMinLength() + " characters.");
                }
                if (field.getMaxLength() >= 0 && text.length() > field.getMaxLength()) {
                    add(violations, field, "maxLength", "Must contain at most " + field.getMaxLength() + " characters.");
                }
            }
            if (field.getType() == FormFieldType.NUMBER || field.getType() == FormFieldType.RANGE) {
                if (!(value instanceof Number number)) {
                    throw configuration(field, "numeric control requires a Number value");
                }
                BigDecimal decimal;
                try {
                    decimal = new BigDecimal(number.toString());
                } catch (NumberFormatException exception) {
                    add(violations, field, "number", "Must be a finite number.");
                    continue;
                }
                if (!Double.isNaN(field.getMin()) && decimal.compareTo(BigDecimal.valueOf(field.getMin())) < 0) {
                    add(violations, field, "min", "Must be at least " + field.getMin() + ".");
                }
                if (!Double.isNaN(field.getMax()) && decimal.compareTo(BigDecimal.valueOf(field.getMax())) > 0) {
                    add(violations, field, "max", "Must be at most " + field.getMax() + ".");
                }
                if (!Double.isNaN(field.getStep())
                        && decimal.subtract(stepBase(field)).remainder(BigDecimal.valueOf(field.getStep())).signum() != 0) {
                    add(violations, field, "step", "Must align with the configured numeric step.");
                }
            }
        }
        return List.copyOf(violations);
    }

    public void validateConfiguration(FormFieldMetadata field) {
        prepareConfiguration(field);
    }

    private HtmlPattern prepareConfiguration(FormFieldMetadata field) {
        Objects.requireNonNull(field.getName(), "field name");
        if (field.getType() == null) {
            throw configuration(field, "control type is required");
        }
        switch (field.getType()) {
            case TEXT, TEXTAREA, PASSWORD, NUMBER, RANGE, CHECKBOX, HIDDEN, DATE, TIME, DATETIME_LOCAL,
                 SELECT, RADIO, EMAIL, URL, PHONE, COLOR -> { }
            default -> throw configuration(field, "unsupported control " + field.getType());
        }
        FormConstraintValidator.validate(field, field.getName());
        FormConstraintValidator.validateLengths(field.getName(), field.getMinLength(), field.getMaxLength());
        FormConstraintValidator.validateNumber(field.getName(), field.getMin(), field.getMax(), field.getStep());
        HtmlPattern pattern = null;
        if (hasText(field.getPattern())) {
            if (field.getType() != FormFieldType.TEXT && field.getType() != FormFieldType.PASSWORD
                    && field.getType() != FormFieldType.EMAIL && field.getType() != FormFieldType.URL
                    && field.getType() != FormFieldType.PHONE) {
                throw configuration(field, "pattern requires a supported HTML input type");
            }
            try {
                pattern = HtmlPattern.compile(field.getPattern());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(field.getName() + ": invalid or unsupported pattern '"
                        + field.getPattern() + "': " + exception.getMessage(), exception);
            }
        }
        if (field.getType() == FormFieldType.COLOR && field.isAlpha()) {
            throw configuration(field, "alpha color validation is not supported yet");
        }
        FormChoiceValues.validateConfiguration(field);
        if (!Double.isNaN(field.getStep())) {
            if (field.getType() != FormFieldType.NUMBER && field.getType() != FormFieldType.RANGE) {
                throw configuration(field, "step requires a numeric control");
            }
            stepBase(field);
        }
        if (field.getType() == FormFieldType.CHECKBOX && field.getDataType() != FormDataType.BOOLEAN
                && !(field.isMultiple() && field.getDataType() == FormDataType.ARRAY)) {
            throw configuration(field, "single checkbox requires a boolean data type");
        }
        if ((field.getMinLength() >= 0 || field.getMaxLength() >= 0)
                && field.getType() != FormFieldType.TEXT && field.getType() != FormFieldType.TEXTAREA
                && field.getType() != FormFieldType.PASSWORD && field.getType() != FormFieldType.EMAIL
                && field.getType() != FormFieldType.URL && field.getType() != FormFieldType.PHONE) {
            throw configuration(field, "length constraints require a text control");
        }
        if ((!Double.isNaN(field.getMin()) || !Double.isNaN(field.getMax()))
                && field.getType() != FormFieldType.NUMBER && field.getType() != FormFieldType.RANGE) {
            throw configuration(field, "numeric bounds require a numeric control");
        }
        TemporalFormValues.validateConfiguration(field);
        return pattern;
    }

    private static BigDecimal stepBase(FormFieldMetadata field) {
        if (!Double.isNaN(field.getMin())) {
            return BigDecimal.valueOf(field.getMin());
        }
        if (hasText(field.getDefaultValue())) {
            try {
                if (!field.getDefaultValue().matches("-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) {
                    throw configuration(field, "numeric step base default must be a finite HTML number");
                }
                return new BigDecimal(field.getDefaultValue());
            } catch (NumberFormatException exception) {
                throw configuration(field, "numeric step base default must be a finite HTML number");
            }
        }
        return BigDecimal.ZERO;
    }

    private static void add(List<FormValueViolation> result, FormFieldMetadata field, String code, String message) {
        result.add(new FormValueViolation(field.getName(), "formatic." + code,
                hasText(field.getErrorMessage()) ? field.getErrorMessage() : message));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static IllegalArgumentException configuration(FormFieldMetadata field, String message) {
        return new IllegalArgumentException(field.getName() + ": " + message);
    }
}
