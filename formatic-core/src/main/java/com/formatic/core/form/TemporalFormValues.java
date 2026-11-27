package com.formatic.core.form;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

/** Calendar and local-clock comparisons without introducing timezone conversions. */
final class TemporalFormValues {
    private static final Pattern TIME = Pattern.compile(
            "(?:[01][0-9]|2[0-3]):[0-5][0-9](?::[0-5][0-9](?:\\.[0-9]{1,3})?)?");

    private TemporalFormValues() {}

    static boolean supports(FormFieldType type) {
        return type == FormFieldType.DATE || type == FormFieldType.TIME || type == FormFieldType.DATETIME_LOCAL;
    }

    static void validateConfiguration(FormFieldMetadata field) {
        if ((hasText(field.getMinDate()) || hasText(field.getMaxDate()) || hasText(field.getDateStep()))
                && field.getType() != FormFieldType.DATE
                || (hasText(field.getMinTime()) || hasText(field.getMaxTime()) || hasText(field.getTimeStep()))
                && field.getType() != FormFieldType.TIME
                || (hasText(field.getMinDateTime()) || hasText(field.getMaxDateTime()) || hasText(field.getDateTimeStep()))
                && field.getType() != FormFieldType.DATETIME_LOCAL) {
            throw invalid(field, "temporal bounds/step do not match the control type");
        }
        if (!supports(field.getType())) {
            return;
        }
        Parsed min = bound(field, minimum(field));
        Parsed max = bound(field, maximum(field));
        if (field.getType() != FormFieldType.TIME && min != null && max != null && min.compareTo(max) > 0) {
            throw invalid(field, "temporal min must not exceed max");
        }
        String step = step(field);
        if (hasText(step) && !step.equals("any")) {
            stepBase(field);
        }
    }

    /** Returns a rule code, or null when the nonempty value satisfies its temporal constraints. */
    static String violation(FormFieldMetadata field, Object value) {
        Parsed parsed;
        try {
            parsed = parse(field.getType(), value);
        } catch (IllegalArgumentException | DateTimeException exception) {
            return "temporal";
        }
        Parsed min = bound(field, minimum(field));
        Parsed max = bound(field, maximum(field));
        if (field.getType() == FormFieldType.TIME && min != null && max != null && min.compareTo(max) > 0) {
            if (parsed.compareTo(min) < 0 && parsed.compareTo(max) > 0) {
                return "timeRange";
            }
        } else {
            if (min != null && parsed.compareTo(min) < 0) {
                return "temporalMin";
            }
            if (max != null && parsed.compareTo(max) > 0) {
                return "temporalMax";
            }
        }
        String step = step(field);
        return hasText(step) && !step.equals("any")
                && coordinate(parsed).subtract(stepBase(field)).remainder(new BigDecimal(step)).signum() != 0
                ? "temporalStep" : null;
    }

