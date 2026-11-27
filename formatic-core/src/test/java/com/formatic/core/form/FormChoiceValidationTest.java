package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormChoiceValidationTest {
    private final FormValueValidator validator = new FormValueValidator();

    @Test
    void checksTypedNumericMembershipWithoutStringCoercion() {
        var field = choice(FormDataType.NUMBER);
        field.setOptions(List.of(new SelectRadioOption("1.0", "One")));
        assertThat(validate(field, new BigDecimal("1.00"))).isEmpty();
        assertThat(validate(field, "1.0")).extracting(FormValueViolation::code).containsExactly("formatic.choice");
        assertThat(validate(field, 2)).hasSize(1);
        assertThat(validate(field, Double.NaN)).hasSize(1);
    }

    @Test
    void checksEveryItemIncludingNullsInCollectionsAndArrays() {
        var field = choice(FormDataType.ARRAY);
        field.setMultiple(true);
        field.setItemDataType(FormDataType.INTEGER);
        field.setOptions(List.of(new SelectRadioOption("1", "One"), new SelectRadioOption("2", "Two")));
        assertThat(validate(field, new int[]{1, 2})).isEmpty();
        assertThat(validate(field, Set.of(1, 2))).isEmpty();
        assertThat(validate(field, List.of(1, 3))).hasSize(1);
        assertThat(validate(field, 1)).hasSize(1);
        assertThat(validate(field, Arrays.asList(1, null))).hasSize(1);
        assertThat(validate(field, List.of(1, 1))).isEmpty();
        field.setRequired(true);
        assertThat(validate(field, List.of())).extracting(FormValueViolation::code).containsExactly("formatic.required");
    }

    @Test
    void checksBooleanChoicesWithoutCoercion() {
        var field = choice(FormDataType.BOOLEAN);
        field.setOptions(List.of(new SelectRadioOption("true", "Yes")));
        assertThat(validate(field, true)).isEmpty();
        assertThat(validate(field, false)).hasSize(1);
        assertThat(validate(field, "true")).hasSize(1);
    }

    @Test
    void acceptsKnownStringMappedJavaTypes() {
        var field = choice(FormDataType.STRING);
        var id = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        var date = java.time.LocalDate.of(2024, 2, 29);
        field.setOptions(List.of(new SelectRadioOption(id.toString(), "ID"),
                new SelectRadioOption(date.toString(), "Date")));
        assertThat(validate(field, id)).isEmpty();
        assertThat(validate(field, date)).isEmpty();
        assertThat(validate(field, new Object())).hasSize(1);
    }

    @Test
    void rejectsMalformedOptionsUnknownItemTypesAndMismatchedMultiplicity() {
        var field = choice(FormDataType.INTEGER);
        field.setOptions(List.of(new SelectRadioOption("one", "One")));
        assertThatThrownBy(() -> validate(field, null)).isInstanceOf(IllegalArgumentException.class);
        field.setOptions(List.of(new SelectRadioOption("1", "One")));
        field.setMultiple(true);
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("multiplicity");
        field.setDataType(FormDataType.ARRAY);
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("unknown data type");
    }

    @Test
    void preservesEnumNamesAndOptionalEmptyValueBehavior() {
        var field = choice(FormDataType.STRING);
        field.setType(FormFieldType.RADIO);
        field.setOptions(List.of(new SelectRadioOption("FIRST", "First")));
        assertThat(validate(field, Choice.FIRST)).isEmpty();
        assertThat(validate(field, Choice.SECOND)).hasSize(1);
        assertThat(validate(field, "")).isEmpty();
        assertThat(validate(field, null)).isEmpty();
    }

    @Test
    void configurationErrorsFailBeforeReadingSubmission() {
        var field = choice(FormDataType.STRING);
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("static options");
        field.setOptions(List.of(new SelectRadioOption("a", "A")));
        field.setOptionsProvider("currentUserChoices");
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("dynamic");
        field.setOptionsProvider("");
        field.setOptions(List.of(new SelectRadioOption("a", "A"), new SelectRadioOption("a", "Again")));
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("duplicate");
    }

    private List<FormValueViolation> validate(FormFieldMetadata field, Object value) {
        return validator.validate(List.of(field), path -> value);
    }

    private static FormFieldMetadata choice(FormDataType type) {
        var field = new FormFieldMetadata("choice", FormFieldType.SELECT, "Choice", 0);
        field.setDataType(type);
        return field;
    }

    enum Choice { FIRST, SECOND }
}
