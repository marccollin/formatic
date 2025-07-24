package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.PhoneInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link PhoneInput}.
 * <p>
 * This class processes metadata for phone number input fields in dynamic form generation.
 * It extracts and sets phone-specific properties such as input pattern, placeholder,
 * min/max length, default value, read-only status, CSS class, and error message.
 * <p>
 * Maps the {@code @PhoneInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class PhoneInputHandler extends BaseFormFieldHandler<PhoneInput> {

    public PhoneInputHandler() {
        super(PhoneInput.class);
    }

    @Override
    protected void processSpecificAttributes(PhoneInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.PHONE);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setPattern(annotation.pattern());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setErrorMessage(annotation.errorMessage());
    }
}
