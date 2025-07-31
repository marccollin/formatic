package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.TextareaInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link TextareaInput}.
 * <p>
 * Maps {@code @TextareaInput}-annotated fields to {@link FormFieldMetadata},
 * setting attributes specific to multi-line text inputs such as:
 * - CSS class, placeholder, default value
 * - Required and readonly flags
 * - Minimum and maximum length
 * - Number of rows and columns
 */
public class TextareaInputHandler extends BaseFormFieldHandler<TextareaInput> {

    public TextareaInputHandler() {
        super(TextareaInput.class);
    }

    @Override
    protected void processSpecificAttributes(TextareaInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.TEXTAREA);
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setRequired(annotation.required());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setRows(annotation.rows());
        metadata.setCols(annotation.cols());

        // metadata.setErrorMessage(annotation.errorMessage());
    }

}
