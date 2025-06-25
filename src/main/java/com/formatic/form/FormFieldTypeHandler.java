package com.formatic.form;

import java.lang.reflect.Field;

public interface FormFieldTypeHandler {

    boolean canHandle(Field field, FormField annotation);
    FormFieldType getFieldType();
    void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation);

}
