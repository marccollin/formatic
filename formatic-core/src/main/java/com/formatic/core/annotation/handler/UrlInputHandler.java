package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.UrlInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link UrlInput}.
 *
 * This class processes metadata for url input fields in dynamic form generation.
 * It extracts and sets url-specific properties such as input pattern, placeholder,
 * min/max length, default value, read-only status, CSS class, and error message.
 *
 * Maps the {@code @UrlInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class UrlInputHandler extends BaseFormFieldHandler<UrlInput> {

    public UrlInputHandler() {
        super(UrlInput.class);
    }

    @Override
    protected void processSpecificAttributes(UrlInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.URL);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setPattern(annotation.pattern());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setErrorMessage(annotation.errorMessage());
        metadata.setTitle(annotation.title());
    }

}
