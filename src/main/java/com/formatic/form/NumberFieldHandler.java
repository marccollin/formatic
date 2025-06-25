package com.formatic.form;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

@Component
public class NumberFieldHandler implements FormFieldTypeHandler {

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.NUMBER ||
                (annotation.type() == FormFieldType.AUTO && isNumericType(field.getType()));
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.NUMBER;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {
        if (annotation.min() != Double.MIN_VALUE) {
            metadata.setMin(annotation.min());
        }
        if (annotation.max() != Double.MAX_VALUE) {
            metadata.setMax(annotation.max());
        }
    }

    private boolean isNumericType(Class<?> type) {
        return type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == double.class || type == Double.class ||
                type == float.class || type == Float.class ||
                type == BigDecimal.class || type == BigInteger.class;
    }
}
