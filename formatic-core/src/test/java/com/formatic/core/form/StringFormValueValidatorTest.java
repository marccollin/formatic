package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringFormValueValidatorTest {
    private final FormValueValidator validator = new FormValueValidator();

    @Test
    void emailUsesHtmlSingleAddressSyntax() {
        var field = field(FormFieldType.EMAIL);
        for (String value : List.of("user@example.com", "user+tag@example.com", "user@localhost",
                "a@xn--bcher-kva.example", "a@" + "x".repeat(63) + ".com")) {
            assertThat(validate(field, value)).as(value).isEmpty();
        }
        for (String value : List.of("user", "@example.com", "a@-host.com", "a@host-.com",
                "a@" + "x".repeat(64) + ".com", "a@example..com", "a@example.com,b@example.com",
                " a@example.com", "a@example.com\n", "a@bücher.example", "\"a b\"@example.com")) {
            assertThat(validate(field, value)).as(value).extracting(FormValueViolation::code)
                    .containsExactly("formatic.email");
        }
    }

    @Test
    void urlRequiresAnAbsoluteUriButNotHttpOrANetworkLookup() {
        var field = field(FormFieldType.URL);
        for (String value : List.of("https://example.com/a?b=c#d", "mailto:a@example.com",
                "urn:isbn:123", "https://example.com/%20")) {
            assertThat(validate(field, value)).as(value).isEmpty();
        }
        for (String value : List.of("/relative", "//example.com", "example.com", "https://",
                "https://example.com/a b", "https://example.com/%zz", " https://example.com")) {
            assertThat(validate(field, value)).as(value).extracting(FormValueViolation::code)
                    .containsExactly("formatic.url");
        }
    }

    @Test
    void colorAcceptsOnlySixHexDigitsAndDoesNotSanitize() {
        var field = field(FormFieldType.COLOR);
        for (String value : List.of("#000000", "#aB12fF", "#FFFFFF")) {
            assertThat(validate(field, value)).as(value).isEmpty();
        }
        for (String value : List.of("#fff", "red", "#12345678", "123456", "#gg0000", " #123456")) {
            assertThat(validate(field, value)).as(value).extracting(FormValueViolation::code)
                    .containsExactly("formatic.color");
        }
        field.setAlpha(true);
        assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("alpha color");
    }

    @Test
    void phoneHasNoImplicitFormatButChecksExplicitLengths() {
        var field = field(FormFieldType.PHONE);
        assertThat(validate(field, "call reception")).isEmpty();
        assertThat(validate(field, "+1 (555) 010-1234 ext. 2")).isEmpty();
        field.setMinLength(2);
        field.setMaxLength(3);
        assertThat(validate(field, "x")).extracting(FormValueViolation::code).containsExactly("formatic.minLength");
        assertThat(validate(field, "\uD83D\uDE00")).isEmpty();
        assertThat(validate(field, "1234")).extracting(FormValueViolation::code).containsExactly("formatic.maxLength");
    }

    @Test
    void appliesRequiredOptionalLengthAndTypeRulesAcrossStringControls() {
        for (var type : List.of(FormFieldType.EMAIL, FormFieldType.URL, FormFieldType.PHONE, FormFieldType.COLOR)) {
            var field = field(type);
            assertThat(validate(field, null)).isEmpty();
            assertThat(validate(field, "")).isEmpty();
            assertThat(validate(field, 123)).extracting(FormValueViolation::code).containsExactly("formatic.format");
            field.setRequired(true);
            assertThat(validate(field, "")).extracting(FormValueViolation::code).containsExactly("formatic.required");
            field.setPattern("(?=a)");
            assertThatThrownBy(() -> validate(field, null)).hasMessageContaining("pattern");
        }
        var email = field(FormFieldType.EMAIL);
        email.setMaxLength(4);
        assertThat(validate(email, "a@b.c")).extracting(FormValueViolation::code).containsExactly("formatic.maxLength");
        var url = field(FormFieldType.URL);
        url.setMinLength(10);
        assertThat(validate(url, "urn:a")).extracting(FormValueViolation::code).containsExactly("formatic.minLength");
    }

    private List<FormValueViolation> validate(FormFieldMetadata field, Object value) {
        return validator.validate(List.of(field), path -> value);
    }

    private static FormFieldMetadata field(FormFieldType type) {
        var field = new FormFieldMetadata("value", type, "Value", 0);
        field.setDataType(FormDataType.STRING);
        return field;
    }
}
