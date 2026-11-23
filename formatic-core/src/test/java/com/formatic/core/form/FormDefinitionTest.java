package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormDefinitionTest {
    @Test
    void preservesTitleAndPreparedFieldOrder() {
        var first = new FormFieldMetadata("first", FormFieldType.TEXT, "First", 2);
        var second = new FormFieldMetadata("second", FormFieldType.NUMBER, "Second", 1);

        var form = new FormDefinition("Contact", List.of(first, second));

        assertThat(form.title()).isEqualTo("Contact");
        assertThat(form.fields()).containsExactly(first, second);
        assertThat(form.fields().getFirst()).isSameAs(first);
    }

    @Test
    void copiesTheListWithoutCopyingMetadata() {
        var field = new FormFieldMetadata("name", FormFieldType.TEXT, "Name", 1);
        var fields = new ArrayList<>(List.of(field));
        var form = new FormDefinition(null, fields);

        fields.clear();

        assertThat(form.fields()).containsExactly(field);
        assertThatThrownBy(() -> form.fields().add(field))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(field.getLabel()).isEqualTo("Name");
    }

    @Test
    void acceptsAnAbsentTitleAndAnEmptyForm() {
        var form = new FormDefinition(null, List.of());

        assertThat(form.title()).isNull();
        assertThat(form.fields()).isEmpty();
    }

    @Test
    void rejectsNullListsAndNullFields() {
        assertThatNullPointerException().isThrownBy(() -> new FormDefinition(null, null));
        assertThatNullPointerException().isThrownBy(
                () -> new FormDefinition(null, Arrays.asList((FormFieldMetadata) null)));
    }
}
