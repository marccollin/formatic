package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormValueValidatorTest {
    private final FormValueValidator validator = new FormValueValidator();

    @Test
    void requiredUsesHtmlSemanticsAndOptionalEmptyValuesSkipBounds() {
        var text = field("name", FormFieldType.TEXT);
        text.setRequired(true);
        text.setMinLength(2);
        var number = field("amount", FormFieldType.NUMBER);
        number.setMin(1);
        assertThat(validator.validate(List.of(text, number), path -> path.equals("name") ? "  " : null)).isEmpty();
        assertThat(validator.validate(List.of(text), path -> "")).extracting(FormValueViolation::code)
                .containsExactly("formatic.required");
        text.setRequired(false);
        assertThat(validator.validate(List.of(text), path -> "")).isEmpty();
    }

    @Test
    void requiredCheckboxAndCollectionsAreCheckedAndNonempty() {
        var checkbox = field("accepted", FormFieldType.CHECKBOX);
        checkbox.setDataType(FormDataType.BOOLEAN);
        checkbox.setRequired(true);
        assertThat(validator.validate(List.of(checkbox), path -> false)).hasSize(1);
        assertThat(validator.validate(List.of(checkbox), path -> true)).isEmpty();
        checkbox.setMultiple(true);
        checkbox.setDataType(FormDataType.ARRAY);
        checkbox.setItemDataType(FormDataType.STRING);
        checkbox.setOptions(List.of(new SelectRadioOption("value", "Value")));
        assertThat(validator.validate(List.of(checkbox), path -> List.of())).hasSize(1);
        assertThat(validator.validate(List.of(checkbox), path -> new String[0])).hasSize(1);
        assertThat(validator.validate(List.of(checkbox), path -> List.of("value"))).isEmpty();
    }

    @Test
    void presentationFlagsDoNotBypassRulesAndOverridesDoNotExposeValues() {
        var text = field("name", FormFieldType.TEXT);
        text.setDisabled(true);
        text.setReadonly(true);
        text.setMaxLength(1);
        text.setErrorMessage("Too long.");
        var result = validator.validate(List.of(text), path -> "private");
        assertThat(result).containsExactly(new FormValueViolation("name", "formatic.maxLength", "Too long."));
    }

    @Test
    void handlesNonfiniteNumbersAndInvalidConfigurationExplicitly() {
        var number = field("amount", FormFieldType.NUMBER);
        assertThat(validator.validate(List.of(number), path -> Double.NaN)).extracting(FormValueViolation::code)
                .containsExactly("formatic.number");
        number.setStep(0);
        assertThatThrownBy(() -> validator.validate(List.of(number), path -> null))
                .hasMessageContaining("step");
        number.setStep(Double.NaN);
        assertThatThrownBy(() -> validator.validate(List.of(number), path -> "12"))
                .hasMessageContaining("Number value");
    }

    @Test
    void numericStepUsesMinThenDefaultThenZeroWithoutFloatingPointRounding() {
        var number = field("amount", FormFieldType.NUMBER);
        number.setStep(0.1);
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.3"))).isEmpty();
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.30000000000000001")))
                .extracting(FormValueViolation::code).containsExactly("formatic.step");
        number.setDefaultValue("0.05");
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.15"))).isEmpty();
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.1"))).hasSize(1);
        number.setMin(-0.02);
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.08"))).isEmpty();
        assertThat(validator.validate(List.of(number), path -> new java.math.BigDecimal("0.15")))
                .extracting(FormValueViolation::code).containsExactly("formatic.step");
    }

    @Test
    void numericStepChecksConfigurationEvenForOptionalNullValues() {
        var number = field("amount", FormFieldType.NUMBER);
        number.setStep(1);
        number.setDefaultValue("NaN");
        assertThatThrownBy(() -> validator.validate(List.of(number), path -> null))
                .hasMessageContaining("step base");
        number.setMin(0);
        assertThat(validator.validate(List.of(number), path -> null)).isEmpty();
        var text = field("name", FormFieldType.TEXT);
        text.setStep(1);
        assertThatThrownBy(() -> validator.validate(List.of(text), path -> null))
                .hasMessageContaining("step requires a numeric control");
    }

    @Test
    void countsHtmlUtf16CodeUnitsAndNeverCachesSubmissionValues() {
        var text = field("name", FormFieldType.TEXT);
        text.setMaxLength(1);
        assertThat(validator.validate(List.of(text), Map.of("name", "\uD83D\uDE00")::get)).hasSize(1);
        assertThat(validator.validate(List.of(text), Map.of("name", "a")::get)).isEmpty();
    }

    private static FormFieldMetadata field(String name, FormFieldType type) {
        return new FormFieldMetadata(name, type, name, 0);
    }
}
