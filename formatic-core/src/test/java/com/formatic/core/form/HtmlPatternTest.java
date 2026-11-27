package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlPatternTest {
    @Test
    void matchesSharedJavaScriptUnicodeSetVectors() throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/html-pattern-vectors.tsv")),
                StandardCharsets.UTF_8))) {
            for (String line : reader.lines().toList()) {
                String[] parts = line.split("\t", -1);
                String value = parts[1].startsWith("U+")
                        ? new String(Character.toChars(Integer.parseInt(parts[1].substring(2), 16))) : parts[1];
                assertThat(HtmlPattern.compile(parts[0]).matches(value)).as(line)
                        .isEqualTo(Boolean.parseBoolean(parts[2]));
            }
        }
    }

    @Test
    void rejectsUnsupportedAndInvalidSyntaxRatherThanApproximating() {
        for (String pattern : List.of("^abc$", "(?=a)a", "(?<=a)b", "(?<x>a)", "(?i:a)",
                "(a)\\1", "\\p{L}", "\\s", "[a&&b]", "[a--b]", "[a-z0-9-]", "[\\d]",
                "[z-a]", "[]", "[a", "a{2,1}", "a{10001}", "a++", "a{", "\\",
                "é", "a\n", "a)")) {
            assertThatThrownBy(() -> HtmlPattern.compile(pattern)).as(pattern)
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pattern");
        }
        assertThatThrownBy(() -> HtmlPattern.compile("(".repeat(33) + "a" + ")".repeat(33)))
                .hasMessageContaining("nesting");
    }

    @Test
    void validatesSupportedControlsWithoutTreatingEmptyValuesAsMismatches() {
        var validator = new FormValueValidator();
        for (var type : List.of(FormFieldType.TEXT, FormFieldType.PASSWORD, FormFieldType.EMAIL,
                FormFieldType.URL, FormFieldType.PHONE)) {
            var field = new FormFieldMetadata("value", type, "Value", 0);
            field.setPattern("[0-9]{3}");
            assertThat(validator.validate(List.of(field), path -> null)).isEmpty();
            assertThat(validator.validate(List.of(field), path -> "")).isEmpty();
            assertThat(validator.validate(List.of(field), path -> "1234"))
                    .extracting(FormValueViolation::code).contains("formatic.pattern");
        }
        var field = new FormFieldMetadata("value", FormFieldType.TEXTAREA, "Value", 0);
        field.setPattern(".*");
        assertThatThrownBy(() -> validator.validate(List.of(field), path -> null))
                .hasMessageContaining("pattern requires");
        field.setType(FormFieldType.TEXT);
        field.setPattern("(?=a)");
        assertThatThrownBy(() -> validator.validate(List.of(field), path -> null))
                .hasMessageContaining("value").hasMessageContaining("(?=a)");
    }
}
