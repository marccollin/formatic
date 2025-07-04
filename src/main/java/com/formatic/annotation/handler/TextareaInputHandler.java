package com.formatic.annotation.handler;

import com.formatic.annotation.TextInput;
import com.formatic.annotation.TextareaInput;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class TextareaInputHandler extends BaseFormFieldHandler<TextareaInput> {

    public TextareaInputHandler() {
        super(TextareaInput.class);
    }

    @Override
    protected void processSpecificAttributes(TextareaInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.TEXTAREA);
        metadata.setCssClass(annotation.cssClass());
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
