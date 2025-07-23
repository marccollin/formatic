package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.EmailInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link EmailInput}.
 *
 * This class processes metadata for email  input fields in dynamic form generation.
 * It extracts and sets email-specific properties such as input pattern, placeholder,
 * min/max length, default value, read-only status, CSS class, and error message.
 *
 * Maps the {@code @UrlInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class EmailInputHandler extends BaseFormFieldHandler<EmailInput> {

    public EmailInputHandler() {
        super(EmailInput.class);
    }

    @Override
    protected void processSpecificAttributes(EmailInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.EMAIL);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setReadonly(annotation.readonly());
        metadata.setPattern(annotation.pattern());
    }

}
