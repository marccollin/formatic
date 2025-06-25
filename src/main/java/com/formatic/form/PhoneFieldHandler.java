package com.formatic.form;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class PhoneFieldHandler implements FormFieldTypeHandler {

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.PHONE;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.PHONE;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {

    }
}
