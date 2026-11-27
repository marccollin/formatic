package com.formatic.validation.jakarta;

import com.formatic.core.annotation.*;
import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormaticValidatedTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void attachesFailuresToActualNestedPropertiesAndAliasesDoNotBreakAccess() {
        var dto = new Customer();
        var violations = validator.validate(dto);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "address.street", "accepted");
        assertThat(violations).extracting(v -> v.getMessageTemplate()).contains("{formatic.required}");
        assertThat(violations).allSatisfy(v -> assertThat(v.getMessage()).doesNotStartWith("{"));
        dto.name = "Valid";
        dto.address.street = " ";
        dto.accepted = true;
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void appliesLengthsBoundsAndRereadsChangedValues() {
        var dto = new Customer();
        dto.name = "x";
        dto.address.street = "Main";
        dto.accepted = true;
        dto.amount = new BigDecimal("-0.00000000000000001");
        assertThat(validator.validate(dto)).extracting(v -> v.getMessageTemplate())
                .containsExactlyInAnyOrder("{formatic.minLength}", "{formatic.min}");
        dto.name = "Valid";
        dto.amount = BigDecimal.ZERO;
        assertThat(validator.validate(dto)).isEmpty();
        dto.amount = BigDecimal.valueOf(101);
        assertThat(validator.validate(dto)).extracting(v -> v.getMessageTemplate()).containsExactly("{formatic.max}");
    }

    @Test
    void requiredNestedLeavesFailWhenParentIsNull() {
        var dto = new Customer();
        dto.address = null;
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .contains("address.street");
    }

    @Test
    void unsupportedRulesAreConfigurationErrorsEvenWhenValuesAreEmpty() {
        assertThatThrownBy(() -> validator.validate(new Unsupported()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("pattern");
        assertThatThrownBy(() -> validator.validate(new UnsupportedControl()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("FileInput");
    }

    @Test
    void supportsStandardValidationGroupsAndLeavesUnmarkedDtosAlone() {
        assertThat(validator.validate(new Grouped())).isEmpty();
        assertThat(validator.validate(new Grouped(), OnCreate.class)).hasSize(1);
        assertThat(validator.validate(new Unmarked())).isEmpty();
    }

    @Test
    void fieldMessageOverridesAreLiteralAndNestedMarkersAreRejected() {
        assertThat(validator.validate(new CustomMessage())).extracting(v -> v.getMessage())
                .containsExactly("Literal {value} ${1+1}");
        assertThatThrownBy(() -> validator.validate(new DuplicateRoot()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("owned by the root");
    }

    interface OnCreate {}

    @Test
    void patternViolationsUseNestedPropertyPathsMessagesAndCustomOverrides() {
        var root = new PatternRoot();
        root.child.code = "A1";
        root.child.phone = "abc";
        assertThat(validator.validate(root)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("child.code", "child.phone");
        assertThat(validator.validate(root)).extracting(v -> v.getMessage())
                .containsExactlyInAnyOrder("Must match the configured pattern.", "Literal {phone} ${1+1}");
        root.child.code = "A12";
        root.child.phone = "123";
        assertThat(validator.validate(root)).isEmpty();
        root.child.code = "";
        root.child.phone = null;
        assertThat(validator.validate(root)).isEmpty();
    }

    @FormaticValidated
    static class PatternRoot {
        @NestedForm private PatternChild child = new PatternChild();
    }

    static class PatternChild {
        @TextInput(name = "alias", pattern = "[A-Z][0-9]{2}") private String code;
        @PhoneInput(pattern = "[0-9]+", errorMessage = "Literal {phone} ${1+1}") private String phone;
    }

    @Test
    void stringFormatsAndPhoneLengthsProduceInterpolatedFieldErrors() {
        var dto = new StringControls();
        dto.email = "not-an-email";
        dto.url = "/relative";
        dto.phone = "1";
        dto.color = "red";
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("email", "url", "phone", "color");
        assertThat(validator.validate(dto)).extracting(v -> v.getMessageTemplate())
                .containsExactlyInAnyOrder("{formatic.email}", "{formatic.url}",
                        "{formatic.minLength}", "{formatic.color}");
        assertThat(validator.validate(dto)).allSatisfy(v -> assertThat(v.getMessage()).doesNotStartWith("{"));
        dto.email = "user@localhost";
        dto.url = "mailto:user@localhost";
        dto.phone = "call reception";
        dto.color = "#aB12fF";
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void stringDeclarationsRejectUnsupportedTypesAlphaAndPatternsEvenWithoutValues() {
        assertThatThrownBy(() -> validator.validate(new WrongEmailType()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("CharSequence");
        assertThatThrownBy(() -> validator.validate(new AlphaColor()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("alpha color");
        assertThatThrownBy(() -> validator.validate(new PatternedPhone()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("pattern");
        assertThatThrownBy(() -> validator.validate(new InvalidEmailLength()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("minLength");
        assertThat(validator.validate(new CustomEmailMessage())).extracting(v -> v.getMessage())
                .containsExactly("Literal {email} ${1+1}");
    }

    @FormaticValidated
    static class StringControls {
        @EmailInput private String email;
        @UrlInput private String url;
        @PhoneInput(minLength = 2) private String phone;
        @ColorInput private String color;
    }

    @FormaticValidated
    static class WrongEmailType {
        @EmailInput private Integer email;
    }

    @FormaticValidated
    static class AlphaColor {
        @ColorInput(alpha = true) private String color;
    }

    @FormaticValidated
    static class PatternedPhone {
        @PhoneInput(pattern = "(?=1)") private String phone;
    }

    @FormaticValidated
    static class InvalidEmailLength {
        @EmailInput(minLength = 5, maxLength = 2) private String email;
    }

    @FormaticValidated
    static class CustomEmailMessage {
        @EmailInput(errorMessage = "Literal {email} ${1+1}") private String email = "invalid";
    }

    @Test
    void staticChoicesValidateTypedValuesAndEverySelection() {
        var dto = new Choices();
        dto.count = 3;
        dto.payment = "forged";
        dto.interests = java.util.List.of("java", "forged");
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("count", "payment", "interests");
        dto.count = 1;
        dto.payment = "cash";
        dto.interests = java.util.List.of("java");
        assertThat(validator.validate(dto)).isEmpty();
        dto.interests = java.util.List.of();
        assertThat(validator.validate(dto)).extracting(v -> v.getMessageTemplate())
                .containsExactly("{formatic.required}");
    }

    @Test
    void dynamicChoiceProvidersAreRejectedWithoutInvocation() {
        assertThatThrownBy(() -> validator.validate(new DynamicChoice()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("dynamic optionsProvider");
    }

    @FormaticValidated
    static class Choices {
        @SelectInput(options = {"1:One", "2:Two"}) private Integer count;
        @RadioInput(options = {"cash:Cash", "card:Card"}) private String payment;
        @CheckboxInput(required = true, options = {"java:Java", "web:Web"})
        private java.util.List<String> interests;
    }

    @FormaticValidated
    static class DynamicChoice {
        @SelectInput(optionsProvider = "currentUserChoices") private String choice;
    }

    @Test
    void numericStepsProduceLocalizedPropertyViolationsForNumberAndRange() {
        var dto = new Stepped();
        dto.amount = new BigDecimal("0.15");
        dto.rating = new BigDecimal("0.08");
        assertThat(validator.validate(dto)).isEmpty();
        dto.amount = new BigDecimal("0.1");
        dto.rating = new BigDecimal("0.15");
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("amount", "rating");
        assertThat(validator.validate(dto)).allSatisfy(v -> {
            assertThat(v.getMessageTemplate()).isEqualTo("{formatic.step}");
            assertThat(v.getMessage()).isEqualTo("Must align with the configured numeric step.");
        });
    }

    @FormaticValidated
    static class Stepped {
        @NumberInput(step = 0.1, defaultValue = "0.05") private BigDecimal amount;
        @RangeInput(min = -0.02, step = 0.1) private BigDecimal rating;
    }

    @Test
    void temporalConstraintsUseLocalTypesAndNestedPropertyPaths() {
        var dto = new TemporalRoot();
        dto.appointment.date = java.time.LocalDate.of(2023, 12, 31);
        dto.appointment.time = java.time.LocalTime.NOON;
        dto.appointment.local = "2024-02-30T12:00";
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("appointment.date", "appointment.time", "appointment.local");
        assertThat(validator.validate(dto)).extracting(v -> v.getMessageTemplate())
                .containsExactlyInAnyOrder("{formatic.temporalMin}", "{formatic.timeRange}", "{formatic.temporal}");
        dto.appointment.date = java.time.LocalDate.of(2024, 2, 29);
        dto.appointment.time = java.time.LocalTime.MIDNIGHT;
        dto.appointment.local = "2024-02-29T12:00";
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void temporalDeclarationsRejectWrongJavaTypesAndInvalidStepBases() {
        assertThatThrownBy(() -> validator.validate(new UnsupportedTemporalType()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("LocalDate");
        assertThatThrownBy(() -> validator.validate(new InvalidTemporalStepBase()))
                .isInstanceOf(ConstraintDeclarationException.class).hasMessageContaining("step base");
    }

    @FormaticValidated
    static class TemporalRoot {
        @NestedForm private Appointment appointment = new Appointment();
    }

    static class Appointment {
        @DateInput(required = true, min = "2024-01-01", max = "2024-12-31")
        private java.time.LocalDate date;
        @TimeInput(min = "22:00", max = "06:00")
        private java.time.LocalTime time;
        @DateTimeInput(min = "2024-01-01T00:00", max = "2024-12-31T23:59")
        private String local;
    }

    @FormaticValidated
    static class UnsupportedTemporalType {
        @DateInput private java.time.Instant date;
    }

    @FormaticValidated
    static class InvalidTemporalStepBase {
        @TimeInput(step = "60", defaultValue = "invalid") private String time;
    }

    @Test
    void temporalStepsProduceInterpolatedNestedFieldViolations() {
        var dto = new TemporalSteppedRoot();
        dto.appointment.date = java.time.LocalDate.of(1970, 1, 2);
        dto.appointment.time = "09:30:30";
        dto.appointment.local = java.time.LocalDateTime.of(2024, 3, 1, 0, 1);
        assertThat(validator.validate(dto)).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("appointment.date", "appointment.time", "appointment.local");
        assertThat(validator.validate(dto)).allSatisfy(v -> {
            assertThat(v.getMessageTemplate()).isEqualTo("{formatic.temporalStep}");
            assertThat(v.getMessage()).isEqualTo("Must align with the configured temporal step.");
        });
        dto.appointment.date = java.time.LocalDate.of(1970, 1, 3);
        dto.appointment.time = "09:31";
        dto.appointment.local = java.time.LocalDateTime.of(2024, 3, 1, 0, 0);
        assertThat(validator.validate(dto)).isEmpty();
    }

    @FormaticValidated
    static class TemporalSteppedRoot {
        @NestedForm private TemporalStepped appointment = new TemporalStepped();
    }

    static class TemporalStepped {
        @DateInput(step = "2") private java.time.LocalDate date;
        @TimeInput(step = "60", defaultValue = "09:30") private String time;
        @DateTimeInput(step = "120", min = "2024-02-29T23:58") private java.time.LocalDateTime local;
    }

    @FormaticValidated
    static class Customer {
        @TextInput(name = "customerName", required = true, minLength = 2, maxLength = 10)
        private String name;
        @NestedForm @Valid private Address address = new Address();
        @CheckboxInput(required = true) private boolean accepted;
        @NumberInput(min = 0, max = 100) private BigDecimal amount;
    }

    static class Address {
        @TextInput(required = true) private String street;
    }

    @FormaticValidated
    static class Unsupported {
        @TextInput(pattern = "(?=a)") private String name;
    }

    @FormaticValidated
    static class UnsupportedControl {
        @FileInput private String file;
    }

    @FormaticValidated(groups = OnCreate.class)
    static class Grouped {
        @TextInput(required = true) private String name;
    }

    static class Unmarked {
        @TextInput(required = true) private String name;
    }

    @FormaticValidated
    static class CustomMessage {
        @TextInput(required = true, errorMessage = "Literal {value} ${1+1}") private String name;
    }

    @FormaticValidated
    static class DuplicateRoot {
        @NestedForm @Valid private Grouped child;
    }
}
