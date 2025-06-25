package com.formatic.form;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class DateFieldHandler implements FormFieldTypeHandler {

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.DATE;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.DATE;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {

    }
}
