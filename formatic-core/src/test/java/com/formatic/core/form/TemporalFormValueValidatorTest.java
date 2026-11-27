package com.formatic.core.form;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemporalFormValueValidatorTest {
    private final FormValueValidator validator = new FormValueValidator();

    @ParameterizedTest
    @CsvSource({
            "DATE, 2024-02-29, 2023-02-29",
            "DATE, 2000-02-29, 1900-02-29",
            "DATE, 2024-04-30, 2024-04-31",
            "DATE, 10000-01-01, 0000-01-01",
            "DATE, 999999999999-01-01, -001-01-01",
            "TIME, 23:59, 24:00",
            "TIME, 09:30:59.123, 09:30:60",
            "TIME, 00:00:00.1, 00:00Z",
            "TIME, 00:00:00.123, 00:00:00.1234",
            "DATETIME_LOCAL, 2024-02-29T23:59, 2023-02-29T23:59",
            "DATETIME_LOCAL, 2024-02-29 23:59, 2024-02-29T24:00",
            "DATETIME_LOCAL, 2024-01-01T00:00, 2024-01-01T00:00Z"
    })
    void parsesCalendarAndClockValuesWithoutTimezoneConversion(FormFieldType type, String valid, String invalid) {
        var field = field(type);
        assertThat(validate(field, valid)).isEmpty();
        assertThat(validate(field, invalid)).extracting(FormValueViolation::code).containsExactly("formatic.temporal");
        assertThat(validate(field, valid + "\n")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporal");
    }

    @Test
    void dateBoundsAreInclusiveForStringsAndLocalDatesAndHaveNoIntegerYearLimit() {
        var field = field(FormFieldType.DATE);
        field.setMinDate("2024-02-29");
        field.setMaxDate("2024-03-01");
        assertThat(validate(field, LocalDate.of(2024, 2, 29))).isEmpty();
        assertThat(validate(field, "2024-03-01")).isEmpty();
        assertThat(validate(field, "2024-02-28")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMin");
        assertThat(validate(field, LocalDate.of(2024, 3, 2))).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMax");
        field.setMinDate("999999999999-01-01");
        field.setMaxDate("999999999999-12-31");
        assertThat(validate(field, "999999999999-06-01")).isEmpty();
    }

    @Test
    void timeBoundsWrapAcrossMidnightAndIncludeBothEndpoints() {
        var field = field(FormFieldType.TIME);
        field.setMinTime("22:00");
        field.setMaxTime("06:00");
        for (var value : List.of(LocalTime.of(22, 0), LocalTime.MIDNIGHT, LocalTime.of(6, 0))) {
            assertThat(validate(field, value)).isEmpty();
        }
        assertThat(validate(field, "12:00")).extracting(FormValueViolation::code)
                .containsExactly("formatic.timeRange");
        field.setMinTime("06:00");
        field.setMaxTime("22:00");
        assertThat(validate(field, "05:59")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMin");
        assertThat(validate(field, "22:01")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMax");
        field.setMaxTime("06:00");
        assertThat(validate(field, "06:00")).isEmpty();
        assertThat(validate(field, "06:00:00.001")).hasSize(1);
    }

    @Test
    void localDateTimeBoundsCompareDatesBeforeClocks() {
        var field = field(FormFieldType.DATETIME_LOCAL);
        field.setMinDateTime("2024-02-29T22:00");
        field.setMaxDateTime("2024-03-01T06:00");
        assertThat(validate(field, LocalDateTime.of(2024, 3, 1, 0, 0))).isEmpty();
        assertThat(validate(field, "2024-02-29T22:00")).isEmpty();
        assertThat(validate(field, "2024-03-01T06:00")).isEmpty();
        assertThat(validate(field, "2024-02-29T21:59")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMin");
        assertThat(validate(field, "2024-03-01T06:00:00.001")).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporalMax");
    }

    @Test
    void requiredOptionalAndInvalidTypedValuesAreHandledWithoutCoercion() {
        var field = field(FormFieldType.DATE);
        assertThat(validate(field, null)).isEmpty();
        assertThat(validate(field, "")).isEmpty();
        field.setRequired(true);
        assertThat(validate(field, null)).extracting(FormValueViolation::code).containsExactly("formatic.required");
        assertThat(validate(field, LocalDate.of(0, 1, 1))).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporal");
        assertThat(validate(field, LocalTime.NOON)).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporal");
        field.setType(FormFieldType.TIME);
        assertThat(validate(field, LocalTime.of(12, 0, 0, 1))).extracting(FormValueViolation::code)
                .containsExactly("formatic.temporal");
    }

    @Test
    void unsupportedOrMalformedConfigurationFailsBeforeReadingOptionalValues() {
        var field = field(FormFieldType.DATE);
        field.setMinDate("2023-02-29");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("min");
        field.setMinDate("2024-01-01");
        field.setMaxDate("2023-01-01");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("min");
        field.setMaxDate("");
        field.setDateStep("any");
        assertThat(validate(field, null)).isEmpty();
        field.setDateStep("");
        field.setMinTime("00:00");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("do not match");
        field.setMinTime("");
        field.setType(FormFieldType.TIME);
        field.setMinDate("");
        field.setMinTime("00:00:00.1234");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("temporal bound");
    }

    @Test
    void dateStepsUseMinThenDefaultThenEpochInDays() {
        var field = field(FormFieldType.DATE);
        field.setDateStep("2");
        assertThat(validate(field, "1970-01-01")).isEmpty();
        assertStepMismatch(field, "1970-01-02");
        assertThat(validate(field, LocalDate.of(1969, 12, 30))).isEmpty();
        field.setDefaultValue("2024-02-28");
        assertThat(validate(field, LocalDate.of(2024, 3, 1))).isEmpty();
        assertStepMismatch(field, "2024-02-29");
        field.setMinDate("2024-02-29");
        field.setDefaultValue("invalid");
        assertThat(validate(field, "2024-03-02")).isEmpty();
        assertStepMismatch(field, "2024-03-01");
        field.setDateStep("1.5");
        assertThat(validate(field, "2024-03-03")).isEmpty();
        assertStepMismatch(field, "2024-03-02");
    }

    @Test
    void epochDayArithmeticMatchesJavaAcrossGregorianBoundaries() {
        var field = field(FormFieldType.DATE);
        field.setDateStep("7");
        var epoch = LocalDate.of(1970, 1, 1);
        for (int year : List.of(1, 4, 100, 400, 1600, 1900, 1969, 1970, 2000, 2024, 10000)) {
            for (int month = 1; month <= 12; month++) {
                var date = LocalDate.of(year, month, 1);
                assertThat(validate(field, date).isEmpty()).as(date.toString())
                        .isEqualTo(ChronoUnit.DAYS.between(epoch, date) % 7 == 0);
            }
        }
        field.setMinDate("999999999999-02-28");
        field.setDateStep("2");
        assertThat(validate(field, "999999999999-03-02")).isEmpty();
        assertStepMismatch(field, "999999999999-03-01");
    }

    @Test
    void timeStepsUseDecimalSecondsWithMillisecondPrecision() {
        var field = field(FormFieldType.TIME);
        field.setTimeStep("0.1");
        assertThat(validate(field, "00:00:00.3")).isEmpty();
        assertStepMismatch(field, "00:00:00.301");
        field.setDefaultValue("00:00:00.025");
        assertThat(validate(field, LocalTime.of(0, 0, 0, 325_000_000))).isEmpty();
        assertStepMismatch(field, "00:00:00.3");
        field.setMinTime("00:00:00.050");
        field.setDefaultValue("invalid");
        assertThat(validate(field, "00:00:00.350")).isEmpty();
        assertStepMismatch(field, "00:00:00.325");
    }

    @Test
    void overnightRangesStillApplyStepsUsingPlainClockDifference() {
        var field = field(FormFieldType.TIME);
        field.setMinTime("22:00");
        field.setMaxTime("06:00");
        field.setTimeStep("18000");
        assertThat(validate(field, "02:00")).isEmpty();
        assertStepMismatch(field, "00:00");
        assertThat(validate(field, "12:00")).extracting(FormValueViolation::code)
                .containsExactly("formatic.timeRange");
    }

    @Test
    void localDateTimeStepsCrossDatesWithoutTimezoneOrFloatingPointConversion() {
        var field = field(FormFieldType.DATETIME_LOCAL);
        field.setDateTimeStep("0.1");
        field.setDefaultValue("2024-02-29T23:59:59.950");
        assertThat(validate(field, LocalDateTime.of(2024, 3, 1, 0, 0, 0, 50_000_000))).isEmpty();
        assertStepMismatch(field, "2024-03-01T00:00:00.051");
        field.setDefaultValue("");
        field.setDateTimeStep("86400");
        assertThat(validate(field, "1969-12-31T00:00")).isEmpty();
        assertStepMismatch(field, "1970-01-01T00:01");
        assertThat(validate(field, "999999999999-03-01T00:00")).isEmpty();
    }

    @Test
    void anyAndAbsentStepsSkipAlignmentButNotParsingAndBounds() {
        var field = field(FormFieldType.TIME);
        assertThat(validate(field, "12:00:00.123")).isEmpty();
        field.setTimeStep("any");
        field.setDefaultValue("invalid");
        assertThat(validate(field, "12:00:00.123")).isEmpty();
        assertThat(validate(field, "24:00")).extracting(FormValueViolation::code).containsExactly("formatic.temporal");
        field.setMinTime("13:00");
        assertThat(validate(field, "12:00")).extracting(FormValueViolation::code).containsExactly("formatic.temporalMin");
    }

    @Test
    void malformedStepsAndActiveStepBasesFailForNullSubmissions() {
        for (var type : List.of(FormFieldType.DATE, FormFieldType.TIME, FormFieldType.DATETIME_LOCAL)) {
            for (String step : List.of("0", "-1", "NaN", "Infinity", "1e309", "no", " 1", "ANY")) {
                var field = field(type);
                switch (type) {
                    case DATE -> field.setDateStep(step);
                    case TIME -> field.setTimeStep(step);
                    case DATETIME_LOCAL -> field.setDateTimeStep(step);
                    default -> throw new AssertionError(type);
                }
                assertThatThrownBy(() -> validate(field, null)).as(type + ": " + step)
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("step");
            }
        }
        var field = field(FormFieldType.DATE);
        field.setDateStep("2");
        field.setDefaultValue("2023-02-29");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("step base");
        field.setType(FormFieldType.TIME);
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("do not match");
    }

    private void assertStepMismatch(FormFieldMetadata field, Object value) {
        assertThat(validate(field, value)).extracting(FormValueViolation::code).containsExactly("formatic.temporalStep");
    }

    private List<FormValueViolation> validate(FormFieldMetadata field, Object value) {
        return validator.validate(List.of(field), path -> value);
    }

    private static FormFieldMetadata field(FormFieldType type) {
        return new FormFieldMetadata("appointment", type, "Appointment", 0);
    }
}
