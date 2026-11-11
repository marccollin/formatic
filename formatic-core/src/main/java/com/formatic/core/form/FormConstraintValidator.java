package com.formatic.core.form;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormConstraintValidator {
    private static final Pattern DATE = Pattern.compile("([0-9]{4,})-([0-9]{2})-([0-9]{2})");
    private static final Pattern FLOAT = Pattern.compile("-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    private FormConstraintValidator() {}

    public static void validate(FormFieldMetadata field, String location) {
        if (field.getType() == null) return;
        switch (field.getType()) {
            case TEXT, TEXTAREA, EMAIL, PASSWORD, PHONE, URL ->
                    validateLengths(location, field.getMinLength(), field.getMaxLength());
            case NUMBER -> validateNumber(location, field.getMin(), field.getMax(), field.getStep());
            case DATE -> validateDate(location, field.getMinDate(), field.getMaxDate(), field.getDateStep());
            default -> { }
        }
    }

    /**
     * Validates each length independently, then checks their ordering only when
     * both are specified. A value of -1 means the corresponding limit is absent.
     */
    public static void validateLengths(String location, int min, int max) {
        if (min < -1) fail(location, "minLength", min, "must be -1 or non-negative");
        if (max < -1) fail(location, "maxLength", max, "must be -1 or non-negative");
        if (min >= 0 && max >= 0 && min > max) {
            fail(location, "minLength", min, "must not exceed maxLength " + max);
        }
    }

    public static void validateNumber(String location, double min, double max, double step) {
        if (!Double.isNaN(min) && !Double.isFinite(min)) fail(location, "min", min, "must be finite or NaN");
        if (!Double.isNaN(max) && !Double.isFinite(max)) fail(location, "max", max, "must be finite or NaN");
        if (!Double.isNaN(step) && (!Double.isFinite(step) || step <= 0)) {
            fail(location, "step", step, "must be finite and greater than 0, or NaN");
        }
        if (!Double.isNaN(min) && !Double.isNaN(max) && min > max) {
            fail(location, "min", min, "must not exceed max " + max);
        }
    }

    public static void validateDate(String location, String min, String max, String step) {
        DateValue minDate = parseDate(location, "min", min);
        DateValue maxDate = parseDate(location, "max", max);
        if (minDate != null && maxDate != null && minDate.compareTo(maxDate) > 0) {
            fail(location, "min", min, "must not be later than max " + max);
        }
        if (step != null && !step.isEmpty() && !step.equals("any")) {
            boolean positive = false;
            if (FLOAT.matcher(step).matches()) {
                try {
                    double parsed = Double.parseDouble(step);
                    positive = Double.isFinite(parsed) && parsed > 0;
                } catch (NumberFormatException ignored) {
                    positive = false;
                }
            }
            if (!positive) fail(location, "step", step, "must be empty, 'any', or a finite positive number of days");
        }
    }

    private static DateValue parseDate(String location, String attribute, String value) {
        if (value == null || value.isEmpty()) return null;
        Matcher match = DATE.matcher(value);
        if (!match.matches()) {
            fail(location, attribute, value, "must be a valid HTML date (YYYY-MM-DD)");
        }
        BigInteger year = new BigInteger(match.group(1));
        int month = Integer.parseInt(match.group(2));
        int day = Integer.parseInt(match.group(3));
        boolean leap = year.mod(BigInteger.valueOf(4)).signum() == 0
                && (year.mod(BigInteger.valueOf(100)).signum() != 0
                    || year.mod(BigInteger.valueOf(400)).signum() == 0);
        int days = switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11 -> 30;
            case 2 -> leap ? 29 : 28;
            default -> 0;
        };
        if (year.signum() <= 0 || day < 1 || day > days) {
            fail(location, attribute, value, "must be a valid HTML date (YYYY-MM-DD)");
        }
        return new DateValue(year, month, day);
    }

    private static void fail(String location, String attribute, Object value, String rule) {
        throw new ConstraintValidationException(location + ": " + attribute + " " + rule + "; received " + value);
    }

    private record DateValue(BigInteger year, int month, int day) implements Comparable<DateValue> {
        @Override
        public int compareTo(DateValue other) {
            int yearComparison = year.compareTo(other.year);
            if (yearComparison != 0) return yearComparison;
            int monthComparison = Integer.compare(month, other.month);
            return monthComparison != 0 ? monthComparison : Integer.compare(day, other.day);
        }
    }
}
