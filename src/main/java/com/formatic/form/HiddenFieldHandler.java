package com.formatic.form;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Component
public class HiddenFieldHandler implements FormFieldTypeHandler {
    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.HIDDEN;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.HIDDEN;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {
        metadata.setLabel(null);
    }
}
