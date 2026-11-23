package com.formatic.core.form;

import com.formatic.core.annotation.CheckboxInput;
import com.formatic.core.annotation.PasswordInput;
import com.formatic.core.annotation.SelectInput;
import com.formatic.core.annotation.handler.CheckBoxHandler;
import com.formatic.core.annotation.handler.PasswordInputHandler;
import com.formatic.core.annotation.handler.SelectInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FormAnnotationDefaultsTest {
    @Test
    void preparesSelectAndCheckboxDefaultsWithoutGuessingTheirTypes() {
        var options = mock(com.formatic.core.service.OptionsProviderService.class);
        var fields = new ReflectionFormFieldMetadataBuilder(List.of(
                new SelectInputHandler(options), new CheckBoxHandler(options), new PasswordInputHandler()))
                .buildMetadata(Defaults.class);

        assertThat(fields).extracting(FormFieldMetadata::getDefaultValue)
                .containsExactly("0", "true", "true", null, null, null);
        assertThat(FormFieldValues.defaultValue(fields.get(0)))
                .contains(new FormDataValue(FormDataType.INTEGER, BigInteger.ZERO));
        assertThat(FormFieldValues.defaultValue(fields.get(1)))
                .contains(new FormDataValue(FormDataType.BOOLEAN, true));
        assertThat(FormFieldValues.defaultValue(fields.get(2)))
                .contains(new FormDataValue(FormDataType.STRING, "true"));
        for (var field : fields.subList(3, fields.size())) {
            assertThat(FormFieldValues.defaultValue(field)).isEmpty();
        }
    }

    static class Defaults {
        @SelectInput(options = {"0:Zero", "1:One"}, defaultValue = "0")
        private int count;
        @CheckboxInput(defaultChecked = true)
        private boolean subscribed;
        @CheckboxInput(defaultChecked = true)
        private String enabled;
        @CheckboxInput
        private Boolean optional;
        @CheckboxInput(options = {"java:Java"})
        private List<String> interests;
        @PasswordInput(defaultValue = "not-exported")
        private String password;
    }
}
