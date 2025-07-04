package com.formatic.annotation.handler;

import com.formatic.annotation.CommonFieldAttributes;
import com.formatic.annotation.TextInput;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class TextInputHandler extends BaseFormFieldHandler<TextInput> {

    public TextInputHandler() {
        super(TextInput.class);
    }

    @Override
    protected void processSpecificAttributes(TextInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.TEXT);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setPattern(annotation.pattern());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setPattern(annotation.pattern());
        metadata.setErrorMessage(annotation.errorMessage());
        metadata.setTitle(annotation.title());
    }

}
