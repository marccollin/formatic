package com.formatic.example;

import com.formatic.core.annotation.handler.*;
import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
import com.formatic.core.builder.FormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.service.OptionsProviderService;
import com.formatic.example.dto.Editor;
import com.formatic.example.dto.User;
import com.formatic.example.dto.UserBasic;
import com.formatic.example.dto.UserClub;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExampleConstraintCompatibilityTest {
    static Stream<Class<?>> exampleForms() {
        return Stream.of(Editor.class, User.class, UserBasic.class, UserClub.class);
    }

    @ParameterizedTest
    @MethodSource("exampleForms")
    void exampleDtosBuildWithAllMetadataModes(Class<?> formClass) {
        OptionsProviderService options = mock(OptionsProviderService.class);
        when(options.loadOptions(anyString())).thenReturn(List.of());
        List<FormFieldAnnotationHandler<?>> handlers = List.of(
                new TextInputHandler(), new TextareaInputHandler(), new DateInputHandler(),
                new NumberInputHandler(), new UrlInputHandler(), new PhoneInputHandler(),
                new RadioInputHandler(options), new SelectInputHandler(options),
                new EmailInputHandler(), new FileInputHandler(), new PasswordInputHandler(),
                new HiddenInputHandler(), new CheckBoxHandler(options));
        ReflectionFormFieldMetadataBuilder reflection = new ReflectionFormFieldMetadataBuilder(handlers);
        List<FormFieldMetadataBuilder> builders = List.of(reflection,
                new BytecodeFormFieldMetadataBuilder(handlers),
                new CompileTimeFormFieldMetadataBuilder(options, reflection));

        for (FormFieldMetadataBuilder builder : builders) {
            assertThat(builder.buildMetadata(formClass))
                    .as("%s using %s", formClass.getSimpleName(), builder.getClass().getSimpleName())
                    .isNotEmpty()
                    .allSatisfy(field -> assertThat(field.getType()).isNotNull());
        }
    }
}
