package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FormFieldLabelsTest {
    @Test
    void resolvesPerLocaleWithoutChangingCachedMetadata() {
        var translated = new FormFieldMetadata("address.birthDate", FormFieldType.DATE, "#{birthDate}", 0);
        var literal = new FormFieldMetadata("firstName", FormFieldType.TEXT, "First name", 1);
        var fields = List.of(translated, literal);
        var english = FormFieldLabels.resolveBundle(fields, Locale.ENGLISH, "labels");
        var french = FormFieldLabels.resolveBundle(fields, Locale.FRENCH, "labels");
        assertThat(english.getFirst().getLabel()).isEqualTo("Birth date");
        assertThat(french.getFirst().getLabel()).isEqualTo("Date de naissance");
        assertThat(french.get(1).getLabel()).isEqualTo("First name");
        assertThat(translated.getLabel()).isEqualTo("#{birthDate}");
        assertThat(english.getFirst()).isNotSameAs(translated);
    }

    @Test
    void missingKeyFallsBackToFinalBindingNameAndLiteralsStayUnchanged() {
        var fields = List.of(
                new FormFieldMetadata("address.birthDate", FormFieldType.DATE, "#{missing}", 0),
                new FormFieldMetadata("name", FormFieldType.TEXT, "literal", 1),
                new FormFieldMetadata("other", FormFieldType.TEXT, "#{}", 2));
        var result = FormFieldLabels.resolve(fields, Locale.FRENCH, (key, locale) -> Optional.empty());
        assertThat(result).extracting(FormFieldMetadata::getLabel)
                .containsExactly("BirthDate", "literal", "#{}");
        assertThat(fields.getFirst().getLabel()).isEqualTo("#{missing}");
    }
}
