package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.DateInput;
import com.formatic.core.annotation.EmailInput;
import com.formatic.core.annotation.HiddenInput;
import com.formatic.core.annotation.TextareaInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatedFieldAttributesTest {

    @Test
    void dateDefaultsPreserveBrowserStep() throws Exception {
        FormFieldMetadata metadata = new DateInputHandler().handle(Form.class.getDeclaredField("date"));

        assertThat(metadata.getType()).isEqualTo(FormFieldType.DATE);
        assertThat(metadata.getDateStep()).isEmpty();
        assertThat(metadata.getMinDate()).isEmpty();
        assertThat(metadata.getMaxDate()).isEmpty();
    }

    @Test
    void dateStepIsCopiedIndependentlyOfNumericStep() throws Exception {
        FormFieldMetadata metadata = new DateInputHandler().handle(Form.class.getDeclaredField("weekly"));
        metadata.setStep(0.5);

        assertThat(metadata.getDateStep()).isEqualTo("7");
        FormFieldMetadata copy = new FormFieldMetadata(metadata);
        assertThat(copy.getDateStep()).isEqualTo("7");
        assertThat(copy.getStep()).isEqualTo(0.5);
        metadata.setDateStep("any");
        assertThat(copy.getDateStep()).isEqualTo("7");
    }

    @Test
    void textareaDefaultsAndCustomAttributesAreMapped() throws Exception {
        TextareaInputHandler handler = new TextareaInputHandler();
        FormFieldMetadata defaults = handler.handle(Form.class.getDeclaredField("comment"));
        assertThat(defaults.getWrap()).isEqualTo("soft");
        assertThat(defaults.getRows()).isEqualTo(4);
        assertThat(defaults.getCols()).isEqualTo(40);
        assertThat(defaults.getMinLength()).isEqualTo(-1);
        assertThat(defaults.getMaxLength()).isEqualTo(-1);

        FormFieldMetadata custom = handler.handle(Form.class.getDeclaredField("details"));
        assertThat(custom.getType()).isEqualTo(FormFieldType.TEXTAREA);
        assertThat(custom.getName()).isEqualTo("description");
        assertThat(custom.getLabel()).isEqualTo("Details");
        assertThat(custom.getWrap()).isEqualTo("hard");
        assertThat(custom.getRows()).isEqualTo(8);
        assertThat(custom.getCols()).isEqualTo(60);
        assertThat(custom.getMinLength()).isEqualTo(2);
        assertThat(custom.getMaxLength()).isEqualTo(120);
        assertThat(custom.getDefaultValue()).isEqualTo("Initial text");
        assertThat(custom.getPlaceholder()).isEqualTo("Enter details");
        assertThat(custom.isRequired()).isTrue();
        assertThat(custom.isReadonly()).isTrue();
        FormFieldMetadata copy = new FormFieldMetadata(custom);
        custom.setWrap("off");
        assertThat(copy.getWrap()).isEqualTo("hard");
    }

    @Test
    void emailMapsDefaultValueAndErrorMessage() throws Exception {
        EmailInputHandler handler = new EmailInputHandler();
        FormFieldMetadata defaults = handler.handle(Form.class.getDeclaredField("email"));
        assertThat(defaults.getDefaultValue()).isEmpty();
        assertThat(defaults.getErrorMessage()).isEmpty();

        FormFieldMetadata custom = handler.handle(Form.class.getDeclaredField("contact"));
        assertThat(custom.getDefaultValue()).isEqualTo("contact@example.com");
        assertThat(custom.getErrorMessage()).isEqualTo("Enter a valid email address");
    }

    @Test
    void hiddenMapsNameAndDefaultValueWithoutAddingLabel() throws Exception {
        FormFieldMetadata metadata = new HiddenInputHandler().handle(Form.class.getDeclaredField("id"));

        assertThat(metadata.getType()).isEqualTo(FormFieldType.HIDDEN);
        assertThat(metadata.getName()).isEqualTo("identifier");
        assertThat(metadata.getDefaultValue()).isEqualTo("123");
        assertThat(metadata.getLabel()).isNull();
    }

    private static class Form {
        @DateInput
        private String date;

        @DateInput(step = "7")
        private String weekly;

        @TextareaInput
        private String comment;

        @TextareaInput(name = "description", wrap = "hard", rows = 8, cols = 60,
                minLength = 2, maxLength = 120, defaultValue = "Initial text",
                placeholder = "Enter details", required = true, readonly = true)
        private String details;

        @EmailInput
        private String email;

        @EmailInput(defaultValue = "contact@example.com", errorMessage = "Enter a valid email address")
        private String contact;

        @HiddenInput(name = "identifier", defaultValue = "123")
        private String id;
    }
}
