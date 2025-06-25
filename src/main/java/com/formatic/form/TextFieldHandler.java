package com.formatic.form;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

@Component
public class TextFieldHandler implements FormFieldTypeHandler {

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.TEXT ||
                (annotation.type() == FormFieldType.AUTO && field.getType() == String.class);
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.TEXT;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {
        // Configuration spécifique au texte
        if (StringUtils.hasText(annotation.pattern())) {
            metadata.setPattern(annotation.pattern());
        }
    }
}
