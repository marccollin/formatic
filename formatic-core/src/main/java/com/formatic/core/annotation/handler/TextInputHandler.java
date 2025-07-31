package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.TextInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link TextInput}.
 * <p>
 * This class processes metadata for text  input fields in dynamic form generation.
 * It extracts and sets text-specific properties such as input pattern, placeholder,
 * min/max length, default value, read-only status, CSS class, and error message.
 * <p>
 * Maps the {@code @TextInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class TextInputHandler extends BaseFormFieldHandler<TextInput> {

    private static final Logger logger = LoggerFactory.getLogger(TextInputHandler.class);

    public TextInputHandler() {
        super(TextInput.class);
    }

    @Override
    protected void processSpecificAttributes(TextInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.TEXT);
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