    private static BigDecimal stepBase(FormFieldMetadata field) {
        Parsed min = bound(field, minimum(field));
        if (min != null) {
            return coordinate(min);
        }
        if (hasText(field.getDefaultValue())) {
            try {
                return coordinate(parse(field.getType(), field.getDefaultValue()));
            } catch (IllegalArgumentException | DateTimeException exception) {
                throw invalid(field, "invalid temporal step base default");
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal coordinate(Parsed parsed) {
        BigInteger days = BigInteger.ZERO;
        if (parsed.date() != null) {
            var date = parsed.date();
            BigInteger previousYear = date.year().subtract(BigInteger.ONE);
            // Proleptic Gregorian days before this year, relative to 1970-01-01.
            days = previousYear.multiply(BigInteger.valueOf(365))
                    .add(previousYear.divide(BigInteger.valueOf(4)))
                    .subtract(previousYear.divide(BigInteger.valueOf(100)))
                    .add(previousYear.divide(BigInteger.valueOf(400)));
            int dayOfYear = java.time.Month.of(date.month()).firstDayOfYear(
                    date.year().mod(BigInteger.valueOf(4)).signum() == 0
                            && (date.year().mod(BigInteger.valueOf(100)).signum() != 0
                            || date.year().mod(BigInteger.valueOf(400)).signum() == 0)) + date.day() - 2;
            days = days.add(BigInteger.valueOf(dayOfYear)).subtract(BigInteger.valueOf(719162));
        }
        if (parsed.time() == null) {
            return new BigDecimal(days);
        }
        return new BigDecimal(days).multiply(BigDecimal.valueOf(86400))
                .add(BigDecimal.valueOf(parsed.time().toNanoOfDay() / 1_000_000, 3));
    }

    private static String step(FormFieldMetadata field) {
        return switch (field.getType()) {
            case DATE -> field.getDateStep();
            case TIME -> field.getTimeStep();
            case DATETIME_LOCAL -> field.getDateTimeStep();
            default -> null;
        };
    }

    private static Parsed bound(FormFieldMetadata field, String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return parse(field.getType(), value);
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw invalid(field, "invalid temporal bound for " + field.getType());
        }
    }

    private static Parsed parse(FormFieldType type, Object value) {
        if (type == FormFieldType.DATE && value instanceof LocalDate date) {
            return new Parsed(date(date), null);
        }
        if (type == FormFieldType.TIME && value instanceof LocalTime time) {
            return new Parsed(null, time(time));
        }
        if (type == FormFieldType.DATETIME_LOCAL && value instanceof LocalDateTime dateTime) {
            return new Parsed(date(dateTime.toLocalDate()), time(dateTime.toLocalTime()));
        }
        if (!(value instanceof CharSequence sequence)) {
            throw new IllegalArgumentException("Expected a matching local temporal type or string");
        }
        String text = sequence.toString();
        return switch (type) {
            case DATE -> new Parsed(FormConstraintValidator.parseDate("value", "date", text), null);
            case TIME -> new Parsed(null, parseTime(text));
            case DATETIME_LOCAL -> {
                int separator = Math.max(text.indexOf('T'), text.indexOf(' '));
                if (separator <= 0) {
                    throw new IllegalArgumentException("Missing local date-time separator");
                }
                yield new Parsed(FormConstraintValidator.parseDate("value", "date", text.substring(0, separator)),
                        parseTime(text.substring(separator + 1)));
            }
            default -> throw new IllegalArgumentException("Not a temporal control");
        };
    }

    private static FormConstraintValidator.DateValue date(LocalDate date) {
        if (date.getYear() <= 0) {
            throw new IllegalArgumentException("HTML dates require a positive year");
        }
        return new FormConstraintValidator.DateValue(BigInteger.valueOf(date.getYear()),
                date.getMonthValue(), date.getDayOfMonth());
    }

    private static LocalTime parseTime(String value) {
        if (!TIME.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid HTML local time");
        }
        return LocalTime.parse(value);
    }

    private static LocalTime time(LocalTime value) {
        if (value.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("HTML local time supports millisecond precision");
        }
        return value;
    }

    private static String minimum(FormFieldMetadata field) {
        return switch (field.getType()) {
            case DATE -> field.getMinDate();
            case TIME -> field.getMinTime();
            case DATETIME_LOCAL -> field.getMinDateTime();
            default -> null;
        };
    }

    private static String maximum(FormFieldMetadata field) {
        return switch (field.getType()) {
            case DATE -> field.getMaxDate();
            case TIME -> field.getMaxTime();
            case DATETIME_LOCAL -> field.getMaxDateTime();
            default -> null;
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static IllegalArgumentException invalid(FormFieldMetadata field, String message) {
        return new IllegalArgumentException(field.getName() + ": " + message);
    }

    private record Parsed(FormConstraintValidator.DateValue date, LocalTime time) implements Comparable<Parsed> {
        @Override
        public int compareTo(Parsed other) {
            int dates = date == null ? 0 : date.compareTo(other.date);
            return dates != 0 || time == null ? dates : time.compareTo(other.time);
        }
    }
}
