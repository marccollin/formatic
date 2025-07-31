package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.PasswordInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link PasswordInput}.
 * <p>
 * This class processes metadata for password input fields in dynamic form generation.
 * It extracts and sets password-specific properties such as input pattern, placeholder,
 * min/max length, default value, read-only status, CSS class, and error message.
 * <p>
 * Maps the {@code @PasswordInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class PasswordInputHandler extends BaseFormFieldHandler<PasswordInput> {

    public PasswordInputHandler() {
        super(PasswordInput.class);
    }

    @Override
    protected void processSpecificAttributes(PasswordInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.PASSWORD);
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setPattern(annotation.pattern());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
    }

}
