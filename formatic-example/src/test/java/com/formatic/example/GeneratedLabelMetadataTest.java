package com.formatic.example;

import com.formatic.core.annotation.handler.DateInputHandler;
import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FormFieldLabels;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.example.dto.Editor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GeneratedLabelMetadataTest {
    @Test
    void allBuilderModesKeepMessageKeysUntilRequestLocalization() {
        List<FormFieldAnnotationHandler<?>> handlers = List.of(new DateInputHandler());
        var reflection = new ReflectionFormFieldMetadataBuilder(handlers);
        var bytecode = new BytecodeFormFieldMetadataBuilder(handlers);
        var generated = new CompileTimeFormFieldMetadataBuilder(
                mock(com.formatic.core.service.OptionsProviderService.class), reflection);
        for (var builder : List.of(reflection, bytecode, generated)) {
            var fields = builder.buildMetadata(Editor.class);
            assertThat(fields).filteredOn(field -> field.getName().equals("birthDate"))
                    .extracting(FormFieldMetadata::getLabel).containsExactly("#{birthDate}");
            var localized = FormFieldLabels.resolveBundle(fields, Locale.FRENCH, "messages");
            assertThat(localized).filteredOn(field -> field.getName().equals("birthDate"))
                    .extracting(FormFieldMetadata::getLabel).containsExactly("Date de naissance");
            assertThat(fields).filteredOn(field -> field.getName().equals("birthDate"))
                    .extracting(FormFieldMetadata::getLabel).containsExactly("#{birthDate}");
        }
    }
}
