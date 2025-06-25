package com.formatic.form;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class TextAreaFieldHandler implements FormFieldTypeHandler {

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.TEXTAREA;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.TEXTAREA;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {
        if (annotation.rows() > 0) {
            metadata.setRows(annotation.rows());
        }
        if (annotation.cols() > 0) {
            metadata.setCols(annotation.cols());
        }
    }
}
