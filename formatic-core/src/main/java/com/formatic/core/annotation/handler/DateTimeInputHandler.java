package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.DateTimeInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

public class DateTimeInputHandler extends BaseFormFieldHandler<DateTimeInput> {

    public DateTimeInputHandler() {
        super(DateTimeInput.class);
    }

    @Override
    protected void processSpecificAttributes(DateTimeInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.DATETIME_LOCAL);
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinDateTime(annotation.min());
        metadata.setMaxDateTime(annotation.max());
        metadata.setDateTimeStep(annotation.step());
    }
}
